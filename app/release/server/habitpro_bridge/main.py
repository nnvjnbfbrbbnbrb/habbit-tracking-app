from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse, Response
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from slowapi.util import get_remote_address

from habitpro_bridge import action_queue as aq
from habitpro_bridge.auth_common import resolve_device_queue_uid, verify_bearer
from habitpro_bridge.config import Settings, get_settings
from habitpro_bridge.handlers import handle_message, parse_update
from habitpro_bridge.admin_store import init_admin_store
from habitpro_bridge.admin_api import router as admin_router
from habitpro_bridge.sqlite_queue import SqliteActionQueueStore
from habitpro_bridge.telegram_client import TelegramClient

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

limiter = Limiter(key_func=get_remote_address)
_offset_lock = asyncio.Lock()
_next_offset: int | None = None


def _queue_uid_http(settings: Settings) -> int:
    try:
        return resolve_device_queue_uid(settings)
    except ValueError as e:
        raise HTTPException(503, str(e)) from e


async def _poller_loop(settings: Settings, tg: TelegramClient) -> None:
    global _next_offset
    log.info("Telegram long polling started")
    while True:
        try:
            updates = await tg.get_updates(_next_offset, timeout=50)
            for u in updates:
                upd_id = u.get("update_id")
                if isinstance(upd_id, int):
                    async with _offset_lock:
                        if _next_offset is None:
                            _next_offset = upd_id + 1
                        else:
                            _next_offset = max(_next_offset, upd_id + 1)
                inc = parse_update(u)
                if inc:
                    await handle_message(inc, tg, settings)
        except (httpx.HTTPError, asyncio.CancelledError) as e:
            if isinstance(e, asyncio.CancelledError):
                raise
            log.exception("poller error: %s", e)
            await asyncio.sleep(3)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    if not settings.use_polling and not settings.telegram_webhook_secret:
        log.warning(
            "TELEGRAM_WEBHOOK_SECRET is empty: webhook endpoint is not "
            "header-authenticated. Set secret before exposing /telegram/webhook."
        )
    sqlite_store = SqliteActionQueueStore(settings.queue_db_path)
    await sqlite_store.init_db()
    aq.action_store = sqlite_store
    log.info("Action queue using SQLite at %s", settings.queue_db_path)
    Path(settings.admin_db_path).parent.mkdir(parents=True, exist_ok=True)
    await init_admin_store(settings.admin_db_path)
    log.info("Admin store SQLite at %s", settings.admin_db_path)
    tg = TelegramClient(settings.telegram_bot_token)
    task: asyncio.Task | None = None
    if settings.use_polling:
        task = asyncio.create_task(_poller_loop(settings, tg))
    yield
    if task:
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass


app = FastAPI(title="HabitPro Telegram Bridge", lifespan=lifespan)
app.state.limiter = limiter
app.add_middleware(SlowAPIMiddleware)
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
app.include_router(admin_router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/v1/ping")
@limiter.limit("60/minute")
async def v1_ping(
    request: Request,
    authorization: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
) -> dict[str, str]:
    verify_bearer(authorization, settings)
    return {"status": "ok", "role": "device"}


@app.get("/v1/actions/next", response_model=None)
@limiter.limit("120/minute")
async def v1_actions_next(
    request: Request,
    authorization: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
) -> Response | dict[str, object]:
    verify_bearer(authorization, settings)
    uid = _queue_uid_http(settings)
    action = await aq.action_store.peek_next(uid)
    if action is None:
        return Response(status_code=204)
    return {"id": action.id, "type": action.type, "payload": action.payload}


@app.post("/v1/actions/{action_id}/ack")
@limiter.limit("120/minute")
async def v1_actions_ack(
    request: Request,
    action_id: str,
    authorization: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
) -> dict[str, bool]:
    verify_bearer(authorization, settings)
    uid = _queue_uid_http(settings)
    ok = await aq.action_store.ack(uid, action_id)
    if not ok:
        raise HTTPException(404, "Unknown action id or not next in queue")
    return {"ok": True}


@app.post("/telegram/webhook")
@limiter.limit("120/minute")
async def telegram_webhook(
    request: Request,
    body: dict[str, Any],
    settings: Settings = Depends(get_settings),
    x_telegram_bot_api_secret_token: str | None = Header(default=None, alias="X-Telegram-Bot-Api-Secret-Token"),
) -> JSONResponse:
    if settings.telegram_webhook_secret:
        if x_telegram_bot_api_secret_token != settings.telegram_webhook_secret:
            raise HTTPException(401, "Invalid webhook secret")

    tg = TelegramClient(settings.telegram_bot_token)
    inc = parse_update(body)
    if inc:
        await handle_message(inc, tg, settings)
    return JSONResponse({"ok": True})
