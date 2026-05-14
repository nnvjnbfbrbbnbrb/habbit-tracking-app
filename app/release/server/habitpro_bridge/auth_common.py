from __future__ import annotations

from fastapi import HTTPException

from habitpro_bridge.config import Settings


def verify_bearer(authorization: str | None, settings: Settings) -> None:
    if not settings.api_secret:
        raise HTTPException(503, "API_SECRET not configured")
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(401, "Missing Bearer token")
    token = authorization.split(" ", 1)[1].strip()
    if token != settings.api_secret:
        raise HTTPException(403, "Invalid token")


def resolve_device_queue_uid(settings: Settings) -> int:
    allowed = settings.allowed_ids_set
    if not allowed:
        raise ValueError("ALLOWED_TELEGRAM_USER_IDS is empty")
    explicit = settings.device_queue_telegram_user_id
    if explicit is not None:
        if explicit not in allowed:
            raise ValueError(
                "DEVICE_QUEUE_TELEGRAM_USER_ID must appear in ALLOWED_TELEGRAM_USER_IDS",
            )
        return explicit
    if len(allowed) == 1:
        return next(iter(allowed))
    raise ValueError(
        "Set DEVICE_QUEUE_TELEGRAM_USER_ID to the Telegram user id whose queue this phone polls, "
        "or allow exactly one user id.",
    )
