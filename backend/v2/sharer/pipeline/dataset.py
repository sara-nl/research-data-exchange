from pydantic import BaseModel, EmailStr


class Metadata(BaseModel):
    doi: str
    title: str
    description: str
    authors: str


class DatasetConfig(BaseModel):
    data_steward_email: EmailStr
    researcher_email: EmailStr
    access_license: str
    metadata: Metadata = None
