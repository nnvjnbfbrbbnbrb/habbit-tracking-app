from __future__ import annotations

import logging
from typing import Awaitable, Callable

from habitpro_bridge.telegram_client import TelegramClient

log = logging.getLogger(__name__)


async def send_safe(tg: TelegramClient, chat_id: int, text: str) -> None:
    try:
        await tg.send_message(chat_id, text)
    except Exception as e:
        log.exception("Telegram send failed: %s", e)


async def with_processing(
    chat_id: int,
    tg: TelegramClient,
    label: str,
    work: Callable[[], Awaitable[str]],
) -> None:
    await send_safe(tg, chat_id, "⏳ Processing...")
    try:
        result = await work()
        await send_safe(tg, chat_id, f"✅ Result\n{result}")
    except Exception as e:
        log.exception("%s: command failed: %s", label, e)
        await send_safe(tg, chat_id, f"❌ Error: {e}")
