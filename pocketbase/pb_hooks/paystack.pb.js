// PocketBase JS hook: Paystack webhook → subscriptions
//
// NOTE: PocketBase only auto-loads hook files with a `*.pb.js` suffix, so this
// file MUST stay named paystack.pb.js (a plain .js name would be treated as a
// require()-able module and silently ignored).
//
// Deploy alongside the PocketBase binary at pocketbase/pb_hooks/paystack.pb.js.
// It registers POST /api/paystack/webhook, verifies the Paystack HMAC-SHA512
// signature over the RAW request body, then upserts an "active" row in the
// `subscriptions` collection for the matching user (matched by email).
//
// Requires the env var `POCKETBASE_PAYSTACK_SECRET_KEY` (your live or test
// secret key — never the public key, and never ship it in the Android app).
//
// Configure the Paystack dashboard webhook URL to your public endpoint:
//   https://pb.yourdomain.com/api/paystack/webhook

// Load-time sanity log to confirm this hook file was picked up by PocketBase.
console.log("paystack.pb.js hook: registering /api/paystack/webhook");

routerAdd("POST", "/api/paystack/webhook", (e) => {
  const secret = $os.getenv("POCKETBASE_PAYSTACK_SECRET_KEY");
  if (!secret) {
    $app.logger().error("paystack webhook: POCKETBASE_PAYSTACK_SECRET_KEY is not set");
    return e.json(500, { error: "secret not configured" });
  }

  // --- Verify signature over the raw body (HMAC-SHA512, hex-encoded) ------
  const rawBody = toString(e.request.body);
  const signature = (e.request.header.get("x-paystack-signature") || "").trim();
  const expected = $security.hs512(rawBody, secret);

  if (!signature || !$security.equal(signature, expected)) {
    $app.logger().warn("paystack webhook: signature mismatch");
    throw new UnauthorizedError("Invalid signature");
  }

  // --- Parse -----------------------------------------------------------------
  let payload = null;
  try {
    payload = JSON.parse(rawBody);
  } catch (_) {
    throw new BadRequestError("Invalid JSON body");
  }

  const event = payload.event || "";
  const data = payload.data || {};

  // --- Resolve user by email --------------------------------------------------
  const d1 = data.customer || data.invoice || {};
  const email = d1.email || data.email || "";
  if (!email) {
    return e.json(200, { received: true });
  }

  let user = null;
  try {
    user = e.app.findFirstRecordByData("users", "email", email);
  } catch (_) {
    user = null;
  }
  if (!user) {
    // Unknown user (e.g. test charge with a throwaway email). Acknowledge so
    // Paystack stops retrying; nothing to grant.
    return e.json(200, { received: true });
  }

  // --- Resolve plan -----------------------------------------------------------
  const sub = (data.subscription || {}).plan || data.plan || {};
  const interval = sub.interval || null;
  let planId = null;
  if (interval === "monthly") planId = "pro_monthly";
  else if (interval === "annually" || interval === "yearly") planId = "pro_annual";
  if (!planId) {
    const amount = sub.amount || data.amount || null;
    if (amount === 150000) planId = "pro_monthly";
    else if (amount === 1440000) planId = "pro_annual";
  }

  const subscriptionRecord = payload.subscription || data.subscription || {};
  const subscriptionCode = data.subscription_code || subscriptionRecord.subscription_code || null;
  const reference = data.reference || null;

  // --- Build date window --------------------------------------------------------
  let startDate = new Date();
  try {
    if (data.created_at) startDate = new Date(data.created_at);
    else if (subscriptionRecord.created_at) startDate = new Date(subscriptionRecord.created_at);
  } catch (_) {
    startDate = new Date();
  }

  const graceMs = 3 * 24 * 60 * 60 * 1000; // 3 days of grace
  let endDate = null;

  const nextPayment = data.next_payment_date || subscriptionRecord.next_payment_date || null;
  if (nextPayment) {
    try {
      endDate = new Date();
      endDate.setTime(new Date(nextPayment).getTime() + graceMs);
    } catch (_) {
      endDate = null;
    }
  } else if (planId === "pro_annual") {
    endDate = new Date();
    endDate.setTime(startDate.getTime());
    endDate.setFullYear(endDate.getFullYear() + 1);
  } else if (planId === "pro_monthly") {
    endDate = new Date();
    endDate.setTime(startDate.getTime());
    endDate.setMonth(endDate.getMonth() + 1);
  }

  // --- Decide action -------------------------------------------------------------
  const grant = (event === "charge.success" ||
    event === "invoice.paid" ||
    event === "invoice.create" ||
    event === "subscription.create" ||
    event === "subscription.update") && planId !== null && endDate !== null;

  const revoke = (event === "subscription.disable" ||
    event === "subscription.cancel" ||
    event === "charge.refund" ||
    event === "charge.failed");

  if (!grant && !revoke) {
    return e.json(200, { received: true });
  }

  // --- Upsert subscription record ------------------------------------------------
  const collection = e.app.findCollectionByNameOrId("subscriptions");

  let existing = null;
  if (subscriptionCode) {
    try {
      existing = e.app.findFirstRecordByFilter(
        "subscriptions",
        "paystackSubscriptionCode = {:code}",
        { code: subscriptionCode }
      );
    } catch (_) {
      existing = null;
    }
  }
  if (!existing) {
    try {
      existing = e.app.findFirstRecordByFilter(
        "subscriptions",
        "userId = {:id} && provider = 'paystack'",
        { id: user.id }
      );
    } catch (_) {
      existing = null;
    }
  }

  if (revoke && !existing) {
    return e.json(200, { received: true });
  }

  const record = existing || new Record(collection);

  if (grant) {
    record.load({
      userId: user.id,
      provider: "paystack",
      plan: planId,
      status: "active",
      startDate: startDate.toISOString(),
      endDate: endDate.toISOString(),
    });
    if (reference) { record.set("paystackReference", reference); }
    if (subscriptionCode) { record.set("paystackSubscriptionCode", subscriptionCode); }
  } else {
    record.set("status", (event === "subscription.disable" || event === "subscription.cancel")
      ? "cancelled" : "expired");
  }

  try {
    e.app.save(record);
  } catch (err) {
    $app.logger().error("paystack webhook: save failed: " + String(err));
    throw new InternalServerError("Failed to save subscription");
  }

  $app.logger().info("paystack webhook: " + event + " -> user " + user.id + " plan " + planId);
  return e.json(200, { received: true });
});