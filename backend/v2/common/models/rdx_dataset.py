import datetime

from pydantic import HttpUrl
from sqlmodel import Field, Relationship, SQLModel

from .rdx_user import RdxUser


class RdxDatasetBase(SQLModel):
    __tablename__ = "rdx_dataset"

    share_id: int = Field(index=True)
    doi: str | None = Field(index=True, default=None)
    title: str | None = None
    authors: str | None = None
    description: str | None = None
    files: list[str]
    conditions_url: HttpUrl
    condtions_share_id: int = Field(index=True)
    published: bool = False
    published_at: datetime.datetime | None = None
    data_steward_id: int | None = Field(
        index=True, default=None, foreign_key="rdx_user.id"
    )
    data_steward: RdxUser | None = Relationship(back_populates="rdx_user")
    researcher_id: int | None = Field(
        index=True, default=None, foreign_key="rdx_user.id"
    )
    rdx_dataset: RdxUser | None = Relationship(back_populates="rdx_user")


class RdxDataset(RdxDatasetBase, table=True):
    id: int | None = Field(default=None, primary_key=True)


class RdxDatasetRead(RdxDatasetBase):
    id: int
