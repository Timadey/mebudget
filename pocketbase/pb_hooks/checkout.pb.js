// PocketBase JS hook: create a Paystack subscription checkout.
//
// Flow:
//   1. Create a Paystack Plan (or find existing one) via POST /plan
//   2. Initialize transaction with the plan code → Paystack auto-subscribes after payment
//   3. Webhooks handle subscription lifecycle (paystack.pb.js)
//
// NOTE: PocketBase only auto-loads hook files with a `*.pb.js` suffix. Keep
// this file named checkout.pb.js.
//
// IMPORTANT: PocketBase's JSVM runs each route handler in an isolated scope, so
// module-level `const`/`var`/functions are NOT visible inside the callback.
// Everything the handler needs must be defined inline.

console.log("checkout.pb.js hook: registering /api/subscriptions/checkout");

routerAdd("POST", "/api/subscriptions/checkout", (e) => {
  const secret = $os.getenv("POCKETBASE_PAYSTACK_SECRET_KEY");
  if (!secret) {
    $app.logger().error("checkout: POCKETBASE_PAYSTACK_SECRET_KEY is not set");
    throw new InternalServerError("Payments are not configured");
  }

  // --- Resolve signed-in user's email --------------------------------------
  const email = e.auth.get("email");
  if (!email) {
    throw new BadRequestError("Account has no email");
  }

  // --- Read plan from request body ----------------------------------------
  const rawBody = toString(e.request.body);
  let body = null;
  try {
    body = JSON.parse(rawBody || "{}");
  } catch (_) {
    throw new BadRequestError("Invalid JSON body");
  }
  const planId = body.plan || "";

  // --- Read plan price from server config ----------------------------------
  let plans = {};
  let plansRecord = null;
  try {
    plansRecord = e.app.findFirstRecordByFilter(
      "config",
      "key = {:key}",
      { key: "plans" }
    );
  } catch (_) {
    plansRecord = null;
  }
  if (plansRecord) {
    const raw = plansRecord.get("value");
    if (raw !== null && raw !== undefined) {
      if (Array.isArray(raw)) {
        let text = "";
        for (const byte of raw) text += String.fromCharCode(byte);
        plans = JSON.parse(text || "null") || {};
      } else if (typeof raw === "string") {
        plans = JSON.parse(raw) || {};
      } else {
        plans = raw || {};
      }
    }
  }
  const plan = plans[planId] || null;
  if (!plan || !plan.priceKobo) {
    throw new BadRequestError("Unknown plan: " + planId);
  }

  // --- Step 1: Create or find Paystack Plan --------------------------------
  // Paystack plan codes are reusable. We create one per plan ID and cache it
  // in the config collection under "paystack_plans".
  let paystackPlanCode = null;
  let paystackPlansConfig = null;
  try {
    paystackPlansConfig = e.app.findFirstRecordByFilter(
      "config",
      "key = {:key}",
      { key: "paystack_plans" }
    );
  } catch (_) {
    paystackPlansConfig = null;
  }

  let paystackPlans = {};
  if (paystackPlansConfig) {
    const raw = paystackPlansConfig.get("value");
    if (raw !== null && raw !== undefined) {
      if (Array.isArray(raw)) {
        let text = "";
        for (const byte of raw) text += String.fromCharCode(byte);
        paystackPlans = JSON.parse(text || "null") || {};
      } else if (typeof raw === "string") {
        paystackPlans = JSON.parse(raw) || {};
      } else {
        paystackPlans = raw || {};
      }
    }
  }

  paystackPlanCode = paystackPlans[planId] || null;

  if (!paystackPlanCode) {
    // Create a new Paystack Plan
    const interval = planId === "pro_annual" ? "annually" : "monthly";
    const planName = planId === "pro_annual" ? "MeBudget Pro (Annual)" : "MeBudget Pro (Monthly)";

    let planRes = null;
    try {
      planRes = $http.send({
        url: "https://api.paystack.co/plan",
        method: "POST",
        headers: {
          "Authorization": "Bearer " + secret,
          "content-type": "application/json",
        },
        body: JSON.stringify({
          name: planName,
          interval: interval,
          amount: plan.priceKobo,
          currency: "NGN",
        }),
        timeout: 30,
      });
    } catch (err) {
      $app.logger().error("checkout: paystack plan creation failed: " + String(err));
      throw new ApiError(502, "Failed to create payment plan");
    }

    const planParsed = (planRes && typeof planRes.json === "object") ? planRes.json : null;
    if (!planRes || planRes.statusCode >= 400 || !planParsed || !planParsed.status) {
      $app.logger().error("checkout: paystack plan returned " + (planRes && planRes.statusCode) + " " + (planRes && planRes.body));
      throw new ApiError(502, "Failed to create payment plan");
    }

    paystackPlanCode = planParsed.data.plan_code;

    // Cache the plan code for future checkouts
    paystackPlans[planId] = paystackPlanCode;
    if (paystackPlansConfig) {
      paystackPlansConfig.set("value", JSON.stringify(paystackPlans));
      e.app.save(paystackPlansConfig);
    } else {
      const col = e.app.findCollectionByNameOrId("config");
      const rec = new Record(col);
      rec.load({ key: "paystack_plans", value: JSON.stringify(paystackPlans) });
      e.app.save(rec);
    }

    $app.logger().info("checkout: created paystack plan " + planId + " -> " + paystackPlanCode);
  }

  // --- Step 2: Initialize transaction with plan code -----------------------
  const publicBase = ($os.getenv("POCKETBASE_URL") || "").trim().replace(/\/+$/, "");
  const fallbackBase = "https://mebudget-api.blackshade.site";
  const successUrl = (publicBase || fallbackBase) + "/checkout-success";
  const cancelUrl = (publicBase || fallbackBase) + "/checkout-cancel";

  let res = null;
  try {
    res = $http.send({
      url: "https://api.paystack.co/transaction/initialize",
      method: "POST",
      headers: {
        "Authorization": "Bearer " + secret,
        "content-type": "application/json",
      },
      body: JSON.stringify({
        email: email,
        amount: plan.priceKobo,
        plan: paystackPlanCode,
        callback_url: successUrl,
        metadata: { plan: planId },
      }),
      timeout: 30,
    });
  } catch (err) {
    $app.logger().error("checkout: paystack request failed: " + String(err));
    throw new ApiError(502, "Failed to reach payment provider");
  }

  const parsed = (res && typeof res.json === "object") ? res.json : null;
  if (!res || res.statusCode >= 400 || !parsed || !parsed.status) {
    $app.logger().error("checkout: paystack returned status " + (res && res.statusCode) + " body " + (res && res.body));
    throw new ApiError(502, "Failed to create checkout");
  }
  const data = parsed.data || {};

  return e.json(200, {
    authorization_url: data.authorization_url || "",
    access_code: data.access_code || null,
    reference: data.reference || null,
  });
}, $apis.requireAuth());
