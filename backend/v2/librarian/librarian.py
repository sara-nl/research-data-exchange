from datetime import datetime

from fastapi import Depends, FastAPI, HTTPException, status
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
    rdx_user: RdxUser = Depends(get_rdx_user),
    dataset_id: int,
    dataset: RdxDatasetUpdate,
):
    rdx_dataset = session.get(RdxDataset, dataset_id)
    if not rdx_dataset:
        raise HTTPException(status=404, detail="Dataset not found")

    # TODO: decide whether this action should be limited to researcher/data steward
    if rdx_user.id not in [rdx_dataset.researcher_id, rdx_dataset.data_steward_id]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    if not rdx_dataset.published and dataset.published:
        rdx_dataset.published_at = datetime.now()

    for key, value in dataset.dict(exclude_unset=True).items():
        setattr(rdx_dataset, key, value)

    session.add(rdx_dataset)
    session.commit()
    session.refresh(rdx_dataset)

    return rdx_dataset
