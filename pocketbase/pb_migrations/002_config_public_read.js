// PocketBase migration: make the `config` collection publicly readable.
//
// The initial migration declared listRule/viewRule as "" (empty), which PocketBase
// treats as DENY-ALL, so the Android app could never fetch server limits. These
// records only hold non-sensitive feature limits, so public read is safe.

migrate((app) => {
  try {
    const config = app.findCollectionByNameOrId("config");
    config.listRule = null; // allow all (public read of limits)
    config.viewRule = null; // allow all
    config.createRule = ""; // deny all (only superuser / seeding script writes)
    config.updateRule = ""; // deny all
    config.deleteRule = ""; // deny all
    app.save(config);
    console.log("config collection: public read enabled");
  } catch (err) {
    $app.logger().warn("config migration skipped: " + String(err));
  }

  // Subscriptions are ONLY written by the Paystack webhook (which runs with
  // superuser privileges and bypasses these rules). Free users must not be able
  // to PATCH/own subscription rows to grant themselves Pro, so deny writes.
  try {
    const subscriptions = app.findCollectionByNameOrId("subscriptions");
    subscriptions.createRule = ""; // deny all
    subscriptions.updateRule = ""; // deny all
    subscriptions.deleteRule = ""; // deny all
    subscriptions.listRule = "@request.auth.id != '' && userId = @request.auth.id";
    subscriptions.viewRule = "@request.auth.id != '' && userId = @request.auth.id";
    app.save(subscriptions);
    console.log("subscriptions collection: self-service writes denied");
  } catch (err) {
    $app.logger().warn("subscriptions migration skipped: " + String(err));
  }
}, (app) => {
  // rollback is a no-op; restoring the previous rules is not required
});