import uuid

from sqlmodel import Session, select

from common.models.rdx_models import (
    JobStatus,
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
        dataset = get_dataset(session, new_job)
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


def get_dataset(session: Session, job: RdxJob) -> RdxDataset:
    rdx_analyst_dataset_link = session.get(
        RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
    )
    return rdx_analyst_dataset_link.dataset


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


def create_workspace(job: RdxJob, dataset: RdxDataset, name: str) -> str:
    researchdrive_path = f"{app_settings.webdav_mount_endpoint}{dataset.rdx_share.path}"
    results_dir = f"{app_settings.webdav_mount_endpoint}{job.results_dir}"
    try:
        workspace_id = rsc_client.create_workspace(
            name, job.script_location, researchdrive_path, results_dir
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
