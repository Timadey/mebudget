// PocketBase migration: create collections for MeBudget cloud sync & billing
// Generated for PocketBase v0.23+ (current v0.39.x). Applies automatically on `serve`.

migrate((app) => {
  // ------------------------------------------------------------------
  // budgets
  // ------------------------------------------------------------------
  let budgets = new Collection({
    type: "base",
    name: "budgets",
    listRule: "@request.auth.id != '' && userId = @request.auth.id",
    viewRule: "@request.auth.id != '' && userId = @request.auth.id",
    createRule: "@request.auth.id != ''",
    updateRule: "@request.auth.id != '' && userId = @request.auth.id",
    deleteRule: "@request.auth.id != '' && userId = @request.auth.id",
    fields: [
      { name: "userId", type: "relation", required: true, maxSelect: 1, collectionId: "_pb_users_auth_" },
      { name: "name", type: "text", required: true, max: 200 },
      { name: "startDateEpochDay", type: "number", required: false },
      { name: "endDateEpochDay", type: "number", required: false },
      { name: "negativeBalanceRule", type: "select", required: true, maxSelect: 1, values: ["ALLOW", "WARN", "BLOCK"] },
      { name: "createdAtMillis", type: "number", required: true },
      { name: "updatedAtMillis", type: "number", required: true },
      { name: "deleted", type: "bool", required: false },
    ],
    indexes: [
      "CREATE INDEX idx_budgets_user ON budgets (userId)",
      "CREATE INDEX idx_budgets_updated ON budgets (updatedAtMillis)",
    ],
  });
  app.save(budgets);

  // ------------------------------------------------------------------
  // wallets
  // ------------------------------------------------------------------
  let wallets = new Collection({
    type: "base",
    name: "wallets",
    listRule: "@request.auth.id != '' && userId = @request.auth.id",
    viewRule: "@request.auth.id != '' && userId = @request.auth.id",
    createRule: "@request.auth.id != ''",
    updateRule: "@request.auth.id != '' && userId = @request.auth.id",
    deleteRule: "@request.auth.id != '' && userId = @request.auth.id",
    fields: [
      { name: "userId", type: "relation", required: true, maxSelect: 1, collectionId: "_pb_users_auth_" },
      { name: "budgetId", type: "relation", required: true, maxSelect: 1, collectionId: budgets.id, cascadeDelete: true },
      { name: "name", type: "text", required: true, max: 200 },
      { name: "plannedAmount", type: "number", required: true },
      { name: "sortOrder", type: "number", required: true },
      { name: "archived", type: "bool", required: false },
      { name: "updatedAtMillis", type: "number", required: true },
      { name: "deleted", type: "bool", required: false },
    ],
    indexes: [
      "CREATE INDEX idx_wallets_user ON wallets (userId)",
      "CREATE INDEX idx_wallets_budget ON wallets (budgetId)",
      "CREATE INDEX idx_wallets_updated ON wallets (updatedAtMillis)",
    ],
  });
  app.save(wallets);

  // ------------------------------------------------------------------
  // transactions
  // ------------------------------------------------------------------
  let transactions = new Collection({
    type: "base",
    name: "transactions",
    listRule: "@request.auth.id != '' && userId = @request.auth.id",
    viewRule: "@request.auth.id != '' && userId = @request.auth.id",
    createRule: "@request.auth.id != ''",
    updateRule: "@request.auth.id != '' && userId = @request.auth.id",
    deleteRule: "@request.auth.id != '' && userId = @request.auth.id",
    fields: [
      { name: "userId", type: "relation", required: true, maxSelect: 1, collectionId: "_pb_users_auth_" },
      { name: "budgetId", type: "relation", required: true, maxSelect: 1, collectionId: budgets.id, cascadeDelete: true },
      { name: "type", type: "select", required: true, maxSelect: 1, values: ["EXPENSE", "TRANSFER", "CREDIT"] },
      { name: "amount", type: "number", required: true },
      { name: "dateEpochDay", type: "number", required: true },
      { name: "sourceWalletId", type: "relation", required: false, maxSelect: 1, collectionId: wallets.id },
      { name: "destinationWalletId", type: "relation", required: false, maxSelect: 1, collectionId: wallets.id },
      { name: "note", type: "text", required: false, max: 1000 },
      { name: "createdAtMillis", type: "number", required: true },
      { name: "updatedAtMillis", type: "number", required: true },
      { name: "deleted", type: "bool", required: false },
    ],
    indexes: [
      "CREATE INDEX idx_transactions_user ON transactions (userId)",
      "CREATE INDEX idx_transactions_budget ON transactions (budgetId)",
      "CREATE INDEX idx_transactions_updated ON transactions (updatedAtMillis)",
    ],
  });
  app.save(transactions);

  // ------------------------------------------------------------------
  // subscriptions
  // ------------------------------------------------------------------
  let subscriptions = new Collection({
    type: "base",
    name: "subscriptions",
    listRule: "@request.auth.id != '' && userId = @request.auth.id",
    viewRule: "@request.auth.id != '' && userId = @request.auth.id",
    createRule: "",
    updateRule: "@request.auth.id != '' && userId = @request.auth.id",
    deleteRule: "",
    fields: [
      { name: "userId", type: "relation", required: true, maxSelect: 1, collectionId: "_pb_users_auth_" },
      { name: "provider", type: "select", required: true, maxSelect: 1, values: ["paystack", "google_play"] },
      { name: "plan", type: "select", required: true, maxSelect: 1, values: ["pro_monthly", "pro_annual"] },
      { name: "status", type: "select", required: true, maxSelect: 1, values: ["active", "expired", "cancelled"] },
      { name: "startDate", type: "date", required: true },
      { name: "endDate", type: "date", required: true },
      { name: "paystackReference", type: "text", required: false, max: 200 },
      { name: "paystackSubscriptionCode", type: "text", required: false, max: 200 },
      { name: "googlePlayPurchaseToken", type: "text", required: false, max: 500 },
    ],
    indexes: [
      "CREATE INDEX idx_subscriptions_user ON subscriptions (userId)",
      "CREATE INDEX idx_subscriptions_status ON subscriptions (status)",
    ],
  });
  app.save(subscriptions);

  // ------------------------------------------------------------------
  // config (server-configurable feature limits)
  // ------------------------------------------------------------------
  let config = new Collection({
    type: "base",
    name: "config",
    listRule: "",
    viewRule: "",
    createRule: "",
    updateRule: "",
    deleteRule: "",
    fields: [
      { name: "key", type: "text", required: true, max: 100 },
      { name: "value", type: "json", required: true },
    ],
    indexes: [
      "CREATE UNIQUE INDEX idx_config_key ON config (key)",
    ],
  });
  app.save(config);
}, (app) => {
  ["budgets", "wallets", "transactions", "subscriptions", "config"].forEach((name) => {
    try {
      let collection = app.findCollectionByNameOrId(name);
      app.delete(collection);
    } catch {
      // collection already deleted
    }
  });
});
