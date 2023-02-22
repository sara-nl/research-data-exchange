import uuid

from sqlmodel import Session, select

from common.models.rdx_models import (
    JobStatus,
    RdxAnalystDatasetLink,
    RdxDataset,
    RdxJob,
)
from common.researchcloud.researchcloud_client import ResearchCloudClient

rsc_client = ResearchCloudClient()


def create_new_jobs(session: Session):
    new_jobs = get_new_jobs(session)
    for new_job in new_jobs:
        print(f"Found new job (id={new_job.id})")
        dataset = get_dataset(session, new_job)
        print(f"Creating workspace for new job (id={new_job.id})")
        workspace_id = create_workspace(new_job, dataset)
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


def create_workspace(job: RdxJob, dataset: RdxDataset) -> str:
    name = f"rdx-{uuid.uuid1()}"
    researchdrive_path = (
        f"https://researchdrive.surfsara.nl/remote.php/webdav{dataset.rdx_share.path}"
    )
    try:
        workspace_id = rsc_client.create_workspace(
            name, job.script_location, researchdrive_path
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
