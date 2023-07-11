import uuid

from sqlmodel import Session, select

from common.models.rdx_models import (
    JobStatus,
    RdxAnalyst,
    RdxAnalystDatasetLink,
    RdxDataset,
    RdxJob,
)
from common.owncloud.owncloud_client import OwnCloudClient
from common.researchcloud.researchcloud_client import ResearchCloudClient
from common.settings.app_settings import app_settings

rsc_client = ResearchCloudClient()


def create_new_jobs(session: Session):
    new_jobs = get_new_jobs(session)
    for new_job in new_jobs:
        print(f"Found new job (id={new_job.id})")
        uuid_name = f"rdx-{uuid.uuid1()}"
        if not new_job.results_dir:
            print(f"Creating results dir for new job (id={new_job.id})")
            results_dir_path = f"{app_settings.webdav_results_basedir}/{uuid_name}"
            success = create_results_dir(session, new_job, results_dir_path)
            if not success:
                print(
                    f"Failed to create results dir for new job (id={new_job.id}): retrying next iteration"
                )
                continue

        dataset, analyst = get_dataset_and_analyst(session, new_job)
        if dataset.tinker_license():
            create_credentials(session, new_job, analyst)
            create_upload_url(session, new_job)

        print(f"Creating workspace for new job (id={new_job.id})")
        workspace_id = create_workspace(new_job, dataset, uuid_name)
        if workspace_id is None:
            print(
                f"Failed to create workspace for new job (id={new_job.id}): retrying next iteration"
            )
            continue
        print(f"Created workspace for new job (id={new_job.id})")
        update_job(session, new_job, workspace_id)


def get_new_jobs(session: Session) -> list[RdxJob]:
    statement = select(RdxJob).where(RdxJob.status == JobStatus.new)
    results = session.exec(statement)
    return results


def get_dataset_and_analyst(
    session: Session, job: RdxJob
) -> tuple[RdxDataset, RdxAnalyst]:
    rdx_analyst_dataset_link = session.get(
        RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
    )
    return (rdx_analyst_dataset_link.dataset, rdx_analyst_dataset_link.analyst)


def create_results_dir(session: Session, job: RdxJob, path: str) -> bool:
    with OwnCloudClient() as owncloud_client:
        try:
            result = owncloud_client.create_dir(path)
        except Exception:
            return False

        if result:
            share_id, share_url = owncloud_client.make_public_link(path)

            job.results_dir = path
            job.results_url = share_url
            job.results_share_id = share_id

            session.add(job)
            session.commit()
            session.refresh(job)

    return result


def create_upload_url(session: Session, job: RdxJob):
    with OwnCloudClient() as owncloud_client:
        share_id, upload_url = owncloud_client.make_public_upload_link(
            job.results_dir, job.get_password()
        )
        job.upload_url = upload_url
        job.upload_share_id = share_id
        session.add(job)
        session.commit()
        session.refresh(job)


def create_credentials(session: Session, job: RdxJob, analyst: RdxAnalyst):
    username = analyst.email.replace("@", "").replace(".", "")[:20]
    job.workspace_username = username
    job.set_password()
    session.add(job)
    session.commit()
    session.refresh(job)


def create_workspace(job: RdxJob, dataset: RdxDataset, name: str) -> str:
    researchdrive_path = f"{app_settings.webdav_mount_endpoint}{dataset.rdx_share.path}"
    results_dir = f"{app_settings.webdav_mount_endpoint}{job.results_dir}"
    try:
        if dataset.blind_license():
            workspace_id = rsc_client.create_blind_workspace(
                name, job.script_location, researchdrive_path, results_dir
            )
        if dataset.tinker_license():
            workspace_id = rsc_client.create_tinker_workspace(
                name,
                job.workspace_username,
                job.get_password(),
                researchdrive_path,
                job.upload_url,
            )
    except Exception as error:
        print(f"Failed to create workspace for new job (id={job.id}): {error}")
        return None
    return workspace_id


def update_job(session: Session, job: RdxJob, workspace_id: str):
    job.status = JobStatus.created
    job.workspace_id = workspace_id
    session.add(job)
    session.commit()
