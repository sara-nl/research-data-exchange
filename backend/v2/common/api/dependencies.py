from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlmodel import Session, select

from common.db.db_client import DBClient
from common.models.rdx_models import RdxAnalyst, RdxAnalystDatasetLink, RdxUser

db = DBClient()

oauth2_scheme = HTTPBearer()


def get_rdx_user(
    token: HTTPAuthorizationCredentials = Depends(oauth2_scheme),
    session: Session = Depends(db.get_session_dependency),
) -> RdxUser:
    rdx_user = session.exec(
        select(RdxUser).where(RdxUser.token == token.credentials)
    ).first()

    if not rdx_user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if rdx_user.has_token_expired():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return rdx_user


def get_rdx_analyst(
    token: HTTPAuthorizationCredentials = Depends(oauth2_scheme),
    session: Session = Depends(db.get_session_dependency),
) -> RdxAnalyst:
    rdx_analyst = session.exec(
        select(RdxAnalyst).where(RdxAnalyst.token == token.credentials)
    ).first()

    if not rdx_analyst:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if rdx_analyst.has_token_expired():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return rdx_analyst


def get_rdx_analyst_dataset_link(
    rdx_analyst_dataset_link_id: int,
    session: Session = Depends(db.get_session_dependency),
) -> RdxAnalystDatasetLink:
    rdx_analyst_dataset_link = session.get(
        RdxAnalystDatasetLink, rdx_analyst_dataset_link_id
    )
    if not rdx_analyst_dataset_link:
        raise HTTPException(status=404, detail="Dataset not found")

    if not rdx_analyst_dataset_link.dataset.published:
        raise HTTPException(status=404, detail="Dataset not found")

    return rdx_analyst_dataset_link
