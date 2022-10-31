import enum
from pydantic import BaseModel, EmailStr

from common.models.rdx_models import AccessLicense


class Metadata(BaseModel):
    doi: str
    title: str
    description: str
    authors: str


class DatasetConfig(BaseModel):
    data_steward_email: EmailStr
    researcher_email: EmailStr
    access_license: AccessLicense
    metadata: Metadata = None
