from pydantic import BaseSettings


class OwnCloudSettings(BaseSettings):
    rdx_webdav_user: str
    rdx_webdav_password: str
    shares_source: str
    max_folder_depth: int = 50
    webdav_server_uri: str
    webdav_server_suffix: str
    minimum_permission_level: int = 16


own_cloud_settings = OwnCloudSettings(_env_file=".env", _env_file_encoding="utf-8")
