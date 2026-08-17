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

### Deploying on Coolify (recommended)

Coolify builds this folder as a Docker image via the included `Dockerfile`
(which bakes in `pb_migrations/` and `pb_hooks/`) and the `docker-compose.yml`.

1. Push this repo to GitHub/GitLab and, in Coolify, create a new resource.
   - Source: your git repo (public or private via GitHub App / Deploy Key).
   - Build pack: **Dockerfile** (or **Docker Compose** with `docker-compose.yml`).
   - Base directory: `pocketbase` (this folder).
2. Set the exposed port to `8090` in the resource's Network settings.
3. Add a **Persistent Storage** volume mounted at `/pb/pb_data`
   (the SQLite DB + uploads must survive redeploys).
4. Set environment variables (Environment Variables tab):

   | Variable | Purpose |
   |----------|---------|
   | `POCKETBASE_PAYSTACK_SECRET_KEY` | Paystack secret key for webhook verification |
   | `POCKETBASE_URL` | Optional; public base URL, e.g. `https://pb.yourdomain.com` |
   | `POCKETBASE_ADMIN_EMAIL` | Optional; superuser email (used by seeding scripts) |
   | `POCKETBASE_ADMIN_PASSWORD` | Optional; superuser password |

5. Deploy. The webhook route becomes available at
   `https://pb.yourdomain.com/api/paystack/webhook` and at base `/_/` create your
   first superuser, then seed the config (below).

Note: migrations auto-apply at startup, and because hooks/migrations live in the
image, a fresh `Deploy` picks up any changes you push to `pb_hooks/` or
`pb_migrations/`. Your data is untouched (it lives in the mounted volume).

### Alternative: bare VPS / other PaaS

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

- `budgets`, `wallets`, `transactions`: list/view/update/delete restricted to records where `userId = @request.auth.id`, so each user can only ever read or write their own data.
- `subscriptions`: lock/view owner-only; create/update/delete are **locked** (superuser-only) because subscription rows are written exclusively by the Paystack webhook — prevents free users from granting themselves Pro.
- `config`: public read (`listRule`/`viewRule` = `""`); writes are **locked** to superusers.

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

PocketBase processes Paystack webhook events (e.g. `subscription.create`, `charge.success`) via the JS hook in `pocketbase/pb_hooks/paystack.pb.js`.

> **Note:** PocketBase only auto-loads hook files with a `*.pb.js` suffix. Keep the
> file named `paystack.pb.js` — a plain `paystack.js` would be treated as a
> `require()`-able module and silently ignored (no route registered).

- **On Coolify**: the hook is baked into the image by the Dockerfile, so no extra step — just deploy.
- **Bare VPS**: copy the file alongside the binary so it loads at startup.

```bash
# Directory layout for a bare deployment
pocketbase
├── pocketbase            # the binary
├── pb_data/              # SQLite data (auto-created)
├── pb_hooks/             # JS hooks (*.pb.js are auto-loaded)
│   └── paystack.pb.js
└── pb_migrations/        # migrations (auto-applied on serve)
```

Then set `POCKETBASE_PAYSTACK_SECRET_KEY` in the environment and point Paystack's
dashboard webhook at:

```
https://pb.yourdomain.com/api/paystack/webhook
```
