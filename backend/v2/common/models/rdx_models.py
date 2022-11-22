import enum
import json
import secrets
from datetime import datetime, timedelta
from typing import Any, Optional

from pydantic import EmailStr, HttpUrl, PrivateAttr, validator
from sqlalchemy import JSON, Integer, Sequence
from sqlmodel import Column, Enum, Field, Relationship, SQLModel


class RdxAnalystDatasetLink(SQLModel, table=True):
    __tablename__ = "rdx_analyst_dataset"
    id: int | None = Field(
        sa_column=Column("id", Integer, primary_key=True, autoincrement=True)
    )
    dataset_id: Optional[int] = Field(
        default=None, foreign_key="rdx_dataset.id", primary_key=True
    )
    analyst_id: Optional[int] = Field(
        default=None, foreign_key="rdx_analyst.id", primary_key=True
    )
    download_url: HttpUrl | None = None
    download_share_id: int | None = Field(index=True, default=None)
    shared_at: datetime | None = None

    dataset: "RdxDataset" = Relationship(back_populates="analyst_links")
    analyst: "RdxAnalyst" = Relationship(back_populates="dataset_links")


class AccessLicense(str, enum.Enum):
    download = "sign+download"
    analyze = "sign+analyze"


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
    access_license: AccessLicense = Field(sa_column=Column(Enum(AccessLicense)))

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
    analysts: list["RdxAnalyst"] = Relationship(
        back_populates="datasets", link_model=RdxAnalystDatasetLink
    )
    analyst_links: list[RdxAnalystDatasetLink] = Relationship(back_populates="dataset")


class RdxDatasetRead(RdxDatasetBase):
    id: int


class RdxDatasetUpdate(SQLModel):
    doi: str | None = None
    title: str | None = None
    authors: str | None = None
    description: str | None = None
    published: bool | None = None
    published_at: datetime | None = None


class ShareStatus(enum.IntEnum):
    share_discovered = 0
    invalid_permissions = 1
    invalid_permissions_notified = 2
    missing_conditions = 3
    missing_conditions_notified = 4
    missing_dataset_config = 5
    missing_dataset_config_notified = 6
    invalid_dataset_config = 7
    invalid_dataset_config_notified = 8
    dataset_accepted = 9
    dataset_accepted_notified = 10


class RdxShareBase(SQLModel):
    __tablename__ = "rdx_share"

    share_id: int = Field(index=True)
    path: str
    uid_owner: EmailStr | None = None
    additional_info_owner: EmailStr | None = None
    permissions: int
    rdx_dataset_id: int | None = Field(default=None, foreign_key="rdx_dataset.id")
    share_time: datetime
    share_status: ShareStatus = Field(sa_column=Column(Enum(ShareStatus)))
    _new_share_status: ShareStatus | None = PrivateAttr(default=None)

    def __init__(__pydantic_self__, **data: Any) -> None:
        __pydantic_self__.initialize_private_attributes()
        super().__init__(**data)

    def initialize_private_attributes(self):
        # Temporary fix: https://github.com/tiangolo/sqlmodel/pull/472
        self._init_private_attributes()

    @property
    def new_share_status(self) -> ShareStatus | None:
        return self._new_share_status

    def set_new_share_status(self, value: ShareStatus) -> None:
        self._new_share_status: ShareStatus = value

    def set_new_share_status(self, status: ShareStatus, notification: bool = False):
        # Update status if a notification has been sent
        if notification:
            self._new_share_status = status
            return

        # Do not change share_status if a previous regression has been registered
        if self._new_share_status is not None:
            return

        # Do not change the share_status if a notification has already been sent for that status
        if status + 1 == self.share_status:
            self._new_share_status = self.share_status
            return

        # Update status if no new status has been set
        if not self.new_share_status:
            self._new_share_status = status

    def update_share_status(self):
        # If a new share status has been set, update share_status
        if self.new_share_status:
            self.share_status = self.new_share_status
            return
        # If no new share status has been set, there were no misconfigurations found
        # If the dataset has not been accepted already, set status to "share discovered"
        # This allows the dataset to be accepted
        if not self.new_share_status and not self.is_accepted():
            self.share_status = ShareStatus.share_discovered

    def is_eligible(self):
        return self.share_status not in [
            ShareStatus.invalid_permissions,
            ShareStatus.invalid_permissions_notified,
            ShareStatus.missing_conditions,
            ShareStatus.missing_conditions_notified,
            ShareStatus.missing_dataset_config,
            ShareStatus.missing_dataset_config_notified,
            ShareStatus.invalid_dataset_config,
            ShareStatus.invalid_dataset_config_notified,
        ]

    def is_accepted(self):
        return self.share_status in [
            ShareStatus.dataset_accepted,
            ShareStatus.dataset_accepted_notified,
        ]

    def is_finished(self):
        return self.share_status == ShareStatus.dataset_accepted_notified


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
    name: str | None
    datasets: list["RdxDataset"] = Relationship(
        back_populates="analysts",
        link_model=RdxAnalystDatasetLink,
    )
    dataset_links: list[RdxAnalystDatasetLink] = Relationship(back_populates="analyst")


class RdxAnalystUpdate(SQLModel):
    email: EmailStr
    name: str
    agree: bool | None = None
