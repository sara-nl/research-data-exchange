from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import HttpUrl
from sqlmodel import Session

from common.api.dependencies import get_rdx_analyst
from common.db.db_client import DBClient
from common.models.rdx_models import (
    AccessLicense,
    JobStatus,
    RdxAnalyst,
    RdxAnalystDatasetLink,
    RdxJob,
)

db = DBClient()

router = APIRouter()


@router.post("/api/lab/{rdx_analyst_dataset_link_id:path}/job", status_code=201)
def submit_analysis_job(
    *,
    session: Session = Depends(db.get_session_dependency),
    rdx_analyst: RdxAnalyst = Depends(get_rdx_analyst),
    rdx_analyst_dataset_link_id: int,
    script_location: HttpUrl
):
    rdx_analyst_dataset_link = session.get(
        RdxAnalystDatasetLink, rdx_analyst_dataset_link_id
    )
    if not rdx_analyst_dataset_link:
        raise HTTPException(status=404, detail="Dataset not found")

    if rdx_analyst.id != rdx_analyst_dataset_link.analyst_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    if not rdx_analyst_dataset_link.dataset.published:
        raise HTTPException(status=404, detail="Dataset not found")

    if rdx_analyst_dataset_link.dataset.access_license in [AccessLicense.download]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden resource"
        )

    new_job = RdxJob(
        rdx_analyst_dataset_link_id=rdx_analyst_dataset_link.id,
        status=JobStatus.new,
        script_location=script_location,
    )
    session.add(new_job)
    session.commit()
