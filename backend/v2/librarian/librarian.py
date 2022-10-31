from datetime import datetime

from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlmodel import Session

from common.db.db_client import DBClient
from common.models.rdx_models import (
    RdxDataset,
    RdxDatasetReadWithShare,
    RdxDatasetUpdate,
)

app = FastAPI()

db = DBClient()

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


# TODO add token middleware
@app.get("/api/dataset/{dataset_id}", response_model=RdxDatasetReadWithShare)
def get_dataset(
    *, session: Session = Depends(db.get_session_dependency), dataset_id: int
):
    rdx_dataset = session.get(RdxDataset, dataset_id)
    if not rdx_dataset:
        raise HTTPException(status_code=404, detail="Dataset not found")
    return rdx_dataset


# TODO add token middleware
@app.patch("/api/dataset/{dataset_id}", response_model=RdxDatasetReadWithShare)
def get_dataset(
    *,
    session: Session = Depends(db.get_session_dependency),
    dataset_id: int,
    dataset: RdxDatasetUpdate
):
    rdx_dataset = session.get(RdxDataset, dataset_id)
    if not rdx_dataset:
        raise HTTPException(status=404, detail="Dataset not found")
    if not rdx_dataset.published and dataset.published:
        rdx_dataset.published_at = datetime.now()
    for key, value in dataset.dict(exclude_unset=True).items():
        setattr(rdx_dataset, key, value)
    session.add(rdx_dataset)
    session.commit()
    session.refresh(rdx_dataset)
    return rdx_dataset
