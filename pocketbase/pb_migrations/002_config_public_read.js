// PocketBase migration: make the `config` collection publicly readable.
//
// In PocketBase, a rule value of null is LOCKED (superuser-only access) and
// an empty string "" ALLOWS EVERYONE. These records only hold non-sensitive
// feature limits, so set list/view to "" (public read) and leave writes locked.

migrate((app) => {
  try {
    const config = app.findCollectionByNameOrId("config");
    config.listRule = ""; // allow all (public read of limits)
    config.viewRule = ""; // allow all
    config.createRule = null; // locked (only superuser / seeding script)
    config.updateRule = null; // locked
    config.deleteRule = null; // locked
    app.save(config);
    console.log("config collection: public read enabled");
  } catch (err) {
    app.logger().warn("config migration skipped: " + String(err));
  }

  // Subscriptions are ONLY written by the Paystack webhook (which runs with
  // superuser privileges and bypasses these rules). Free users must not be able
  // to create/PATCH subscription rows to grant themselves Pro. LOCKED (null)
  // means only superusers can write; reads stay owner-only.
  try {
    const subscriptions = app.findCollectionByNameOrId("subscriptions");
    subscriptions.createRule = null; // locked (webhook runs as superuser)
    subscriptions.updateRule = null; // locked
    subscriptions.deleteRule = null; // locked
    subscriptions.listRule = "@request.auth.id != '' && userId = @request.auth.id";
    subscriptions.viewRule = "@request.auth.id != '' && userId = @request.auth.id";
    app.save(subscriptions);
    console.log("subscriptions collection: self-service writes denied");
  } catch (err) {
    app.logger().warn("subscriptions migration skipped: " + String(err));
  }
}, (app) => {
  // rollback is a no-op; restoring the previous rules is not required
});