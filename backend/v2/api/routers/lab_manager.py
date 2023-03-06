from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import HttpUrl
from sqlmodel import Session, select

from common.api.dependencies import get_rdx_analyst, get_rdx_analyst_dataset_link
from common.db.db_client import DBClient
from common.models.rdx_models import (
    AccessLicense,
    JobStatus,
    RdxAnalyst,
    RdxAnalystDatasetLink,
    RdxDatasetReadWithShare,
    RdxJob,
    RdxJobRead,
    RdxJobSubmission,
)

db = DBClient()

router = APIRouter()


@router.get(
    "/api/lab/{rdx_analyst_dataset_link_id}", response_model=RdxDatasetReadWithShare
)
def get_dataset_for_lab(
    *,
    rdx_analyst: RdxAnalyst = Depends(get_rdx_analyst),
    rdx_analyst_dataset_link: RdxAnalystDatasetLink = Depends(
        get_rdx_analyst_dataset_link
    ),
):
    if rdx_analyst.id != rdx_analyst_dataset_link.analyst_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    if rdx_analyst_dataset_link.dataset.access_license in [AccessLicense.download]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    return rdx_analyst_dataset_link.dataset


@router.post("/api/lab/{rdx_analyst_dataset_link_id}/job", status_code=201)
def submit_analysis_job(
    *,
    session: Session = Depends(db.get_session_dependency),
    rdx_analyst: RdxAnalyst = Depends(get_rdx_analyst),
    rdx_analyst_dataset_link: RdxAnalystDatasetLink = Depends(
        get_rdx_analyst_dataset_link
    ),
    job_submission: RdxJobSubmission,
):
    if rdx_analyst.id != rdx_analyst_dataset_link.analyst_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    if rdx_analyst_dataset_link.dataset.access_license in [AccessLicense.download]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    new_job = RdxJob(
        rdx_analyst_dataset_link_id=rdx_analyst_dataset_link.id,
        status=JobStatus.new,
        script_location=job_submission.script_location,
    )
    session.add(new_job)
    session.commit()


@router.get(
    "/api/lab/{rdx_analyst_dataset_link_id}/job", response_model=list[RdxJobRead]
)
def submit_analysis_job(
    *,
    session: Session = Depends(db.get_session_dependency),
    rdx_analyst: RdxAnalyst = Depends(get_rdx_analyst),
    rdx_analyst_dataset_link: RdxAnalystDatasetLink = Depends(
        get_rdx_analyst_dataset_link
    ),
):
    if rdx_analyst.id != rdx_analyst_dataset_link.analyst_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    if rdx_analyst_dataset_link.dataset.access_license in [AccessLicense.download]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )
    statement = select(RdxJob).where(
        RdxJob.rdx_analyst_dataset_link_id == rdx_analyst_dataset_link.id
    )
    results = session.exec(statement).all()
    return results
