from sqlmodel import Session, or_, select

from common.models.rdx_models import JobStatus, RdxAnalystDatasetLink, RdxJob
from common.researchcloud.researchcloud_client import ResearchCloudClient

rsc_client = ResearchCloudClient()


def update_active_jobs(session: Session):
    active_jobs = get_active_jobs(session)
    print(f"Found {len(active_jobs)} active jobs")
    for job in active_jobs:
        workspace_status = get_workspace_status(job)
        if workspace_status is None:
            print(
                f"Failed to get status of workspace for job (id={job.id}): retrying next iteration"
            )
            continue
        update_job_status(session, job, workspace_status)
    return


def get_active_jobs(session: Session) -> list[RdxJob]:
    statement = select(RdxJob).where(
        or_(
            RdxJob.status == JobStatus.creating,
            RdxJob.status == JobStatus.created,
            RdxJob.status == JobStatus.running,
            RdxJob.status == JobStatus.running_notified,
        )
    )
    active_jobs = session.exec(statement)
    return list(active_jobs)


def get_workspace_status(job: RdxJob):
    try:
        status = rsc_client.get_workspace_status(job.workspace_id)
    except Exception as error:
        print(
            f"Failed to get status of workspace for new job (id={job.id}, workspace_id={job.workspace_id}): {error}"
        )
        return None
    return status


def update_job_status(session: Session, job: RdxJob, workspace_status: str):
    # Only update job status based on workspace status if job was previously running
    if job.status not in [
        JobStatus.creating,
        JobStatus.created,
        JobStatus.running,
        JobStatus.running_notified,
    ]:
        return

    dataset = session.get(
        RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
    ).dataset

    job.workspace_status = workspace_status

    if dataset.blind_license() and workspace_status == "creating":
        job.status = JobStatus.running
    if dataset.tinker_license() and workspace_status == "creating":
        job.status = JobStatus.creating
    if dataset.blind_license() and workspace_status == "running":
        job.status = JobStatus.finished
    if (
        dataset.tinker_license()
        and not (job.status == JobStatus.running_notified)
        and workspace_status == "running"
    ):
        job.workspace_ip = rsc_client.get_workspace_ip(job.workspace_id)
        job.status = JobStatus.running
    if dataset.tinker_license() and workspace_status == "deleted":
        job.status = JobStatus.finished
    if workspace_status == "failed":
        job.status = JobStatus.failed

    session.add(job)
    session.commit()
