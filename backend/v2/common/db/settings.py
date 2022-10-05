from pydantic import BaseSettings


class DBSettings(BaseSettings):
    host: str
    port: int = 5432
    name: str = "postgres"
    user: str
    password: str

    class Config:
        env_prefix = "db_"


db_settings = DBSettings(_env_file=".env", _env_file_encoding="utf-8")
