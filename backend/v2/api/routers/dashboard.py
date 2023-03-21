from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlmodel import Session, select

from common.api.dependencies import get_rdx_user
from common.db.db_client import DBClient
from common.models.rdx_models import RdxDataset, RdxSigninRequest, RdxUser

from ..dashboard.email import send_dashboard_signin_email

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
