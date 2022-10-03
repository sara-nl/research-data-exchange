from pydantic import BaseSettings


class EmailSettings(BaseSettings):
    host: str
    port: int
    user: str
    password: str
    sender: str

    class Config:
        env_prefix = "email_"


email_settings = EmailSettings(_env_file=".env", _env_file_encoding="utf-8")
