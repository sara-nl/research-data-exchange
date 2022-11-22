import datetime
import os
import uuid

from fastapi import HTTPException, status
from sqlmodel import Session, select

from common.models.rdx_models import (
    AccessLicense,
    RdxAnalyst,
    RdxAnalystDatasetLink,
    RdxAnalystUpdate,
    RdxDataset,
    RdxUser,
)
from common.owncloud.owncloud_client import OwnCloudClient

from .email import send_access_email, send_access_notification_email


def get_public_dataset_by_doi(session: Session, doi: str):
    rdx_dataset = session.exec(select(RdxDataset).where(RdxDataset.doi == doi)).first()
    if not rdx_dataset:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Dataset not found"
        )
    if not rdx_dataset.published:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Dataset unavailable"
        )
    return rdx_dataset


def get_analyst(session: Session, analyst_data: RdxAnalystUpdate):
    rdx_analyst = session.exec(
        select(RdxAnalyst).where(RdxAnalyst.email == analyst_data.email)
    ).first()

    if rdx_analyst:
        print(f"Analyst {rdx_analyst.email} already exists")
        if rdx_analyst.has_token_expired():
            print(f"User access token has expired for {rdx_analyst.email}")
            new_token, token_expires_at = RdxAnalyst.create_new_token()
            rdx_analyst.token = new_token
            rdx_analyst.token_expires_at = token_expires_at

    if not rdx_analyst:
        print(f"Creating new analyst {analyst_data.email}")
        token, token_expires_at = RdxAnalyst.create_new_token()
        rdx_analyst = RdxAnalyst(
            email=analyst_data.email,
            name=analyst_data.name,
            token=token,
            token_expires_at=token_expires_at,
        )

    return rdx_analyst


def give_access_to_dataset(
    session: Session, rdx_dataset: RdxDataset, rdx_analyst: RdxAnalyst
):
    rdx_dataset_analyst_link = next(
        link for link in rdx_analyst.dataset_links if link.dataset_id == rdx_dataset.id
    )
    if rdx_dataset.access_license == AccessLicense.analyze:
        print("Processing access license sign+analyze")
    if rdx_dataset.access_license == AccessLicense.download:
        print("Processing access license sign+download")
        if rdx_dataset_analyst_link.download_url == None:
            create_public_link_to_dataset(
                session, rdx_dataset, rdx_dataset_analyst_link
            )

    send_email_to_analyst(rdx_analyst, rdx_dataset, rdx_dataset_analyst_link)
    send_email_to_researcher(session, rdx_analyst, rdx_dataset)


def create_public_link_to_dataset(
    session: Session,
    rdx_dataset: RdxDataset,
    rdx_dataset_analyst_link: RdxAnalystDatasetLink,
) -> None:
    with OwnCloudClient() as oc_client:
        share_id, share_url = oc_client.make_public_link(rdx_dataset.rdx_share.path)

    rdx_dataset_analyst_link.download_share_id = share_id
    rdx_dataset_analyst_link.download_url = share_url
    rdx_dataset_analyst_link.shared_at = datetime.datetime.now()

    session.add(rdx_dataset_analyst_link)
    session.commit()
    session.refresh(rdx_dataset_analyst_link)


def send_email_to_analyst(
    rdx_analyst: RdxAnalyst,
    rdx_dataset: RdxDataset,
    rdx_dataset_analyst_link: RdxAnalystDatasetLink,
):
    tmp_conditions_dir = f"/tmp/{uuid.uuid4()}"
    os.mkdir(tmp_conditions_dir)
    with OwnCloudClient() as oc_client:
        conditions_path = oc_client.download_conditions(
            rdx_dataset.rdx_share.path, tmp_conditions_dir
        )
    send_access_email(
        rdx_analyst, rdx_dataset, rdx_dataset_analyst_link, conditions_path
    )


def send_email_to_researcher(
    session: Session, rdx_analyst: RdxAnalyst, rdx_dataset: RdxDataset
):
    rdx_user = session.get(RdxUser, rdx_dataset.researcher_id)
    send_access_notification_email(rdx_user, rdx_analyst, rdx_dataset)
