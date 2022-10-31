import json
import secrets
from datetime import datetime, timedelta
from typing import Optional

from pydantic import EmailStr, HttpUrl, validator
from sqlalchemy import JSON
from sqlmodel import Column, Field, Relationship, SQLModel


class RdxDatasetBase(SQLModel):
    __tablename__ = "rdx_dataset"

    share_id: int = Field(index=True)
    doi: str | None = Field(index=True, default=None)
    title: str | None = None
    authors: str | None = None
    description: str | None = None
    files: list[str] = Field(sa_column=Column(JSON))
    conditions_url: HttpUrl
    condtions_share_id: int = Field(index=True)
    published: bool = False
    published_at: datetime | None = None
    data_steward_id: int | None = Field(
        index=True, default=None, foreign_key="rdx_user.id"
    )
    researcher_id: int | None = Field(
        index=True, default=None, foreign_key="rdx_user.id"
    )

    @validator("files", pre=True)
    def name_must_contain_space(cls, v):
        if isinstance(v, str):
            v = json.loads(v)
        if not isinstance(v, list):
            raise ValueError("must be list")
        return v


class RdxDataset(RdxDatasetBase, table=True):
    id: int | None = Field(default=None, primary_key=True)
    rdx_share: Optional["RdxShare"] = Relationship(
        back_populates="rdx_dataset", sa_relationship_kwargs={"uselist": False}
    )


class RdxDatasetRead(RdxDatasetBase):
    id: int


class RdxDatasetUpdate(SQLModel):
    doi: str | None = None
    title: str | None = None
    authors: str | None = None
    description: str | None = None
    published: bool | None = None
    published_at: datetime | None = None


class RdxShareBase(SQLModel):
    __tablename__ = "rdx_share"

    share_id: int = Field(index=True)
    path: str
    uid_owner: EmailStr | None = None
    additional_info_owner: EmailStr | None = None
    permissions: int
    rdx_dataset_id: int | None = Field(default=None, foreign_key="rdx_dataset.id")
    share_time: datetime


class RdxShare(RdxShareBase, table=True):
    id: int | None = Field(default=None, primary_key=True)
    rdx_dataset: RdxDataset | None = Relationship(back_populates="rdx_share")


class RdxShareRead(RdxShareBase):
    id: int


class RdxDatasetReadWithShare(RdxDatasetRead):
    rdx_share: RdxShareRead | None = None


class RdxShareReadWithDataset(RdxShareRead):
    rdx_dataset: RdxDatasetRead | None = None


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
