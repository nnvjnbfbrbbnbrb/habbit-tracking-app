from __future__ import annotations

from typing import Any

import httpx

from habitpro_bridge.config import Settings


def _base(settings: Settings) -> str | None:
    u = (settings.control_server_url or "").strip().rstrip("/")
    return u or None


async def control_get(
    settings: Settings,
    paths: list[str],
    *,
    timeout_s: float = 12.0,
) -> tuple[int, str]:
    base = _base(settings)
    if not base:
        return 0, "Control server URL not set (CONTROL_SERVER_URL)."
    last_status = 0
    last_body = ""
    async with httpx.AsyncClient(timeout=timeout_s) as client:
        for p in paths:
            url = f"{base}{p if p.startswith('/') else '/' + p}"
            try:
                r = await client.get(url)
                last_status, last_body = r.status_code, r.text[:4000]
                if r.status_code < 500:
                    return r.status_code, r.text[:4000]
            except Exception as e:
                last_status = 0
                last_body = str(e)
    return last_status, last_body


async def control_post_json(
    settings: Settings,
    paths: list[str],
    body: dict[str, Any] | None = None,
    *,
    timeout_s: float = 12.0,
) -> tuple[int, str]:
    base = _base(settings)
    if not base:
        return 0, "Control server URL not set (CONTROL_SERVER_URL)."
    last_status = 0
    last_body = ""
    async with httpx.AsyncClient(timeout=timeout_s) as client:
        for p in paths:
            url = f"{base}{p if p.startswith('/') else '/' + p}"
            try:
                r = await client.post(url, json=body if body is not None else {})
                last_status, last_body = r.status_code, r.text[:4000]
                if r.status_code < 500:
                    return r.status_code, r.text[:4000]
            except Exception as e:
                last_status = 0
                last_body = str(e)
    return last_status, last_body


async def control_post_empty(
    settings: Settings,
    paths: list[str],
    *,
    timeout_s: float = 12.0,
) -> tuple[int, str]:
    return await control_post_json(settings, paths, {}, timeout_s=timeout_s)
