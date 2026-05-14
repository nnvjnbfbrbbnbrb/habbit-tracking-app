from __future__ import annotations

import re
from dataclasses import dataclass

import habitpro_bridge.action_queue as aq
from habitpro_bridge import admin_services as ad
from habitpro_bridge.admin_store import get_admin_store
from habitpro_bridge.auth_common import resolve_device_queue_uid
from habitpro_bridge.config import Settings
from habitpro_bridge.telegram_client import TelegramClient
from habitpro_bridge.telegram_flow import send_safe, with_processing

HELP_TEXT = (
    "HabitPro bridge — commands:\n\n"
    "GROUP 1 — App control:\n"
    "/app_on /app_off /app_status /maintenance on|off\n\n"
    "GROUP 2 — Users (control server):\n"
    "/users /user_info <id> /user_ban <id> /user_unban <id> /broadcast <text>\n\n"
    "GROUP 3 — Habits:\n"
    "/habits /habit_stats /habit_week /habit_top /habit_add <name> [| desc] /habit_delete <id> /streak\n\n"
    "GROUP 3b — Daily routines (device queue + optional snapshot):\n"
    "/routine_list /routine_add <name> /routine_delete <taskId> /routine_stats /routine_report\n"
    "/focus_on /focus_off /sleep_time <sleepH> <wakeH> /wake_time <wakeH> /reward_xp <amount>\n\n"
    "GROUP 4 — Notifications:\n"
    "/notify <text> /reminder_set <title> [| detail] /reminder_list /reminder_off <id> /schedule\n\n"
    "GROUP 5 — Logs:\n"
    "/logs /logs_clear /server_stats\n\n"
    "GROUP 6 — Analytics (requires app bundle sync):\n"
    "/analytics_today /analytics_week /analytics_month\n"
    "/analytics_user <id> (only if ENABLE_MULTI_TENANT_ANALYTICS)\n"
    "/analytics_habit <name>\n"
    "/mood_report /eye_report /sleep_report\n"
    "/backup_pdf_hint — how to fetch PDF from bridge\n\n"
    "Other:\n"
    "/status — bridge action queue\n"
    "/vibrate /pingphone /clear\n"
    "/start /help /commands — this help\n"
    "Natural: “ping my phone”, “list habits”\n\n"
    "Control-server commands need CONTROL_SERVER_URL on the VPS."
)


@dataclass
class IncomingMessage:
    chat_id: int
    user_id: int
    text: str


def parse_update(body: dict) -> IncomingMessage | None:
    msg = body.get("message") or body.get("edited_message")
    if not isinstance(msg, dict):
        return None
    chat = msg.get("chat") or {}
    from_user = msg.get("from") or {}
    chat_id = chat.get("id")
    user_id = from_user.get("id")
    text = (msg.get("text") or "").strip()
    if chat_id is None or user_id is None:
        return None
    try:
        return IncomingMessage(chat_id=int(chat_id), user_id=int(user_id), text=text)
    except (TypeError, ValueError):
        return None


def _command_token(first_word: str) -> str:
    if not first_word.startswith("/"):
        return ""
    return first_word.split("@", 1)[0].lower()


async def handle_message(
    incoming: IncomingMessage,
    tg: TelegramClient,
    settings: Settings,
) -> None:
    uid = incoming.user_id
    allowed = settings.allowed_ids_set
    if not allowed:
        await send_safe(
            tg,
            incoming.chat_id,
            "⛔ Bot misconfigured: set ALLOWED_TELEGRAM_USER_IDS on the server.",
        )
        return
    if uid not in allowed:
        await send_safe(
            tg,
            incoming.chat_id,
            "⛔ You are not authorized to use this bot.",
        )
        return

    try:
        await get_admin_store().remember_chat(uid, incoming.chat_id)
    except Exception:
        pass

    text = incoming.text
    if not text:
        await send_safe(
            tg,
            incoming.chat_id,
            "Send text commands, e.g. /help or /notify Hi.",
        )
        return

    parts = text.split(maxsplit=1)
    cmd = _command_token(parts[0])
    rest = parts[1].strip() if len(parts) > 1 else ""
    lower = text.lower()

    if cmd == "/start":
        await send_safe(
            tg,
            incoming.chat_id,
            "👋 HabitPro bridge online.\n" + HELP_TEXT,
        )
        return

    if cmd in ("/help", "/commands"):
        await send_safe(tg, incoming.chat_id, HELP_TEXT)
        return

    if cmd == "/status":

        async def _status() -> str:
            depth = await aq.action_store.depth(uid)
            total = await aq.action_store.total_pending()
            return (
                f"Bridge OK.\nYour queue: {depth} pending\n"
                f"All users (queued): {total} total pending"
            )

        await with_processing(incoming.chat_id, tg, "/status", _status)
        return

    if cmd == "/app_on":

        async def _w() -> str:
            return await ad.svc_app_on(settings)

        await with_processing(incoming.chat_id, tg, "/app_on", _w)
        return

    if cmd == "/app_off":

        async def _w() -> str:
            return await ad.svc_app_off(settings)

        await with_processing(incoming.chat_id, tg, "/app_off", _w)
        return

    if cmd == "/app_status":

        async def _w() -> str:
            return await ad.svc_app_status(settings)

        await with_processing(incoming.chat_id, tg, "/app_status", _w)
        return

    if cmd == "/maintenance":

        async def _w() -> str:
            r = rest.lower().strip()
            if r in ("on", "1", "true", "yes"):
                return await ad.svc_maintenance(settings, True)
            if r in ("off", "0", "false", "no"):
                return await ad.svc_maintenance(settings, False)
            raise ValueError("Usage: /maintenance on|off")

        await with_processing(incoming.chat_id, tg, "/maintenance", _w)
        return

    if cmd == "/users":

        async def _w() -> str:
            return await ad.svc_users_list(settings)

        await with_processing(incoming.chat_id, tg, "/users", _w)
        return

    if cmd == "/user_info":

        async def _w() -> str:
            return await ad.svc_user_info(settings, rest)

        await with_processing(incoming.chat_id, tg, "/user_info", _w)
        return

    if cmd == "/user_ban":

        async def _w() -> str:
            return await ad.svc_user_ban(settings, rest)

        await with_processing(incoming.chat_id, tg, "/user_ban", _w)
        return

    if cmd == "/user_unban":

        async def _w() -> str:
            return await ad.svc_user_unban(settings, rest)

        await with_processing(incoming.chat_id, tg, "/user_unban", _w)
        return

    if cmd == "/broadcast":

        async def _w() -> str:
            return await ad.svc_broadcast(settings, rest)

        await with_processing(incoming.chat_id, tg, "/broadcast", _w)
        return

    if cmd == "/habits":

        async def _w() -> str:
            return await ad.svc_habits_list(settings, None)

        await with_processing(incoming.chat_id, tg, "/habits", _w)
        return

    if cmd in ("/habit_stats", "/stats"):

        async def _w() -> str:
            return await ad.svc_habit_stats(settings)

        await with_processing(incoming.chat_id, tg, "/habit_stats", _w)
        return

    if cmd == "/habit_week":

        async def _w() -> str:
            return await ad.svc_habit_week(settings)

        await with_processing(incoming.chat_id, tg, "/habit_week", _w)
        return

    if cmd == "/habit_top":

        async def _w() -> str:
            return await ad.svc_habit_top(settings)

        await with_processing(incoming.chat_id, tg, "/habit_top", _w)
        return

    if cmd == "/habit_add":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_habit_add(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/habit_add", _w)
        return

    if cmd == "/habit_delete":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_habit_delete(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/habit_delete", _w)
        return

    if cmd == "/streak":

        async def _w() -> str:
            return await ad.svc_streak(settings)

        await with_processing(incoming.chat_id, tg, "/streak", _w)
        return

    if cmd == "/notify":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_notify(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/notify", _w)
        return

    if cmd == "/reminder_set":

        async def _w() -> str:
            return await ad.svc_reminder_set(settings, rest)

        await with_processing(incoming.chat_id, tg, "/reminder_set", _w)
        return

    if cmd == "/reminder_list":

        async def _w() -> str:
            return await ad.svc_reminder_list(settings)

        await with_processing(incoming.chat_id, tg, "/reminder_list", _w)
        return

    if cmd == "/reminder_off":

        async def _w() -> str:
            return await ad.svc_reminder_off(settings, rest)

        await with_processing(incoming.chat_id, tg, "/reminder_off", _w)
        return

    if cmd == "/schedule":

        async def _w() -> str:
            return await ad.svc_schedule(settings)

        await with_processing(incoming.chat_id, tg, "/schedule", _w)
        return

    if cmd == "/routine_list":

        async def _w() -> str:
            return await ad.svc_routines_list(settings, None)

        await with_processing(incoming.chat_id, tg, "/routine_list", _w)
        return

    if cmd == "/routine_add":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_routine_add(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/routine_add", _w)
        return

    if cmd == "/routine_delete":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_routine_delete(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/routine_delete", _w)
        return

    if cmd == "/routine_stats":

        async def _w() -> str:
            return await ad.svc_routine_stats(settings, None)

        await with_processing(incoming.chat_id, tg, "/routine_stats", _w)
        return

    if cmd == "/routine_report":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_routine_report(settings, q)

        await with_processing(incoming.chat_id, tg, "/routine_report", _w)
        return

    if cmd == "/focus_on":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_focus_on(settings, q)

        await with_processing(incoming.chat_id, tg, "/focus_on", _w)
        return

    if cmd == "/focus_off":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_focus_off(settings, q)

        await with_processing(incoming.chat_id, tg, "/focus_off", _w)
        return

    if cmd == "/sleep_time":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_sleep_time(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/sleep_time", _w)
        return

    if cmd == "/wake_time":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_wake_time(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/wake_time", _w)
        return

    if cmd == "/reward_xp":

        async def _w() -> str:
            q = resolve_device_queue_uid(settings)
            return await ad.svc_reward_xp(settings, q, rest)

        await with_processing(incoming.chat_id, tg, "/reward_xp", _w)
        return

    if cmd == "/logs":

        async def _w() -> str:
            return await ad.svc_logs(settings)

        await with_processing(incoming.chat_id, tg, "/logs", _w)
        return

    if cmd == "/logs_clear":

        async def _w() -> str:
            return await ad.svc_logs_clear(settings)

        await with_processing(incoming.chat_id, tg, "/logs_clear", _w)
        return

    if cmd == "/server_stats":

        async def _w() -> str:
            return await ad.svc_server_stats(settings)

        await with_processing(incoming.chat_id, tg, "/server_stats", _w)
        return

    if cmd == "/vibrate":

        async def _w() -> str:
            await aq.action_store.enqueue(uid, "VIBRATE_ONCE", {})
            return "Queued VIBRATE_ONCE."

        await with_processing(incoming.chat_id, tg, "/vibrate", _w)
        return

    if cmd == "/pingphone":

        async def _w() -> str:
            await aq.action_store.enqueue(uid, "PING", {})
            return "Queued PING (phone will ack)."

        await with_processing(incoming.chat_id, tg, "/pingphone", _w)
        return

    if cmd == "/clear":

        async def _w() -> str:
            n = await aq.action_store.clear(uid)
            return f"Cleared {n} pending action(s) from your queue."

        await with_processing(incoming.chat_id, tg, "/clear", _w)
        return

    if cmd == "/analytics_today":

        async def _w() -> str:
            return await ad.svc_analytics_today(settings)

        await with_processing(incoming.chat_id, tg, "/analytics_today", _w)
        return

    if cmd == "/analytics_week":

        async def _w() -> str:
            return await ad.svc_analytics_week(settings)

        await with_processing(incoming.chat_id, tg, "/analytics_week", _w)
        return

    if cmd == "/analytics_month":

        async def _w() -> str:
            return await ad.svc_analytics_month(settings)

        await with_processing(incoming.chat_id, tg, "/analytics_month", _w)
        return

    if cmd == "/analytics_user":

        async def _w() -> str:
            return await ad.svc_analytics_user(settings, rest)

        await with_processing(incoming.chat_id, tg, "/analytics_user", _w)
        return

    if cmd == "/analytics_habit":

        async def _w() -> str:
            return await ad.svc_analytics_habit(settings, rest)

        await with_processing(incoming.chat_id, tg, "/analytics_habit", _w)
        return

    if cmd == "/mood_report":

        async def _w() -> str:
            return await ad.svc_mood_report(settings)

        await with_processing(incoming.chat_id, tg, "/mood_report", _w)
        return

    if cmd == "/eye_report":

        async def _w() -> str:
            return await ad.svc_eye_report(settings)

        await with_processing(incoming.chat_id, tg, "/eye_report", _w)
        return

    if cmd == "/sleep_report":

        async def _w() -> str:
            return await ad.svc_sleep_report(settings)

        await with_processing(incoming.chat_id, tg, "/sleep_report", _w)
        return

    if cmd == "/backup_pdf_hint":

        async def _w() -> str:
            return (
                "PDF export (MVP):\n"
                "GET /v1/admin/backup/latest.pdf?user_id=<id> with Bearer API_SECRET "
                "(server generates a small PDF stub from latest backup row).\n"
                "Telegram document send from bot can be wired later."
            )

        await with_processing(incoming.chat_id, tg, "/backup_pdf_hint", _w)
        return

    if "list habit" in lower or "list habits" in lower:

        async def _w() -> str:
            return await ad.svc_habits_list(settings, None)

        await with_processing(incoming.chat_id, tg, "habits_nl", _w)
        return

    if "show stat" in lower and cmd not in ("/habit_stats", "/stats"):

        async def _w() -> str:
            return await ad.svc_habit_stats(settings)

        await with_processing(incoming.chat_id, tg, "stats_nl", _w)
        return

    if re.search(r"\bping\b", lower) and ("phone" in lower or "device" in lower or "app" in lower):

        async def _w() -> str:
            await aq.action_store.enqueue(uid, "PING", {})
            return "Queued PING for your phone (same as /pingphone)."

        await with_processing(incoming.chat_id, tg, "ping_nl", _w)
        return

    await send_safe(
        tg,
        incoming.chat_id,
        "🤷 Didn’t understand that. Try /commands or /help.",
    )
