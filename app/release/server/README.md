# HabitPro Telegram bridge (VPS)

Small **FastAPI** service: Telegram webhook or long polling, optional **device API** authenticated with a **Bearer token** (`API_SECRET`) you keep in the app — **not** the bot token.

**Scope:** personal automation only. Do not use this to control other people’s devices or bypass consent.

## Security (read this)

- **Never commit** `server/.env` or paste real `TELEGRAM_BOT_TOKEN`, `API_SECRET`, or webhook secrets into chat or a public repo.
- **Android:** use **placeholders only** in git-tracked files. Put `habitpro.api.baseUrl` and `habitpro.api.bearerToken` in **`local.properties`** (normally gitignored); they are wired at build time to `BuildConfig` — see app `build.gradle.kts`.
- **Server:** keep all secrets in **`server/.env`** on the VPS (chmod `600`, not in backups you share).

## Architecture

| Piece | Role |
|--------|------|
| **Telegram** | You message your bot; Telegram delivers updates to your VPS (webhook) or the server polls Telegram (`USE_POLLING=true`). |
| **This server** | Verifies **who** may use the bot (`ALLOWED_TELEGRAM_USER_IDS`), optional **webhook secret** header, **rate limits**, SQLite-backed **action queue** per allowlisted user, HTTP API for your phone. |
| **Android (HabitPro)** | Optional **WorkManager** poll (~15 min): `GET /v1/actions/next` with `Authorization: Bearer <API_SECRET>` — **never** ship `TELEGRAM_BOT_TOKEN` in the APK. |

**Queue note:** actions persist under **`QUEUE_DB_PATH`** (default `data/actions.db`) and survive process restarts. Back up this file when you move VPS.

## Quick start (local)

```bash
cd server
python -m venv .venv
.venv\Scripts\activate   # Windows
# source .venv/bin/activate   # Linux/macOS
pip install -r requirements.txt
copy .env.example .env   # edit: token, secrets, your Telegram user id
```

Set **`ALLOWED_TELEGRAM_USER_IDS`** to your numeric id (message [@userinfobot](https://t.me/userinfobot) on Telegram).

**Option A — polling (no HTTPS):** in `.env` set `USE_POLLING=true`, then:

```bash
uvicorn habitpro_bridge.main:app --host 127.0.0.1 --port 8000
```

**Option B — webhook (production):** `USE_POLLING=false`, put the app behind **HTTPS**, then register the webhook (see below).

- Health: `GET http://127.0.0.1:8000/health`
- Device ping: `GET http://127.0.0.1:8000/v1/ping` with header `Authorization: Bearer <API_SECRET>`

## HTTP API (Android / scripts)

All routes below require:

`Authorization: Bearer <API_SECRET>`

(same secret as `/v1/ping`). If `API_SECRET` is unset, these return **503**.

### `GET /v1/ping`

Returns `{"status":"ok","role":"device"}` — connectivity check.

### `GET /v1/actions/next`

Returns the next **pending** action for the configured device queue user, or **204 No Content** if the queue is empty.

JSON shape:

```json
{
  "id": "<uuid>",
  "type": "SHOW_MESSAGE | VIBRATE_ONCE | PING",
  "payload": {}
}
```

- **`SHOW_MESSAGE`** — `payload` includes `"text": "..."` (from `/notify`).
- **`VIBRATE_ONCE`** / **`PING`** — `payload` is usually `{}`.

The client must call **`POST /v1/actions/{id}/ack`** after handling the action so the server removes it from the head of the queue. Only the **current head** id can be acked (stable FIFO).

### `POST /v1/actions/{id}/ack`

Marks the action done. **404** if the id is unknown or not the next pending item.

### Device queue user id

The phone does **not** send a Telegram user id. The server resolves the queue from env:

- **`DEVICE_QUEUE_TELEGRAM_USER_ID`** — must be one of **`ALLOWED_TELEGRAM_USER_IDS`**.
- If unset and **exactly one** id is allowlisted, that id is used automatically.
- If multiple ids are allowlisted and `DEVICE_QUEUE_TELEGRAM_USER_ID` is unset, the device endpoints return **503** until you set it.

## Telegram commands (allowlisted users only)

Every Telegram path checks **`ALLOWED_TELEGRAM_USER_IDS`**. If the list is empty, the bot refuses all control.

| Command | Description |
|--------|--------------|
| `/start` | Welcome + command summary |
| `/help` or `/commands` | List commands |
| `/status` | Server OK + your queue depth + global pending count |
| `/notify <text>` | Enqueue **SHOW_MESSAGE** for your phone |
| `/vibrate` | Enqueue **VIBRATE_ONCE** |
| `/pingphone` | Enqueue **PING** (phone shows a small notification when it acks) |
| `/clear` | Clear **your** pending queue |
| `/habits`, `/stats` | Demo stubs (unchanged) |
| Natural: “ping my phone” | Same as **PING** |

## Environment variables

See `.env.example`. Important:

- **`TELEGRAM_BOT_TOKEN`** — from BotFather; **server only**.
- **`TELEGRAM_WEBHOOK_SECRET`** — random string; same value passed to `setWebhook` as `secret_token`. Telegram sends it as header `X-Telegram-Bot-Api-Secret-Token`. **If set, requests without a matching header are rejected.**
- **`ALLOWED_TELEGRAM_USER_IDS`** — comma-separated Telegram user ids. **If empty, nobody can use the bot** (fail closed).
- **`API_SECRET`** — long random string; use as **Bearer** token from your phone app (`habitpro.api.bearerToken` in `local.properties`).
- **`DEVICE_QUEUE_TELEGRAM_USER_ID`** — optional; see above.
- **`USE_POLLING`** — `true` for local dev without public URL.
- **`QUEUE_DB_PATH`** — SQLite file for the Telegram→device action queue (default `data/actions.db`).

## Production on a VPS (webhook + HTTPS)

1. **DNS** — `A` record for e.g. `habitpro.example.com` → your VPS IP.
2. **TLS** — use **Caddy** or **nginx + certbot** so `https://habitpro.example.com` terminates TLS and reverse-proxies to `127.0.0.1:8000`.
3. **systemd** — run uvicorn as a dedicated user; load env from `/etc/habitpro-bridge.env` (chmod `600`, root-owned).

Example unit `/etc/systemd/system/habitpro-bridge.service`:

```ini
[Unit]
Description=HabitPro Telegram bridge
After=network.target

[Service]
User=habitpro
Group=habitpro
WorkingDirectory=/opt/habitpro-bridge/server
EnvironmentFile=/etc/habitpro-bridge.env
ExecStart=/opt/habitpro-bridge/server/.venv/bin/uvicorn habitpro_bridge.main:app --host 127.0.0.1 --port 8000
Restart=always

[Install]
WantedBy=multi-user.target
```

Then: `sudo systemctl daemon-reload && sudo systemctl enable --now habitpro-bridge`

4. **nginx** (snippet) — proxy only the paths you need:

```nginx
location / {
    proxy_pass http://127.0.0.1:8000;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

5. **Register Telegram webhook** (replace placeholders):

```bash
SECRET="$(python -c 'import secrets; print(secrets.token_urlsafe(32))')"
echo "Put this in TELEGRAM_WEBHOOK_SECRET and in secret_token below: $SECRET"

curl "https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/setWebhook" \
  -d "url=https://habitpro.example.com/telegram/webhook" \
  -d "secret_token=$SECRET"
```

After that, Telegram will send `X-Telegram-Bot-Api-Secret-Token: <same secret>` on each update.

6. **Rate limiting** — **slowapi** per client IP on webhook, ping, and action routes (see `habitpro_bridge/main.py`). Add **fail2ban** or a cloud WAF for extra protection.

## Android (`local.properties`)

At the **project root** (same folder as `settings.gradle.kts`), add:

```properties
habitpro.api.baseUrl=https://your-vps.example.com
habitpro.api.bearerToken=YOUR_API_SECRET_SAME_AS_SERVER
```

Rebuild the app. If either value is empty, **no** periodic poll is scheduled.

## Files (server package)

- `server/requirements.txt` — Python dependencies.
- `server/.env.example` — template env (no secrets committed).
- `server/habitpro_bridge/` — `main.py`, `config.py`, `handlers.py`, `telegram_client.py`, `action_queue.py`, `sqlite_queue.py`.

## Legal / ethical note

Use only on **your** accounts and devices, with clear consent. Queued actions (**notify / vibrate / ping**) are **intents your own app** chooses to honor when polling with a secret you configured — not hidden surveillance, remote mic/camera, or SMS exfiltration.
