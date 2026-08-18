// PocketBase migration: relax `required` on numeric fields that legitimately
// hold 0.
//
// The app sorts wallets 0-based, so the first wallet in a budget has
// `sortOrder = 0`. PocketBase treats 0 on a `required` number field as "blank"
// and rejects the create with `sortOrder: cannot be blank`, breaking cloud sync.
// `sortOrder` is an ordering hint, not business data, so it must not be required.

migrate((app) => {
  try {
    const wallets = app.findCollectionByNameOrId("wallets");
    const sortOrder = wallets.fields.getByName("sortOrder");
    sortOrder.required = false;
    app.save(wallets);
    console.log("wallets.sortOrder: required relaxed (0 allowed)");
  } catch (err) {
    app.logger().warn("wallets.sortOrder migration skipped: " + String(err));
  }
}, (app) => {
  // rollback is a no-op; restoring `required` is not required
});