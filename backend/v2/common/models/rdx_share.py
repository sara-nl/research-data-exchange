from pydantic import EmailStr
from sqlmodel import Field, Relationship, SQLModel

from .rdx_dataset import RdxDataset


class RdxShareBase(SQLModel):
    __tablename__ = "rdx_share"

    share_id: int = Field(index=True)
    path: str
    uid_owner: EmailStr | None = None
    additional_info_owner: EmailStr | None = None
    permissions: int
    rdx_dataset_id: int | None = Field(default=None, foreign_key="rdx_dataset.id")
    rdx_dataset: RdxDataset | None = Relationship(back_populates="rdx_dataset")


class RdxShare(RdxShareBase, table=True):
    id: int | None = Field(default=None, primary_key=True)


class RdxShareRead(RdxShareBase):
    id: int
