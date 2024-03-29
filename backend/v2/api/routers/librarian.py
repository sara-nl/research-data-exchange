from datetime import datetime

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from pydantic import HttpUrl
from sqlmodel import Session

from common.api.dependencies import get_rdx_user
from common.db.db_client import DBClient
from common.metadata.metadata_client import DataPortalMetadata, MetaDataClient
from common.models.rdx_models import (
    PublicRdxDatasetRead,
    RdxAnalystUpdate,
    RdxDataset,
    RdxDatasetReadWithShare,
    RdxDatasetUpdate,
    RdxUser,
)
from common.models.utils import create_rdx_user

from ..librarian.access import (
    get_analyst,
    get_public_dataset_by_doi,
    give_access_to_dataset,
)
from ..librarian.email import send_publication_email

db = DBClient()

router = APIRouter()


@router.get("/api/dataset/{dataset_id}", response_model=RdxDatasetReadWithShare)
def get_dataset(
    *,
    session: Session = Depends(db.get_session_dependency),
    dataset_id: int,
    rdx_user: RdxUser = Depends(get_rdx_user),
):
    rdx_dataset = session.get(RdxDataset, dataset_id)
    if not rdx_dataset:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Dataset not found"
        )

    if rdx_user.id not in [rdx_dataset.researcher_id, rdx_dataset.data_steward_id]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    return rdx_dataset


@router.patch("/api/dataset/{dataset_id}", response_model=RdxDatasetReadWithShare)
def publish_dataset(
    *,
    session: Session = Depends(db.get_session_dependency),
    background_tasks: BackgroundTasks,
    rdx_user: RdxUser = Depends(get_rdx_user),
    dataset_id: int,
    dataset: RdxDatasetUpdate,
):
    rdx_dataset = session.get(RdxDataset, dataset_id)
    if not rdx_dataset:
        raise HTTPException(status=404, detail="Dataset not found")

    if rdx_user.id != rdx_dataset.data_steward_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    new_publication = False
    if not rdx_dataset.published and dataset.published:
        rdx_dataset.published_at = datetime.now()
        new_publication = True

    research_email = dataset.researcher_email
    del dataset.researcher_email
    researcher = create_rdx_user(db, research_email)
    rdx_dataset.researcher_id = researcher.id

    for key, value in dataset.dict(exclude_unset=True).items():
        setattr(rdx_dataset, key, value)

    session.add(rdx_dataset)
    session.commit()
    session.refresh(rdx_dataset)

    if new_publication:
        background_tasks.add_task(send_publication_email, researcher, rdx_dataset)

    return rdx_dataset


@router.get(
    "/api/dataset/{dataset_doi:path}/access", response_model=PublicRdxDatasetRead
)
def get_public_dataset(
    *,
    session: Session = Depends(db.get_session_dependency),
    dataset_doi: str,
):
    return get_public_dataset_by_doi(session, dataset_doi)


@router.post("/api/dataset/{dataset_doi:path}/access", status_code=201)
def request_access_to_dataset(
    *,
    session: Session = Depends(db.get_session_dependency),
    background_tasks: BackgroundTasks,
    dataset_doi: str,
    analyst_data: RdxAnalystUpdate,
):
    if not analyst_data.agree:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Terms and conditions need to be agreed to",
        )

    rdx_dataset = get_public_dataset_by_doi(session, dataset_doi)
    rdx_analyst = get_analyst(session, analyst_data)

    print(f"Linking {rdx_analyst.email} to dataset {rdx_dataset.id}")
    rdx_analyst.datasets.append(rdx_dataset)
    session.add(rdx_analyst)
    session.commit()
    session.refresh(rdx_analyst)
    session.refresh(rdx_dataset)

    background_tasks.add_task(give_access_to_dataset, session, rdx_dataset, rdx_analyst)


@router.get("/api/metadata", response_model=DataPortalMetadata)
def get_public_dataset(
    *,
    _: RdxUser = Depends(get_rdx_user),
    url: HttpUrl,
):
    try:
        print("URL", url)
        return MetaDataClient.get_metadata(url)
    except Exception as error:
        print(f"Error getting metadata for url {url}: {error}")
        raise HTTPException(
            status_code=404, detail=f"Could not retrieve metadata for {url}"
        )
