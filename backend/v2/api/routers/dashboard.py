from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlmodel import Session, func, select

from common.api.dependencies import get_rdx_user
from common.db.db_client import DBClient
from common.models.rdx_models import (
    DatasetsPerLicense,
    RdxDataset,
    RdxSigninRequest,
    RdxUser,
)

from ..dashboard.email import send_dashboard_signin_email

# from sqlalchemy import func


db = DBClient()

router = APIRouter()


@router.post("/api/dashboard/signin", status_code=201)
def signin_to_dashboard(
    *,
    session: Session = Depends(db.get_session_dependency),
    background_tasks: BackgroundTasks,
    signin_request: RdxSigninRequest,
):
    if signin_request.role not in ["researcher", "data_steward"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported role"
        )

    rdx_user = session.exec(
        select(RdxUser).where(RdxUser.email == signin_request.email)
    ).first()

    if not rdx_user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="User not found"
        )

    rdx_user.create_new_token()
    session.add(rdx_user)
    session.commit()

    background_tasks.add_task(
        send_dashboard_signin_email, rdx_user, signin_request.role
    )


@router.get(
    "/api/dashboard/data_steward/datasets", response_model=list[DatasetsPerLicense]
)
def get_analysis_jobs(
    *,
    session: Session = Depends(db.get_session_dependency),
    rdx_user: RdxUser = Depends(get_rdx_user),
):
    datasets_per_license = session.exec(
        select(RdxDataset.access_license_id, func.count(RdxDataset.id).label("total"))
        .where(RdxDataset.data_steward_id == rdx_user.id)
        .where(RdxDataset.access_license_id != None)
        .group_by(RdxDataset.access_license_id)
        .order_by(RdxDataset.access_license_id)
    ).all()
    return datasets_per_license
