from __future__ import annotations

from functools import lru_cache

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    telegram_bot_token: str = ""
    telegram_webhook_secret: str = ""
    allowed_telegram_user_ids: str = ""
    api_secret: str = ""
    control_server_url: str = ""
    admin_db_path: str = "data/admin.db"
    # Which allowlisted user's queue the Android app polls (Bearer API_SECRET).
    # If unset and exactly one id is allowlisted, that id is used.
    device_queue_telegram_user_id: int | None = None
    enable_multi_tenant_analytics: bool = False
    host: str = "0.0.0.0"
    port: int = 8000
    use_polling: bool = False
    queue_db_path: str = "data/actions.db"

    @property
    def allowed_ids_set(self) -> set[int]:
        raw = (self.allowed_telegram_user_ids or "").strip()
        if not raw:
            return set()
        out: set[int] = set()
        for part in raw.split(","):
            part = part.strip()
            if not part:
                continue
            try:
                out.add(int(part))
            except ValueError:
                continue
        return out

    @field_validator("device_queue_telegram_user_id", mode="before")
    @classmethod
    def _empty_queue_user_to_none(cls, v: object) -> object:
        if v is None or v == "":
            return None
        return v


@lru_cache
def get_settings() -> Settings:
    return Settings()
