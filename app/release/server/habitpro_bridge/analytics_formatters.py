"""ASCII sparklines and light analytics helpers for Telegram text."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any


def ascii_sparkline(values: list[float], width: int = 14) -> str:
    if not values:
        return "—"
    blocks = "▁▂▃▄▅▆▇█"
    lo, hi = min(values), max(values)
    if hi <= lo:
        return blocks[0] * min(width, max(1, len(values)))
    step = max(1, len(values) // width)
    sampled = [sum(values[i : i + step]) / step for i in range(0, len(values), step)][:width]
    out = []
    for v in sampled:
        t = (v - lo) / (hi - lo)
        idx = int(t * (len(blocks) - 1))
        out.append(blocks[idx])
    return "".join(out)


def _parse_bundle(data: dict[str, Any] | None) -> dict[str, Any]:
    return data or {}


def bundle_daily_series(bundle: dict[str, Any], days: int = 14) -> tuple[list[str], list[float]]:
    """Approximate daily completion from completions list in bundle."""
    comp = bundle.get("completions") or []
    habits = bundle.get("habits") or []
    active = sum(1 for h in habits if h.get("is_active", h.get("isActive", True)))
    denom = max(active, 1)
    now = datetime.now(timezone.utc).astimezone()
    days = max(1, min(days, 60))
    keys: list[str] = []
    rates: list[float] = []
    for i in range(days - 1, -1, -1):
        d = (now.date() - timedelta(days=i)).isoformat()
        keys.append(d[-5:])
        day_count = 0
        seen: set[str] = set()
        for c in comp:
            ts = int(c.get("completed_at", c.get("completedAt", 0)) or 0)
            if ts <= 0:
                continue
            dt = datetime.fromtimestamp(ts / 1000, tz=timezone.utc).astimezone().date().isoformat()
            if dt != d:
                continue
            hid = str(c.get("habit_id", c.get("habitId", "")))
            if hid and hid not in seen:
                seen.add(hid)
                day_count += 1
        rates.append(min(1.0, day_count / denom))
    return keys, rates


def mood_summary_from_bundle(bundle: dict[str, Any], days: int = 14) -> str:
    comp = bundle.get("completions") or []
    now = datetime.now(timezone.utc).astimezone()
    moods: list[int] = []
    for c in comp:
        ts = int(c.get("completed_at", c.get("completedAt", 0)) or 0)
        if ts <= 0:
            continue
        age = (now.timestamp() * 1000 - ts) / 86400000
        if age > days:
            continue
        m = int(c.get("mood", 0) or 0)
        if m > 0:
            moods.append(m)
    if not moods:
        return "No mood-tagged check-ins in this window. Log mood with completions to enable this report."
    avg = sum(moods) / len(moods)
    return f"Check-in mood samples: n={len(moods)}, avg={avg:.2f}/5 (reflection only, not medical advice)."


def eye_screen_proxy_from_bundle(bundle: dict[str, Any]) -> str:
    u = bundle.get("usage_screen_summary")
    if u:
        return "Screen-time proxy (UsageStats / foreground totals):\n" + str(u)[:3500]
    return (
        "Screen-time summary not available on the bridge yet.\n"
        "Grant usage access on the phone and sync — this is device screen time, not eye tracking."
    )


def sleep_summary_from_bundle(bundle: dict[str, Any]) -> str:
    bed = bundle.get("sleep_bed_hour")
    wake = bundle.get("sleep_wake_hour")
    if bed is None or wake is None:
        return "Set sleep/wake hours in the app (Studio → Sleep window) to personalize sleep summaries."
    return f"Sleep window (self-reported): bed ≈ {bed}:00, wake ≈ {wake}:00 local (guidance only, not a diagnosis)."
