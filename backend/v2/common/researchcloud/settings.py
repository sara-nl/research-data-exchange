from pydantic import BaseSettings


class ResearchCloudSettings(BaseSettings):
    client_id: str
    client_secret: str
    co_id: str
    co_name: str
    wallet_id: str
    wallet_name: str
    owner_id: str
    webdav_password_co_secret: str
    webdav_user_co_secret: str

    class Config:
        env_prefix = "research_cloud_"


research_cloud_settings = ResearchCloudSettings(
    _env_file=".env", _env_file_encoding="utf-8"
)
