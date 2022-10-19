import secrets
from datetime import datetime, timedelta

from pydantic import EmailStr
from sqlmodel import Field, SQLModel


class RdxUserBase(SQLModel):

    id: int | None = Field(default=None, primary_key=True)
    email: EmailStr = Field(index=True)
    token: str = Field(index=True)
    token_expires_at: datetime

    @classmethod
    def create_new_token(cls) -> tuple[str, datetime]:
        token = secrets.token_urlsafe()
        # TODO: set token expiration in config/settings
        token_expires_at = datetime.now() + timedelta(hours=48)
        return (token, token_expires_at)

    def has_token_expired(self) -> bool:
        return self.token_expires_at < datetime.now()


class RdxUser(RdxUserBase, table=True):
    __tablename__ = "rdx_user"


class RdxAnalyst(RdxUserBase, table=True):
    __tablename__ = "rdx_analyst"
