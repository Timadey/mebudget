# PocketBase Setup

## Prerequisites

- [PocketBase binary](https://pocketbase.io/docs/) (latest v0.39.x) for your platform
- A domain or IP with SSL for production (Let's Encrypt recommended)
- For local development: just the binary, no domain needed

## Installation (Local Development)

```bash
# 1. Download PocketBase for your OS from https://pocketbase.io/docs/
# 2. Place the binary in the pocketbase/ directory
# 3. Run migrations and start the server
./pocketbase migrate up
./pocketbase serve --http=127.0.0.1:8090
```

The admin UI is available at `http://127.0.0.1:8090/_/`. Create your first superuser account there.

## Android Emulator Access

The Android emulator reaches the host machine via `10.0.2.2`, so the app's default dev base URL is:

```
http://10.0.2.2:8090
```

For a physical device, use your machine's LAN IP and run:

```bash
./pocketbase serve --http=0.0.0.0:8090
```

## Production Hosting

Recommended: Railway, Fly.io, or a small VPS. PocketBase is a single binary.

```bash
# Example: run with a persistent data dir
./pocketbase serve --http=0.0.0.0:8090 --dir=/data
```

Environment variables:

| Variable | Purpose |
|----------|---------|
| `POCKETBASE_URL` | Public base URL, e.g. `https://pb.yourdomain.com` |
| `POCKETBASE_ADMIN_EMAIL` | Superuser email (used by seeding scripts) |
| `POCKETBASE_ADMIN_PASSWORD` | Superuser password |
| `POCKETBASE_PAYSTACK_SECRET_KEY` | Paystack secret key for webhook verification |

## Collections

Created by `pb_migrations/001_initial.js`:

| Collection | Type | Purpose |
|------------|------|---------|
| `users` | auth (built-in) | Email + Google authentication |
| `budgets` | base | Synced budget records |
| `wallets` | base | Synced wallet records (relates to budgets) |
| `transactions` | base | Synced transaction records (relates to budgets/wallets) |
| `subscriptions` | base | Pro subscription status per user |
| `config` | base | Server-configurable limits / feature flags |

## Access Rules

All user data collections (`budgets`, `wallets`, `transactions`, `subscriptions`) restrict list/view/create/update/delete to records where `userId = @request.auth.id`. This means each user can only ever read or write their own data.

## Seeding Default Config

```bash
# After first serve, seed default limits via the admin API:
./pocketbase serve &
curl -X POST http://127.0.0.1:8090/api/collections/config/records \
  -H "Authorization: Bearer <superuser-token>" \
  -H "Content-Type: application/json" \
  -d @pocketbase/seed/default_config.json
```

## Google OAuth2 (Sign in with Google)

1. Create a Google Cloud project and configure an OAuth 2.0 Client ID.
2. In PocketBase admin UI: Settings → Auth Providers → Google.
3. Paste the Client ID and Client Secret and enable the provider.
4. For Android, the Credential Manager flow returns a Google ID token, which PocketBase exchanges server-side for a user session.

## Paystack Webhook

PocketBase processes Paystack webhook events (e.g. `subscription.create`, `charge.success`) via the JS hook in `pocketbase/pb_hooks/paystack.js`. Deploy that file alongside the binary so it loads at startup.

```bash
# Directory layout for a deployment
pocketbase
├── pocketbase            # the binary
├── pb_data/              # SQLite data (auto-created)
├── pb_hooks/             # JS hooks
│   └── paystack.js
└── pb_migrations/        # migrations (auto-applied on serve)
```
