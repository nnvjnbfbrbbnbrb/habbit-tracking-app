from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Depends, Header, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

from habitpro_bridge import admin_services as ad
from habitpro_bridge.admin_store import get_admin_store
from habitpro_bridge.auth_common import resolve_device_queue_uid, verify_bearer
from habitpro_bridge.config import Settings, get_settings

router = APIRouter(prefix="/v1/admin", tags=["admin"])


def _queue_uid_http(settings: Settings) -> int:
    try:
        return resolve_device_queue_uid(settings)
    except ValueError as e:
        raise HTTPException(503, str(e)) from e


async def require_bearer(
    authorization: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
) -> Settings:
    verify_bearer(authorization, settings)
    return settings


class MaintenanceBody(BaseModel):
    enabled: bool = Field(description="True = maintenance/on, False = normal/off")


class BroadcastBody(BaseModel):
    text: str


class HabitsSnapshotBody(BaseModel):
    user_id: str
    habits: list[dict[str, Any]] = Field(default_factory=list)


class RoutinesSnapshotBody(BaseModel):
    user_id: str
    routines: list[dict[str, Any]] = Field(default_factory=list)


class HabitAddBody(BaseModel):
    name: str
    description: str = ""


class HabitDeleteBody(BaseModel):
    habit_id: str


class NotifyBody(BaseModel):
    text: str


class AnalyticsBundleBody(BaseModel):
    user_id: str
    habits: list[dict[str, Any]] = Field(default_factory=list)
    completions: list[dict[str, Any]] = Field(default_factory=list)
    usage_screen_summary: str | None = None
    sleep_bed_hour: int | None = None
    sleep_wake_hour: int | None = None


class TelegramReportBody(BaseModel):
    text: str


class BackupPostBody(BaseModel):
    user_id: str
    payload_b64: str


class ReminderBody(BaseModel):
    title: str
    detail: str = ""


# --- GROUP 1: App control (proxies CONTROL_SERVER_URL) ---


@router.post("/app/on")
async def admin_app_on(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_app_on(settings)}


@router.post("/app/off")
async def admin_app_off(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_app_off(settings)}


@router.get("/app/status")
async def admin_app_status(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_app_status(settings)}


@router.post("/maintenance")
async def admin_maintenance(
    body: MaintenanceBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_maintenance(settings, body.enabled)}


# --- GROUP 2: Users ---


@router.get("/users")
async def admin_users(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_users_list(settings)}


@router.get("/users/{user_id}")
async def admin_user_info(
    user_id: str,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_user_info(settings, user_id)}


@router.post("/users/{user_id}/ban")
async def admin_user_ban(
    user_id: str,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_user_ban(settings, user_id)}


@router.post("/users/{user_id}/unban")
async def admin_user_unban(
    user_id: str,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_user_unban(settings, user_id)}


@router.post("/broadcast")
async def admin_broadcast(
    body: BroadcastBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_broadcast(settings, body.text)}


# --- GROUP 3: Habits (bridge snapshot + queue) ---


@router.get("/habits")
async def admin_habits(
    settings: Settings = Depends(require_bearer),
    user_id: str | None = None,
) -> dict[str, str]:
    return {"result": await ad.svc_habits_list(settings, user_id)}


@router.get("/habits/stats")
async def admin_habit_stats(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_habit_stats(settings)}


@router.get("/habits/week")
async def admin_habit_week(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_habit_week(settings)}


@router.get("/habits/top")
async def admin_habit_top(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_habit_top(settings)}


@router.get("/habits/streak")
async def admin_streak(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_streak(settings)}


@router.post("/habits/snapshot")
async def admin_habits_snapshot(
    body: HabitsSnapshotBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_set_habits_snapshot(settings, body.user_id, body.habits)}


@router.post("/routines/snapshot")
async def admin_routines_snapshot(
    body: RoutinesSnapshotBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_set_routines_snapshot(settings, body.user_id, body.routines)}


@router.post("/queue/habit-add")
async def admin_queue_habit_add(
    body: HabitAddBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    q = _queue_uid_http(settings)
    rest = body.name if not body.description else f"{body.name} | {body.description}"
    return {"result": await ad.svc_habit_add(settings, q, rest)}


@router.post("/queue/habit-delete")
async def admin_queue_habit_delete(
    body: HabitDeleteBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    q = _queue_uid_http(settings)
    return {"result": await ad.svc_habit_delete(settings, q, body.habit_id)}


@router.post("/analytics/bundle")
async def admin_analytics_bundle(
    body: AnalyticsBundleBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {
        "result": await ad.svc_post_analytics_bundle(
            settings,
            body.user_id,
            body.habits,
            body.completions,
            body.usage_screen_summary,
            body.sleep_bed_hour,
            body.sleep_wake_hour,
        )
    }


@router.post("/telegram-report")
async def admin_telegram_report(
    body: TelegramReportBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, object]:
    q = _queue_uid_http(settings)
    return {"result": await ad.svc_telegram_report(settings, q, body.text)}


@router.post("/backup")
async def admin_backup_post(
    body: BackupPostBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_backup_post(settings, body.user_id, body.payload_b64)}


@router.get("/backup/latest")
async def admin_backup_latest(
    user_id: str,
    settings: Settings = Depends(require_bearer),
) -> dict[str, object]:
    row = await get_admin_store().latest_backup_row(user_id)
    if row is None:
        raise HTTPException(404, "no backup")
    return {"user_id": user_id, "payload_b64": row[0], "created_ts": row[1]}


@router.get("/backup/latest.pdf")
async def admin_backup_latest_pdf(
    user_id: str,
    settings: Settings = Depends(require_bearer),
) -> Response:
    try:
        pdf = await ad.svc_backup_pdf(settings, user_id)
    except ValueError as e:
        raise HTTPException(400, str(e)) from e
    return Response(content=pdf, media_type="application/pdf")


# --- GROUP 4: Notifications / reminders ---


@router.post("/notify")
async def admin_notify(
    body: NotifyBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    q = _queue_uid_http(settings)
    return {"result": await ad.svc_notify(settings, q, body.text)}


@router.post("/reminders")
async def admin_reminder_create(
    body: ReminderBody,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    rest = body.title if not body.detail else f"{body.title} | {body.detail}"
    return {"result": await ad.svc_reminder_set(settings, rest)}


@router.get("/reminders")
async def admin_reminders_list(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_reminder_list(settings)}


@router.post("/reminders/{reminder_id}/off")
async def admin_reminder_off(
    reminder_id: str,
    settings: Settings = Depends(require_bearer),
) -> dict[str, str]:
    return {"result": await ad.svc_reminder_off(settings, reminder_id)}


@router.get("/schedule")
async def admin_schedule(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_schedule(settings)}


# --- GROUP 5: Logs / stats ---


@router.get("/logs")
async def admin_logs(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_logs(settings)}


@router.post("/logs/clear")
async def admin_logs_clear(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    return {"result": await ad.svc_logs_clear(settings)}


@router.get("/server/stats")
async def admin_server_stats(settings: Settings = Depends(require_bearer)) -> dict[str, str]:
    try:
        return {"result": await ad.svc_server_stats(settings)}
    except ValueError as e:
        raise HTTPException(503, str(e)) from e
