from __future__ import annotations

import logging
from typing import Any

import httpx

log = logging.getLogger(__name__)


class TelegramClient:
    def __init__(self, token: str) -> None:
        self._token = token
        self._base = f"https://api.telegram.org/bot{token}"

    async def send_message(self, chat_id: int, text: str) -> None:
        if not self._token:
            log.warning("TELEGRAM_BOT_TOKEN missing; skip send_message")
            return
        url = f"{self._base}/sendMessage"
        payload = {"chat_id": chat_id, "text": text[:4096]}
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                r = await client.post(url, json=payload)
                r.raise_for_status()
                data: dict[str, Any] = r.json()
                if not data.get("ok"):
                    log.error("Telegram API error: %s", data)
                    raise RuntimeError(str(data.get("description") or data))
        except httpx.HTTPError as e:
            log.exception("Telegram HTTP error: %s", e)
            raise RuntimeError(str(e)) from e

    async def get_updates(self, offset: int | None, timeout: int = 50) -> list[dict[str, Any]]:
        url = f"{self._base}/getUpdates"
        params: dict[str, Any] = {"timeout": timeout}
        if offset is not None:
            params["offset"] = offset
        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                r = await client.get(url, params=params)
                r.raise_for_status()
                data = r.json()
                if not data.get("ok"):
                    log.error("getUpdates failed: %s", data)
                    return []
                return list(data.get("result") or [])
        except httpx.HTTPError as e:
            log.exception("getUpdates HTTP error: %s", e)
            return []
