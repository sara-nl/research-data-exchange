from typing import ClassVar

import fairly
from pydantic import BaseModel, HttpUrl


class DataPortalMetadata(BaseModel):
    doi: str | None = None
    title: str | None = None
    authors: str | None = None
    description: str | None = None


class MetaDataClient(BaseModel):

    DOMAIN_CLIENT_MAPPINGS: ClassVar = {"figshare.com": "figshare"}

    @classmethod
    def get_client(cls, url: HttpUrl) -> fairly.Client | None:
        client_id = ""

        for domain, client in cls.DOMAIN_CLIENT_MAPPINGS.items():
            if domain in url:
                client_id = client
                break

        if client_id:
            return fairly.client(id=client_id)

        return None

    @classmethod
    def get_metadata(cls, url: HttpUrl) -> DataPortalMetadata | Exception:
        client = cls.get_client(url)
        if not client:
            raise ValueError(f"Could not find client for url {url}")
        try:
            dataset = client.get_dataset(url)
            metadata = dataset.metadata
            return DataPortalMetadata(
                doi=metadata["doi"],
                title=metadata["title"],
                description=metadata["description"],
                authors=", ".join([author.fullname for author in metadata["authors"]]),
            )
        except Exception as error:
            raise error
