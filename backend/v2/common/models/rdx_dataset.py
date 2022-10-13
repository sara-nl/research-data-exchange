import datetime
from importlib.metadata import metadata

from pydantic import HttpUrl
from sqlmodel import Field, Relationship, SQLModel


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


class RdxDataset(RdxDatasetBase, table=True):
    id: int | None = Field(default=None, primary_key=True)


class RdxDatasetRead(RdxDatasetBase):
    id: int
