from sqlmodel import Session, or_, select

from common.models.rdx_models import JobStatus, RdxAnalystDatasetLink, RdxJob
from common.researchcloud.researchcloud_client import ResearchCloudClient

rsc_client = ResearchCloudClient()


def cleanup_jobs(session: Session):
    finished_jobs = get_finished_jobs(session)
    print(f"Found {len(finished_jobs)} finished jobs to clean up")
    for job in finished_jobs:
        dataset = session.get(
            RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
        ).dataset

        if dataset.tinker_license():
            update_job_status(session, job)
            continue

        success = delete_workspace(job)
        if not success:
            print(
                f"Failed to delete workspace for job (id={job.id}): retrying next iteration"
            )
            continue
        update_job_status(session, job)


def get_finished_jobs(session: Session) -> list[RdxJob]:
    statement = select(RdxJob).where(
        or_(
            RdxJob.status == JobStatus.finished_notified,
            RdxJob.status == JobStatus.failed_notified,
        )
    )
    finished_jobs = session.exec(statement)
    return list(finished_jobs)


def delete_workspace(session: Session, job: RdxJob) -> bool:
    try:
        rsc_client.delete_workspace(job.workspace_id)
    except Exception as error:
        print(
            f"Failed to delete workspace for job (id={job.id}, workspace_id={job.workspace_id}): {error}"
        )
        return False
    return True


def update_job_status(session: Session, job: RdxJob):
    if job.status == JobStatus.finished_notified:
        job.status = JobStatus.finished_notified_deleted
    if job.status == JobStatus.failed_notified:
        job.status = JobStatus.failed_notified_deleted
    session.add(job)
    session.commit()
