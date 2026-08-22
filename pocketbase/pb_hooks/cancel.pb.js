// PocketBase JS hook: cancel Paystack subscription
//
// POST /api/subscriptions/cancel
// Requires authentication. Finds the user's active Paystack subscription,
// calls POST /subscription/disable on Paystack, and updates local state.
//
// The subscription continues until the end of the paid period (graceful cancel).
// Paystack will fire a subscription.disable webhook when the period ends.

console.log("cancel.pb.js hook: registering /api/subscriptions/cancel");

routerAdd("POST", "/api/subscriptions/cancel", (e) => {
  const secret = $os.getenv("POCKETBASE_PAYSTACK_SECRET_KEY");
  if (!secret) {
    $app.logger().error("cancel: POCKETBASE_PAYSTACK_SECRET_KEY is not set");
    throw new InternalServerError("Payments are not configured");
  }

  const userId = e.auth.id;
  if (!userId) {
    throw new UnauthorizedError("Not authenticated");
  }

  // --- Find active Paystack subscription ----------------------------------
  let sub = null;
  try {
    sub = e.app.findFirstRecordByFilter(
      "subscriptions",
      "userId = {:id} && provider = 'paystack' && status = 'active'",
      { id: userId }
    );
  } catch (_) {
    sub = null;
  }

  if (!sub) {
    throw new BadRequestError("No active Paystack subscription found");
  }

  const subscriptionCode = sub.get("paystackSubscriptionCode") || "";
  const emailToken = sub.get("emailToken") || "";

  if (!subscriptionCode) {
    throw new BadRequestError("Subscription has no Paystack code");
  }

  // --- Call Paystack to disable subscription --------------------------------
  let res = null;
  try {
    res = $http.send({
      url: "https://api.paystack.co/subscription/disable",
      method: "POST",
      headers: {
        "Authorization": "Bearer " + secret,
        "content-type": "application/json",
      },
      body: JSON.stringify({
        code: subscriptionCode,
        token: emailToken,
      }),
      timeout: 30,
    });
  } catch (err) {
    $app.logger().error("cancel: paystack request failed: " + String(err));
    throw new ApiError(502, "Failed to reach payment provider");
  }

  const parsed = (res && typeof res.json === "object") ? res.json : null;
  if (!res || res.statusCode >= 400 || !parsed || !parsed.status) {
    $app.logger().error("cancel: paystack returned " + (res && res.statusCode) + " " + (res && res.body));
    throw new ApiError(502, "Failed to cancel subscription");
  }

  // --- Update local subscription status ------------------------------------
  // Mark as non-renewing (user keeps Pro until endDate)
  sub.set("status", "cancelled");
  e.app.save(sub);

  $app.logger().info("cancel: subscription " + subscriptionCode + " cancelled for user " + userId);
  return e.json(200, {
    status: "cancelled",
    message: "Subscription cancelled. You retain Pro access until the end of your billing period.",
    endDate: sub.get("endDate") || null,
  });
}, $apis.requireAuth());
