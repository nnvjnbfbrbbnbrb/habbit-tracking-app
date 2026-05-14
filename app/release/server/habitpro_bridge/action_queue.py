from __future__ import annotations

import asyncio
import uuid
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class QueuedAction:
    id: str
    type: str
    payload: dict[str, Any]


class InMemoryActionQueueStore:
    """In-memory FIFO per Telegram user id. Peek until ack removes."""

    def __init__(self) -> None:
        self._lock = asyncio.Lock()
        self._pending: dict[int, list[QueuedAction]] = {}

    async def enqueue(self, user_id: int, action_type: str, payload: dict[str, Any] | None = None) -> QueuedAction:
        action = QueuedAction(
            id=str(uuid.uuid4()),
            type=action_type,
            payload=dict(payload or {}),
        )
        async with self._lock:
            self._pending.setdefault(user_id, []).append(action)
        return action

    async def depth(self, user_id: int) -> int:
        async with self._lock:
            return len(self._pending.get(user_id, ()))

    async def total_pending(self) -> int:
        async with self._lock:
            return sum(len(v) for v in self._pending.values())

    async def peek_next(self, user_id: int) -> QueuedAction | None:
        async with self._lock:
            q = self._pending.get(user_id)
            if not q:
                return None
            return q[0]

    async def ack(self, user_id: int, action_id: str) -> bool:
        async with self._lock:
            q = self._pending.get(user_id)
            if not q:
                return False
            if q[0].id != action_id:
                # Stable ordering: only head is "delivered" to client
                return False
            q.pop(0)
            if not q:
                del self._pending[user_id]
            return True

    async def clear(self, user_id: int) -> int:
        async with self._lock:
            n = len(self._pending.get(user_id, ()))
            self._pending.pop(user_id, None)
            return n


# Default in-memory store until FastAPI lifespan replaces with SQLite (see main.py).
action_store: Any = InMemoryActionQueueStore()
