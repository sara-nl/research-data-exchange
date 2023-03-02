from pydantic import AnyHttpUrl, BaseSettings


class AppSettings(BaseSettings):
    web_url: AnyHttpUrl
    webdav_mount_endpoint: AnyHttpUrl = (
        "https://researchdrive.surfsara.nl/remote.php/webdav"
    )
    webdav_results_basedir: str

    class Config:
        env_prefix = "rdx_"


app_settings = AppSettings(_env_file=".env", _env_file_encoding="utf-8")
