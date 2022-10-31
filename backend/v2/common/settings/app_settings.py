from pydantic import AnyHttpUrl, BaseSettings


class AppSettings(BaseSettings):
    web_url: AnyHttpUrl

    class Config:
        env_prefix = "rdx_"


app_settings = AppSettings(_env_file=".env", _env_file_encoding="utf-8")
