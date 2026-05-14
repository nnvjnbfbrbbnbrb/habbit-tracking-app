from __future__ import annotations

import json
import time
import uuid
from typing import Any

from habitpro_bridge import action_queue as aq
from habitpro_bridge import analytics_formatters
from habitpro_bridge.admin_store import get_admin_store
from habitpro_bridge.auth_common import resolve_device_queue_uid
from habitpro_bridge.config import Settings
from habitpro_bridge.control_proxy import control_get, control_post_empty, control_post_json


def _http_ok(code: int) -> bool:
    return 200 <= code < 300


def _require_control(settings: Settings) -> None:
    if not (settings.control_server_url or "").strip():
        raise ValueError("CONTROL_SERVER_URL is not set on the bridge.")


async def svc_app_on(settings: Settings) -> str:
    _require_control(settings)
    code, body = await control_post_empty(
        settings,
        ["/app-on", "/app/on", "/control/app-on", "/app/enable"],
    )
    if _http_ok(code):
        return f"HTTP {code}\n{(body or '').strip()[:1200]}"
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_app_off(settings: Settings) -> str:
    _require_control(settings)
    code, body = await control_post_empty(
        settings,
        ["/app-off", "/app/off", "/control/app-off", "/app/disable"],
    )
    if _http_ok(code):
        return f"HTTP {code}\n{(body or '').strip()[:1200]}"
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_app_status(settings: Settings) -> str:
    _require_control(settings)
    code, body = await control_get(
        settings,
        ["/app-status", "/app/status", "/status/app", "/control/app-status"],
    )
    if _http_ok(code):
        return f"HTTP {code}\n{(body or '').strip()[:1200]}"
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_maintenance(settings: Settings, enabled: bool) -> str:
    _require_control(settings)
    variants: list[dict[str, Any]] = [
        {"maintenance": enabled},
        {"enabled": not enabled},
        {"mode": "maintenance" if enabled else "normal"},
    ]
    last_code, last_body = 0, ""
    for body in variants:
        code, text = await control_post_json(
            settings,
            ["/maintenance", "/app/maintenance", "/control/maintenance"],
            body,
        )
        last_code, last_body = code, text
        if _http_ok(code):
            return f"maintenance={'on' if enabled else 'off'}\nHTTP {code}\n{text.strip()[:1200]}"
    raise ValueError(f"HTTP {last_code}: {(last_body or '')[:600]}")


async def svc_users_list(settings: Settings) -> str:
    _require_control(settings)
    code, body = await control_get(settings, ["/users", "/user/list", "/admin/users"])
    if _http_ok(code):
        return (body or "").strip()[:3500]
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_user_info(settings: Settings, user_id: str) -> str:
    _require_control(settings)
    uid = user_id.strip()
    if not uid:
        raise ValueError("Missing user id. Usage: /user_info <userId>")
    code, body = await control_get(
        settings,
        [
            f"/user/{uid}",
            f"/users/{uid}",
            f"/user-info/{uid}",
            f"/admin/user/{uid}",
        ],
    )
    if _http_ok(code):
        return (body or "").strip()[:3500]
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_user_ban(settings: Settings, user_id: str) -> str:
    _require_control(settings)
    uid = user_id.strip()
    if not uid:
        raise ValueError("Missing user id. Usage: /user_ban <userId>")
    code, body = await control_post_json(
        settings,
        ["/user-ban", "/ban-user", f"/users/{uid}/ban", "/admin/ban"],
        {"userId": uid},
    )
    if _http_ok(code):
        return f"banned {uid}\nHTTP {code}\n{(body or '').strip()[:800]}"
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_user_unban(settings: Settings, user_id: str) -> str:
    _require_control(settings)
    uid = user_id.strip()
    if not uid:
        raise ValueError("Missing user id. Usage: /user_unban <userId>")
    code, body = await control_post_json(
        settings,
        ["/user-unban", "/unban-user", f"/users/{uid}/unban", "/admin/unban"],
        {"userId": uid},
    )
    if _http_ok(code):
        return f"unbanned {uid}\nHTTP {code}\n{(body or '').strip()[:800]}"
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


async def svc_broadcast(settings: Settings, message: str) -> str:
    _require_control(settings)
    if not message.strip():
        raise ValueError("Missing text. Usage: /broadcast <message>")
    code, body = await control_post_json(
        settings,
        ["/broadcast", "/admin/broadcast", "/notify-all"],
        {"message": message, "text": message},
    )
    if _http_ok(code):
        return f"HTTP {code}\n{(body or '').strip()[:1200]}"
    raise ValueError(f"HTTP {code}: {(body or '')[:600]}")


def _parse_habits_json(raw: str | None) -> list[dict[str, Any]]:
    if not raw:
        return []
    data = json.loads(raw)
    if isinstance(data, list):
        return [x for x in data if isinstance(x, dict)]
    raise ValueError("Invalid habits snapshot JSON.")


async def _load_snapshot(settings: Settings, explicit_user: str | None) -> tuple[list[dict[str, Any]], str | None]:
    store = get_admin_store()
    uid = explicit_user
    raw, _ts = await store.get_habits_snapshot(uid)
    habits = _parse_habits_json(raw) if raw else []
    return habits, uid


async def svc_habits_list(settings: Settings, user_id: str | None) -> str:
    habits, uid = await _load_snapshot(settings, user_id)
    if not habits:
        return (
            "No habit snapshot on the bridge yet.\n"
            "POST /v1/admin/habits/snapshot with Bearer API_SECRET, "
            "or sync the app so the bridge receives habit JSON."
        )
    suffix = f", user_id={uid}" if uid else ""
    lines = [f"{len(habits)} habits{suffix}"]
    for h in habits[:40]:
        name = h.get("name") or h.get("title") or "?"
        hid = h.get("id", "?")
        streak = h.get("streak_days", h.get("streakDays", "?"))
        lines.append(f"• {name} (id={hid}, streak={streak})")
    if len(habits) > 40:
        lines.append(f"… and {len(habits) - 40} more")
    return "\n".join(lines)


async def svc_habit_stats(settings: Settings) -> str:
    habits, _ = await _load_snapshot(settings, None)
    if not habits:
        return (
            "No habit snapshot.\n"
            "Use POST /v1/admin/habits/snapshot or wire the app to push habits to the bridge."
        )
    active = sum(1 for h in habits if h.get("is_active", h.get("isActive", True)))
    streaks = [int(h.get("streak_days", h.get("streakDays", 0)) or 0) for h in habits]
    bests = [int(h.get("best_streak", h.get("bestStreak", 0)) or 0) for h in habits]
    done_today = sum(
        1 for h in habits if h.get("is_completed_today", h.get("isCompletedToday", False))
    )
    return (
        f"habits={len(habits)}, active≈{active}\n"
        f"completed_today_flag={done_today}\n"
        f"max current streak={max(streaks) if streaks else 0}\n"
        f"max best streak={max(bests) if bests else 0}"
    )


async def svc_habit_week(settings: Settings) -> str:
    habits, _ = await _load_snapshot(settings, None)
    if not habits:
        return "No habit snapshot for week view."
    now = int(time.time() * 1000)
    window = 7 * 86400 * 1000
    n = 0
    lines: list[str] = []
    for h in habits:
        last = int(h.get("last_completed_at", h.get("lastCompletedAt", 0)) or 0)
        if last and (now - last) <= window:
            n += 1
            name = h.get("name") or "?"
            lines.append(f"• {name} (last_completed_at within 7d)")
    if not lines:
        return "No habits with last_completed_at in the last 7 days (wire snapshot may omit history)."
    return f"{n} habit(s) with recent completion:\n" + "\n".join(lines[:30])


async def svc_habit_top(settings: Settings) -> str:
    habits, _ = await _load_snapshot(settings, None)
    if not habits:
        return "No habit snapshot."
    ranked = sorted(
        habits,
        key=lambda h: int(h.get("streak_days", h.get("streakDays", 0)) or 0),
        reverse=True,
    )
    out = []
    for i, h in enumerate(ranked[:15], start=1):
        name = h.get("name") or "?"
        sid = h.get("id", "?")
        st = h.get("streak_days", h.get("streakDays", 0))
        out.append(f"{i}. {name} — streak={st} (id={sid})")
    return "\n".join(out)


async def svc_streak(settings: Settings) -> str:
    habits, _ = await _load_snapshot(settings, None)
    if not habits:
        return "No habit snapshot."
    best = max(habits, key=lambda h: int(h.get("streak_days", h.get("streakDays", 0)) or 0))
    name = best.get("name") or "?"
    st = int(best.get("streak_days", best.get("streakDays", 0)) or 0)
    bid = best.get("id", "?")
    return f"Top current streak: {st} days — {name} (id={bid})"


async def svc_habit_add(settings: Settings, queue_uid: int, rest: str) -> str:
    name, desc = _split_pipe(rest)
    if not name:
        raise ValueError("Usage: /habit_add <name> [| optional description]")
    action = await aq.action_store.enqueue(
        queue_uid,
        "HABIT_ADD",
        {
            "name": name,
            "description": desc,
            "icon": "📝",
            "color": "#4CAF50",
            "category": "telegram",
        },
    )
    return f"Queued HABIT_ADD id={action.id} for device queue user {queue_uid}."


async def svc_habit_delete(settings: Settings, queue_uid: int, habit_id: str) -> str:
    hid = habit_id.strip()
    if not hid:
        raise ValueError("Usage: /habit_delete <habitId>")
    action = await aq.action_store.enqueue(queue_uid, "HABIT_DELETE", {"habit_id": hid})
    return f"Queued HABIT_DELETE id={action.id} habit_id={hid} for queue user {queue_uid}."


async def svc_notify(settings: Settings, queue_uid: int, message: str) -> str:
    if not message.strip():
        raise ValueError("Usage: /notify <text>")
    action = await aq.action_store.enqueue(queue_uid, "SHOW_MESSAGE", {"text": message})
    return f"Queued SHOW_MESSAGE id={action.id} ({len(message)} chars)."


async def svc_reminder_set(settings: Settings, rest: str) -> str:
    title, detail = _split_pipe(rest)
    if not title:
        raise ValueError("Usage: /reminder_set <title> [| detail]")
    rid = uuid.uuid4().hex[:10]
    await get_admin_store().add_reminder(rid, title, detail)
    return f"Reminder saved id={rid}\n{title}" + (f"\n{detail}" if detail else "")


async def svc_reminder_list(settings: Settings) -> str:
    rows = await get_admin_store().list_reminders()
    if not rows:
        return "No reminders stored on the bridge."
    lines = []
    for r in rows:
        en = "on" if r.get("enabled") else "off"
        lines.append(f"• [{r['id']}] {r['title']} ({en})")
        if r.get("detail"):
            lines.append(f"    {r['detail']}")
    return "\n".join(lines)


async def svc_reminder_off(settings: Settings, rid: str) -> str:
    rid = rid.strip()
    if not rid:
        raise ValueError("Usage: /reminder_off <reminderId>")
    ok = await get_admin_store().disable_reminder(rid)
    if not ok:
        raise ValueError(f"No reminder with id={rid}")
    return f"Reminder {rid} disabled."


async def svc_schedule(settings: Settings) -> str:
    reminders = await get_admin_store().list_reminders()
    active = [r for r in reminders if r.get("enabled")]
    lines = ["Bridge reminder schedule (stored locally):"]
    if not active:
        lines.append("(none enabled)")
    else:
        for r in active:
            lines.append(f"• {r['title']} — id={r['id']}")
    lines.append("")
    lines.append(
        "Device notifications still use Android local scheduling; "
        "this list is admin bookkeeping on the VPS."
    )
    return "\n".join(lines)


async def svc_logs(settings: Settings) -> str:
    entries = await get_admin_store().list_logs(40)
    if not entries:
        return "No audit lines yet (bridge admin log)."
    return "\n".join(f"• {e[:500]}" for e in entries)


async def svc_logs_clear(settings: Settings) -> str:
    n = await get_admin_store().clear_logs()
    return f"Cleared {n} audit line(s)."


async def svc_server_stats(settings: Settings) -> str:
    store = get_admin_store()
    uptime = store.uptime_human()
    q_uid = resolve_device_queue_uid(settings)
    depth = await aq.action_store.depth(q_uid)
    total = await aq.action_store.total_pending()
    ctrl = (settings.control_server_url or "").strip() or "(not set)"
    return (
        f"uptime: {uptime}\n"
        f"control_server: {ctrl}\n"
        f"device_queue_uid: {q_uid}\n"
        f"queue_depth(user): {depth}\n"
        f"queue_total(all users): {total}"
    )


async def svc_set_habits_snapshot(settings: Settings, user_id: str, habits: list[dict[str, Any]]) -> str:
    uid = user_id.strip()
    if not uid:
        raise ValueError("user_id required")
    await get_admin_store().set_habits_snapshot(uid, habits)
    return f"Snapshot stored for user_id={uid} ({len(habits)} habits)."


def _split_pipe(rest: str) -> tuple[str, str]:
    rest = rest.strip()
    if "|" in rest:
        a, b = rest.split("|", 1)
        return a.strip(), b.strip()
    return rest, ""


async def _load_bundle(settings: Settings, explicit_user: str | None) -> dict[str, Any] | None:
    store = get_admin_store()
    uid = explicit_user.strip() if explicit_user else None
    return await store.get_analytics_bundle(uid)


async def svc_post_analytics_bundle(
    settings: Settings,
    user_id: str,
    habits: list[dict[str, Any]],
    completions: list[dict[str, Any]],
    usage_screen_summary: str | None,
    sleep_bed_hour: int | None,
    sleep_wake_hour: int | None,
) -> str:
    uid = user_id.strip()
    if not uid:
        raise ValueError("user_id required")
    payload: dict[str, Any] = {
        "user_id": uid,
        "habits": habits,
        "completions": completions,
        "usage_screen_summary": usage_screen_summary,
        "sleep_bed_hour": sleep_bed_hour,
        "sleep_wake_hour": sleep_wake_hour,
    }
    await get_admin_store().set_analytics_bundle(uid, payload)
    await get_admin_store().set_habits_snapshot(uid, habits)
    return f"Analytics bundle stored for {uid} ({len(completions)} completions)."


async def svc_telegram_report(settings: Settings, queue_uid: int, text: str) -> str:
    from habitpro_bridge.telegram_client import TelegramClient

    chat_id = await get_admin_store().get_chat_id(queue_uid)
    if chat_id is None:
        return "No Telegram chat binding yet — send any /command to the bot from your account once."
    tg = TelegramClient(settings.telegram_bot_token)
    await tg.send_message(chat_id, text)
    return "Sent Telegram report."


async def svc_analytics_today(settings: Settings) -> str:
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet. Open the app on the phone (Pulse tab) or wait for background sync."
    keys, rates = analytics_formatters.bundle_daily_series(b, days=1)
    spark = analytics_formatters.ascii_sparkline(rates[-7:])
    habits = b.get("habits") or []
    done = sum(1 for h in habits if h.get("is_completed_today", h.get("isCompletedToday", False)))
    return f"Today (approx from bundle)\nhabits_done_flag={done}\nspark7: {spark}\n"


async def svc_analytics_week(settings: Settings) -> str:
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet."
    _, rates = analytics_formatters.bundle_daily_series(b, days=7)
    spark = analytics_formatters.ascii_sparkline(rates)
    avg = sum(rates) / len(rates) if rates else 0.0
    return f"Last 7 local days (approx)\navg completion≈{avg * 100:.0f}%\n{spark}\n"


async def svc_analytics_month(settings: Settings) -> str:
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet."
    _, rates = analytics_formatters.bundle_daily_series(b, days=30)
    spark = analytics_formatters.ascii_sparkline(rates[-30:])
    avg = sum(rates) / len(rates) if rates else 0.0
    return f"Last 30 local days (approx)\navg completion≈{avg * 100:.0f}%\n{spark}\n"


async def svc_analytics_user(settings: Settings, rest: str) -> str:
    if not settings.enable_multi_tenant_analytics:
        return "Single-user mode: omit /analytics_user (bridge DEVICE_QUEUE_TELEGRAM_USER_ID is the only device user)."
    uid = rest.strip()
    if not uid:
        raise ValueError("Usage: /analytics_user <user_id>")
    b = await _load_bundle(settings, uid)
    if not b:
        return f"No bundle for user_id={uid}"
    _, rates = analytics_formatters.bundle_daily_series(b, days=14)
    spark = analytics_formatters.ascii_sparkline(rates)
    return f"User {uid} — 14d spark:\n{spark}\n"


async def svc_analytics_habit(settings: Settings, rest: str) -> str:
    name = rest.strip().lower()
    if not name:
        raise ValueError("Usage: /analytics_habit <name substring>")
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet."
    habits = b.get("habits") or []
    hid = None
    for h in habits:
        nm = str(h.get("name", "")).lower()
        if name in nm:
            hid = str(h.get("id", ""))
            break
    if not hid:
        return f"No habit matching “{rest.strip()}”."
    comp = b.get("completions") or []
    hits = [c for c in comp if str(c.get("habit_id", c.get("habitId", ""))) == hid]
    return f"Habit id={hid}\ncompletions in bundle: {len(hits)}\n(last ~120d pushed from phone)\n"


async def svc_mood_report(settings: Settings) -> str:
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet."
    return analytics_formatters.mood_summary_from_bundle(b)


async def svc_eye_report(settings: Settings) -> str:
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet."
    return analytics_formatters.eye_screen_proxy_from_bundle(b)


async def svc_sleep_report(settings: Settings) -> str:
    b = await _load_bundle(settings, None)
    if not b:
        return "No analytics bundle yet."
    return analytics_formatters.sleep_summary_from_bundle(b)


async def svc_backup_post(settings: Settings, user_id: str, payload_b64: str) -> str:
    uid = user_id.strip()
    if not uid or not payload_b64.strip():
        raise ValueError("user_id and payload_b64 required")
    await get_admin_store().append_backup(uid, payload_b64.strip())
    return f"Backup stored for {uid}."


async def svc_backup_pdf(settings: Settings, user_id: str) -> bytes:
    row = await get_admin_store().latest_backup_row(user_id.strip())
    if row is None:
        raise ValueError("No backup for user")
    try:
        from reportlab.lib.pagesizes import letter
        from reportlab.pdfgen import canvas
    except Exception as e:  # pragma: no cover
        raise ValueError("reportlab not installed on server") from e
    import io

    buf = io.BytesIO()
    c = canvas.Canvas(buf, pagesize=letter)
    c.drawString(72, 720, "HabitPro backup summary (encrypted payload not decoded here).")
    c.drawString(72, 700, f"user_id={user_id}")
    c.drawString(72, 680, f"stored_b64_chars={len(row[0])}")
    c.showPage()
    c.save()
    return buf.getvalue()


def _parse_routines_json(raw: str | None) -> list[dict[str, Any]]:
    if not raw:
        return []
    data = json.loads(raw)
    if isinstance(data, list):
        return [x for x in data if isinstance(x, dict)]
    if isinstance(data, dict) and "routines" in data:
        v = data["routines"]
        if isinstance(v, list):
            return [x for x in v if isinstance(x, dict)]
    raise ValueError("Invalid routines snapshot JSON.")


async def svc_set_routines_snapshot(settings: Settings, user_id: str, routines: list[dict[str, Any]]) -> str:
    uid = user_id.strip()
    if not uid:
        raise ValueError("user_id required")
    await get_admin_store().set_routines_snapshot(uid, routines)
    return f"Routine snapshot stored for user_id={uid} ({len(routines)} routines)."


async def svc_routines_list(settings: Settings, user_id: str | None) -> str:
    raw, _ts = await get_admin_store().get_routines_snapshot(user_id)
    routines = _parse_routines_json(raw) if raw else []
    if not routines:
        return "No routine snapshot yet. Use POST /v1/admin/routines/snapshot from the app (optional)."
    lines = [f"{len(routines)} routines"]
    for r in routines[:40]:
        lines.append(f"• {r.get('name', '?')} (id={r.get('id', '?')}, enabled={r.get('enabled', True)})")
    if len(routines) > 40:
        lines.append(f"… and {len(routines) - 40} more")
    return "\n".join(lines)


async def svc_routine_add(settings: Settings, queue_uid: int, rest: str) -> str:
    name, _ = _split_pipe(rest)
    if not name.strip():
        raise ValueError("Usage: /routine_add <name> [| optional note]")
    action = await aq.action_store.enqueue(
        queue_uid,
        "ROUTINE_ADD",
        {"name": name.strip(), "time_minutes": 9 * 60, "days_mask": 127, "repeat_count": 1},
    )
    return f"Queued ROUTINE_ADD id={action.id} for device queue user {queue_uid}."


async def svc_routine_delete(settings: Settings, queue_uid: int, task_id: str) -> str:
    tid = task_id.strip()
    if not tid:
        raise ValueError("Usage: /routine_delete <taskId>")
    action = await aq.action_store.enqueue(queue_uid, "ROUTINE_DELETE", {"task_id": int(tid)})
    return f"Queued ROUTINE_DELETE id={action.id} task_id={tid}."


async def svc_routine_stats(settings: Settings, user_id: str | None) -> str:
    raw, _ts = await get_admin_store().get_routines_snapshot(user_id)
    routines = _parse_routines_json(raw) if raw else []
    if not routines:
        return "No routine snapshot for stats."
    enabled = sum(1 for r in routines if r.get("enabled", True))
    return f"routines={len(routines)}, enabled≈{enabled} (from last snapshot)\n"


async def svc_routine_report(settings: Settings, queue_uid: int) -> str:
    action = await aq.action_store.enqueue(queue_uid, "ROUTINE_REPORT", {})
    return f"Queued ROUTINE_REPORT id={action.id} — phone will post a local monthly summary as a notification."


async def svc_focus_on(settings: Settings, queue_uid: int) -> str:
    action = await aq.action_store.enqueue(queue_uid, "FOCUS_ON", {})
    return f"Queued FOCUS_ON id={action.id}."


async def svc_focus_off(settings: Settings, queue_uid: int) -> str:
    action = await aq.action_store.enqueue(queue_uid, "FOCUS_OFF", {})
    return f"Queued FOCUS_OFF id={action.id}."


async def svc_sleep_time(settings: Settings, queue_uid: int, rest: str) -> str:
    parts = rest.strip().split()
    if len(parts) < 2:
        raise ValueError("Usage: /sleep_time <sleepHour> <wakeHour>  (0-23)")
    sh = int(parts[0])
    wh = int(parts[1])
    action = await aq.action_store.enqueue(
        queue_uid,
        "SLEEP_TIME",
        {"sleep_start_minutes": sh * 60, "wake_minutes": wh * 60},
    )
    return f"Queued SLEEP_TIME id={action.id}."


async def svc_wake_time(settings: Settings, queue_uid: int, rest: str) -> str:
    wh = int(rest.strip().split()[0]) if rest.strip() else 7
    action = await aq.action_store.enqueue(queue_uid, "WAKE_TIME", {"wake_minutes": wh * 60})
    return f"Queued WAKE_TIME id={action.id}."


async def svc_reward_xp(settings: Settings, queue_uid: int, rest: str) -> str:
    amt = int(rest.strip().split()[0]) if rest.strip() else 0
    if amt <= 0:
        raise ValueError("Usage: /reward_xp <amount>")
    action = await aq.action_store.enqueue(queue_uid, "REWARD_XP", {"amount": amt})
    return f"Queued REWARD_XP id={action.id} amount={amt}."
