from datetime import datetime

from fastapi import BackgroundTasks, Depends, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from sqlmodel import Session

from common.api.dependencies import get_rdx_user
from common.db.db_client import DBClient
from common.models.rdx_models import (
    RdxDataset,
    RdxDatasetReadWithShare,
    RdxDatasetUpdate,
    RdxUser,
)

from .email import send_publication_email

app = FastAPI()

db = DBClient()

# TODO: use .env to set origins and CORS
origins = [
    "http://localhost",
    "http://localhost:3000",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/dataset/{dataset_id}", response_model=RdxDatasetReadWithShare)
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


@app.patch("/api/dataset/{dataset_id}", response_model=RdxDatasetReadWithShare)
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

    if rdx_user.id != rdx_dataset.researcher_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    new_publication = False
    if not rdx_dataset.published and dataset.published:
        rdx_dataset.published_at = datetime.now()
        new_publication = True

    for key, value in dataset.dict(exclude_unset=True).items():
        setattr(rdx_dataset, key, value)

    session.add(rdx_dataset)
    session.commit()
    session.refresh(rdx_dataset)

    if new_publication:
        background_tasks.add_task(send_publication_email, rdx_user, rdx_dataset)

    return rdx_dataset
