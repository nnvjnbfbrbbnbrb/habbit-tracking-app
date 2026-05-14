from __future__ import annotations

import asyncio
import json
import uuid
from pathlib import Path
from typing import Any

import aiosqlite

from habitpro_bridge.action_queue import QueuedAction


class SqliteActionQueueStore:
    """Persistent FIFO per Telegram user id (SQLite). Survives process restarts."""

    def __init__(self, db_path: str) -> None:
        self._path = str(Path(db_path))
        self._lock = asyncio.Lock()

    async def init_db(self) -> None:
        Path(self._path).parent.mkdir(parents=True, exist_ok=True)
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS pending_actions (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    payload TEXT NOT NULL
                )
                """
            )
            await db.commit()

    async def enqueue(
        self, user_id: int, action_type: str, payload: dict[str, Any] | None = None
    ) -> QueuedAction:
        action = QueuedAction(
            id=str(uuid.uuid4()),
            type=action_type,
            payload=dict(payload or {}),
        )
        async with self._lock:
            async with aiosqlite.connect(self._path) as db:
                await db.execute(
                    "INSERT INTO pending_actions (user_id, id, type, payload) VALUES (?, ?, ?, ?)",
                    (user_id, action.id, action.type, json.dumps(action.payload)),
                )
                await db.commit()
        return action

    async def depth(self, user_id: int) -> int:
        async with self._lock:
            async with aiosqlite.connect(self._path) as db:
                async with db.execute(
                    "SELECT COUNT(*) FROM pending_actions WHERE user_id = ?",
                    (user_id,),
                ) as cur:
                    row = await cur.fetchone()
                return int(row[0]) if row else 0

    async def total_pending(self) -> int:
        async with self._lock:
            async with aiosqlite.connect(self._path) as db:
                async with db.execute("SELECT COUNT(*) FROM pending_actions") as cur:
                    row = await cur.fetchone()
                return int(row[0]) if row else 0

    async def peek_next(self, user_id: int) -> QueuedAction | None:
        async with self._lock:
            async with aiosqlite.connect(self._path) as db:
                db.row_factory = aiosqlite.Row
                async with db.execute(
                    "SELECT id, type, payload FROM pending_actions WHERE user_id = ? ORDER BY rowid ASC LIMIT 1",
                    (user_id,),
                ) as cur:
                    row = await cur.fetchone()
                if row is None:
                    return None
                return QueuedAction(
                    id=str(row["id"]),
                    type=str(row["type"]),
                    payload=json.loads(row["payload"]),
                )

    async def ack(self, user_id: int, action_id: str) -> bool:
        async with self._lock:
            async with aiosqlite.connect(self._path) as db:
                db.row_factory = aiosqlite.Row
                async with db.execute(
                    "SELECT rowid, id FROM pending_actions WHERE user_id = ? ORDER BY rowid ASC LIMIT 1",
                    (user_id,),
                ) as cur:
                    row = await cur.fetchone()
                if row is None:
                    return False
                if str(row["id"]) != action_id:
                    return False
                await db.execute("DELETE FROM pending_actions WHERE rowid = ?", (int(row["rowid"]),))
                await db.commit()
            return True

    async def clear(self, user_id: int) -> int:
        async with self._lock:
            async with aiosqlite.connect(self._path) as db:
                async with db.execute(
                    "SELECT COUNT(*) FROM pending_actions WHERE user_id = ?",
                    (user_id,),
                ) as cur:
                    row = await cur.fetchone()
                n = int(row[0]) if row else 0
                await db.execute("DELETE FROM pending_actions WHERE user_id = ?", (user_id,))
                await db.commit()
            return n
