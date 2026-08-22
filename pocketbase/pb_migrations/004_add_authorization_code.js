migrate((app) => {
  const subscriptions = app.findCollectionByNameOrId("subscriptions");
  subscriptions.fields.addAt(subscriptions.fields.length, new TextField({
    name: "authorizationCode",
    required: false,
    max: 200,
  }));
  app.save(subscriptions);
}, (app) => {
  const subscriptions = app.findCollectionByNameOrId("subscriptions");
  const idx = subscriptions.fields.findIndex(f => f.name === "authorizationCode");
  if (idx >= 0) {
    subscriptions.fields.remove(idx);
    app.save(subscriptions);
  }
});
