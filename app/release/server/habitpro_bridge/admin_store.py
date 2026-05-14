from __future__ import annotations

import json
import time
from typing import Any

import aiosqlite

_START_MONO = time.monotonic()


class AdminStore:
    """SQLite persistence for admin logs, reminders, and habit snapshot (bridge-local)."""

    def __init__(self, db_path: str) -> None:
        self._path = db_path

    async def init_db(self) -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ts REAL NOT NULL,
                    message TEXT NOT NULL
                )
                """
            )
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS reminders (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    detail TEXT NOT NULL DEFAULT '',
                    enabled INTEGER NOT NULL DEFAULT 1,
                    created_ts REAL NOT NULL
                )
                """
            )
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS habits_snapshot (
                    user_id TEXT PRIMARY KEY,
                    payload TEXT NOT NULL,
                    updated_ts REAL NOT NULL
                )
                """
            )
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS routines_snapshot (
                    user_id TEXT PRIMARY KEY,
                    payload TEXT NOT NULL,
                    updated_ts REAL NOT NULL
                )
                """
            )
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS telegram_chat_bindings (
                    telegram_user_id INTEGER PRIMARY KEY,
                    chat_id INTEGER NOT NULL,
                    updated_ts REAL NOT NULL
                )
                """
            )
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS device_analytics_bundle (
                    user_id TEXT PRIMARY KEY,
                    payload TEXT NOT NULL,
                    updated_ts REAL NOT NULL
                )
                """
            )
            await db.execute(
                """
                CREATE TABLE IF NOT EXISTS device_backup (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    payload_b64 TEXT NOT NULL,
                    created_ts REAL NOT NULL
                )
                """
            )
            await db.commit()

    async def append_log(self, message: str) -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                "INSERT INTO audit_logs (ts, message) VALUES (?, ?)",
                (time.time(), message[:4000]),
            )
            await db.commit()

    async def list_logs(self, limit: int = 50) -> list[str]:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT message FROM audit_logs ORDER BY id DESC LIMIT ?",
                (limit,),
            ) as cur:
                rows = await cur.fetchall()
        return [str(r["message"]) for r in rows]

    async def clear_logs(self) -> int:
        async with aiosqlite.connect(self._path) as db:
            async with db.execute("SELECT COUNT(*) FROM audit_logs") as cur:
                row = await cur.fetchone()
            n = int(row[0]) if row else 0
            await db.execute("DELETE FROM audit_logs")
            await db.commit()
        return n

    async def set_habits_snapshot(self, user_id: str, habits: list[dict[str, Any]]) -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                """
                INSERT INTO habits_snapshot (user_id, payload, updated_ts)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    payload = excluded.payload,
                    updated_ts = excluded.updated_ts
                """,
                (user_id, json.dumps(habits), time.time()),
            )
            await db.commit()

    async def get_habits_snapshot(self, user_id: str | None) -> tuple[str | None, float | None]:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            if user_id:
                async with db.execute(
                    "SELECT payload, updated_ts FROM habits_snapshot WHERE user_id = ?",
                    (user_id,),
                ) as cur:
                    row = await cur.fetchone()
            else:
                async with db.execute(
                    "SELECT payload, updated_ts FROM habits_snapshot ORDER BY updated_ts DESC LIMIT 1",
                ) as cur:
                    row = await cur.fetchone()
            if row is None:
                return None, None
            return str(row["payload"]), float(row["updated_ts"])

    async def set_routines_snapshot(self, user_id: str, routines: list[dict[str, Any]]) -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                """
                INSERT INTO routines_snapshot (user_id, payload, updated_ts)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    payload = excluded.payload,
                    updated_ts = excluded.updated_ts
                """,
                (user_id, json.dumps(routines), time.time()),
            )
            await db.commit()

    async def get_routines_snapshot(self, user_id: str | None) -> tuple[str | None, float | None]:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            if user_id:
                async with db.execute(
                    "SELECT payload, updated_ts FROM routines_snapshot WHERE user_id = ?",
                    (user_id,),
                ) as cur:
                    row = await cur.fetchone()
            else:
                async with db.execute(
                    "SELECT payload, updated_ts FROM routines_snapshot ORDER BY updated_ts DESC LIMIT 1",
                ) as cur:
                    row = await cur.fetchone()
            if row is None:
                return None, None
            return str(row["payload"]), float(row["updated_ts"])

    async def add_reminder(self, rid: str, title: str, detail: str = "") -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                """
                INSERT INTO reminders (id, title, detail, enabled, created_ts)
                VALUES (?, ?, ?, 1, ?)
                ON CONFLICT(id) DO UPDATE SET title = excluded.title, detail = excluded.detail, enabled = 1
                """,
                (rid, title, detail, time.time()),
            )
            await db.commit()

    async def list_reminders(self) -> list[dict[str, Any]]:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT id, title, detail, enabled FROM reminders ORDER BY created_ts DESC",
            ) as cur:
                rows = await cur.fetchall()
        return [dict(r) for r in rows]

    async def disable_reminder(self, rid: str) -> bool:
        async with aiosqlite.connect(self._path) as db:
            cur = await db.execute("UPDATE reminders SET enabled = 0 WHERE id = ?", (rid,))
            await db.commit()
            return cur.rowcount > 0

    async def remember_chat(self, telegram_user_id: int, chat_id: int) -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                """
                INSERT INTO telegram_chat_bindings (telegram_user_id, chat_id, updated_ts)
                VALUES (?, ?, ?)
                ON CONFLICT(telegram_user_id) DO UPDATE SET
                    chat_id = excluded.chat_id,
                    updated_ts = excluded.updated_ts
                """,
                (telegram_user_id, chat_id, time.time()),
            )
            await db.commit()

    async def get_chat_id(self, telegram_user_id: int) -> int | None:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT chat_id FROM telegram_chat_bindings WHERE telegram_user_id = ?",
                (telegram_user_id,),
            ) as cur:
                row = await cur.fetchone()
        if row is None:
            return None
        return int(row["chat_id"])

    async def set_analytics_bundle(self, user_id: str, payload: dict[str, Any]) -> None:
        async with aiosqlite.connect(self._path) as db:
            uid = user_id.strip()
            await db.execute(
                """
                INSERT INTO device_analytics_bundle (user_id, payload, updated_ts)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    payload = excluded.payload,
                    updated_ts = excluded.updated_ts
                """,
                (uid, json.dumps(payload), time.time()),
            )
            await db.commit()

    async def get_analytics_bundle(self, user_id: str | None) -> dict[str, Any] | None:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            if user_id:
                async with db.execute(
                    "SELECT payload FROM device_analytics_bundle WHERE user_id = ?",
                    (user_id.strip(),),
                ) as cur:
                    row = await cur.fetchone()
            else:
                async with db.execute(
                    "SELECT payload FROM device_analytics_bundle ORDER BY updated_ts DESC LIMIT 1",
                ) as cur:
                    row = await cur.fetchone()
            if row is None:
                return None
            return json.loads(str(row["payload"]))

    async def append_backup(self, user_id: str, payload_b64: str) -> None:
        async with aiosqlite.connect(self._path) as db:
            await db.execute(
                "INSERT INTO device_backup (user_id, payload_b64, created_ts) VALUES (?, ?, ?)",
                (user_id.strip(), payload_b64, time.time()),
            )
            await db.commit()

    async def latest_backup_row(self, user_id: str) -> tuple[str, float] | None:
        async with aiosqlite.connect(self._path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                """
                SELECT payload_b64, created_ts FROM device_backup
                WHERE user_id = ?
                ORDER BY id DESC LIMIT 1
                """,
                (user_id.strip(),),
            ) as cur:
                row = await cur.fetchone()
        if row is None:
            return None
        return str(row["payload_b64"]), float(row["created_ts"])

    def uptime_human(self) -> str:
        sec = int(time.monotonic() - _START_MONO)
        h, sec = divmod(sec, 3600)
        m, sec = divmod(sec, 60)
        return f"{h}h {m}m {sec}s (process)"


_store: AdminStore | None = None


async def init_admin_store(path: str) -> None:
    global _store
    _store = AdminStore(path)
    await _store.init_db()


def get_admin_store() -> AdminStore:
    if _store is None:
        raise RuntimeError("AdminStore not initialized")
    return _store
