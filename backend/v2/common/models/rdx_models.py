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
        sa_column=Column(
            "id", Integer, primary_key=True, autoincrement=True, unique=True
        )
    )
    dataset_id: Optional[int] = Field(
        default=None, foreign_key="rdx_dataset.id", nullable=False
    )
    analyst_id: Optional[int] = Field(
        default=None, foreign_key="rdx_analyst.id", nullable=False
    )
    download_url: HttpUrl | None = None
    download_share_id: int | None = Field(index=True, default=None)
    shared_at: datetime | None = None

    dataset: "RdxDataset" = Relationship(sa_relationship_kwargs={"viewonly": True})
    analyst: "RdxAnalyst" = Relationship(sa_relationship_kwargs={"viewonly": True})
    jobs: list["RdxJob"] = Relationship(sa_relationship_kwargs={"viewonly": True})


class AccessLicense(enum.IntEnum):
    download = 1
    analyze_blind_with_output_check = 2
    analyze_blind_no_output_check = 3

    @classmethod
    def print_friendly_access_license(cls, value: int) -> str:
        if cls.download == value:
            return "sign+download"
        if cls.analyze_blind_with_output_check == value:
            return "sign+analyze (blind, with output check)"
        if cls.analyze_blind_no_output_check == value:
            return "sign+analyze (blind, without output check)"


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
    access_license_id: int | None

    @property
    def access_license(self) -> AccessLicense:
        return AccessLicense(self.access_license_id)

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
    analyst_links: list[RdxAnalystDatasetLink] = Relationship(
        sa_relationship_kwargs={"viewonly": True}
    )


class RdxDatasetRead(RdxDatasetBase):
    id: int


class RdxDatasetUpdate(SQLModel):
    doi: str
    title: str
    authors: str
    description: str
    published: bool | None = None
    published_at: datetime | None = None
    access_license_id: int
    researcher_email: EmailStr


class ShareStatus(enum.IntEnum):
    share_discovered = 0
    invalid_permissions = 1
    invalid_permissions_notified = 2
    missing_conditions = 3
    missing_conditions_notified = 4
    missing_dataset_config = 5  # deprecated
    missing_dataset_config_notified = 6  # deprecated
    invalid_dataset_config = 7  # deprecated
    invalid_dataset_config_notified = 8  # deprecated
    dataset_accepted = 9  # deprecated
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


class PublicRdxDatasetRead(SQLModel):
    doi: str
    title: str
    authors: str
    description: str
    files: list[str]
    conditions_url: HttpUrl
    access_license_id: int


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
    dataset_links: list[RdxAnalystDatasetLink] = Relationship(
        sa_relationship_kwargs={"viewonly": True}
    )


class RdxAnalystUpdate(SQLModel):
    email: EmailStr
    name: str
    agree: bool | None = None


class JobStatus(str, enum.Enum):
    new = "new"
    created = "created"
    running = "running"
    failed = "failed"
    failed_notified = "failed_notified"
    failed_notified_deleted = "failed_notified_deleted"
    finished = "finished"
    finished_notified = "finished_notified"
    finished_notified_deleted = "finished_notified_deleted"


class RdxJob(SQLModel, table=True):
    __tablename__ = "rdx_job"
    id: int | None = Field(
        sa_column=Column("id", Integer, primary_key=True, autoincrement=True)
    )
    rdx_analyst_dataset_link_id: int | None = Field(
        default=None, foreign_key="rdx_analyst_dataset.id"
    )
    status: str = Field(index=True)
    script_location: HttpUrl
    workspace_id: str | None
    workspace_status: str | None
    results_dir: str | None
    results_url: HttpUrl | None = None
    results_share_id: int | None = Field(index=True, default=None)

    def get_status(self) -> JobStatus:
        return JobStatus(self.status)


class RdxJobSubmission(SQLModel):
    script_location: HttpUrl


class RdxSigninRequest(SQLModel):
    role: str
    email: EmailStr


class RdxJobRead(SQLModel):
    id: int
    script_location: HttpUrl
    status: str


class RdxJobForResearcher(SQLModel):
    id: int
    script_location: HttpUrl
    status: str
    results_url: HttpUrl | None
    analyst_name: str
    analyst_email: EmailStr


class DatasetsPerLicense(SQLModel):
    access_license_id: int
    total: int


class DatasetStat(SQLModel):
    id: int
    doi: str
    title: str
    access_license_id: int
    signed: int = 0
    analyzed: int = 0
