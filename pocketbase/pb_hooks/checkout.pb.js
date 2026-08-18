// PocketBase JS hook: create a Paystack transaction/initialize checkout URL.
//
// NOTE: PocketBase only auto-loads hook files with a `*.pb.js` suffix. Keep
// this file named checkout.pb.js.
//
// Deploy alongside the PocketBase binary at pocketbase/pb_hooks/checkout.pb.js.
// Requires the env var POCKETBASE_PAYSTACK_SECRET_KEY (server-side only).
// Prices are read from the `config` collection record key = "plans"
// (see pocketbase/seed/default_plans.json).
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
  // $apis.requireAuth() guarantees e.auth is a valid users record here.
  const email = e.auth.get("email");
  if (!email) {
    throw new BadRequestError("Account has no email");
  }

  // --- Read plan price from server config (single source of truth) ----------
  const rawBody = toString(e.request.body);
  let body = null;
  try {
    body = JSON.parse(rawBody || "{}");
  } catch (_) {
    throw new BadRequestError("Invalid JSON body");
  }
  const planId = body.plan || "";

  let plans = {};
  let plansRecord = null;
  try {
    plansRecord = e.app.findFirstRecordByFilter(
      "config",
      "key = {:key}",
      { key: "plans" }
    );
  } catch (_) {
    // No "plans" config record seeded yet; unknown plan is reported below.
    plansRecord = null;
  }
  if (plansRecord) {
    // PocketBase exposes json-type field values from `record.get()` as a raw
    // []byte array (numbers), so decode them back into a JS object.
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

  // --- Build checkout URL (the app's WebView watches for this prefix) ---------
  const publicBase = ($os.getenv("POCKETBASE_URL") || "").trim().replace(/\/+$/, "");
  const fallbackBase = "https://mebudget-api.blackshade.site";
  const successUrl = (publicBase || fallbackBase) + "/checkout-success";
  const cancelUrl = (publicBase || fallbackBase) + "/checkout-cancel";

  // --- Initialize a Paystack transaction --------------------------------------
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