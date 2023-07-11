from sqlmodel import Session, or_, select

from common.email.mail_client import MailClient
from common.models.rdx_models import (
    AccessLicense,
    JobStatus,
    RdxAnalystDatasetLink,
    RdxJob,
    RdxUser,
)

from .email import (
    get_message_for_analyst,
    get_message_for_researcher,
    get_workspace_message_for_analyst,
)


def notify_users(session: Session):
    send_workspace_ready_notification(session)
    send_finished_job_notification(session)


def send_workspace_ready_notification(session: Session):
    ready_tinker_jobs = get_ready_tinker_jobs(session)
    print(f"Found {len(ready_tinker_jobs)} ready tinker jobs")
    for job in ready_tinker_jobs:
        success = send_tinker_ready_email(session, job)
        if not success:
            print(
                f"Failed to send email for job (id={job.id}): retrying next iteration"
            )
            continue
        update_job_status(session, job)


def send_finished_job_notification(session: Session):
    finished_jobs = get_finished_jobs(session)
    print(f"Found {len(finished_jobs)} finished jobs")
    for job in finished_jobs:
        success = send_finished_job_email(session, job)
        if not success:
            print(
                f"Failed to send email for job (id={job.id}): retrying next iteration"
            )
            continue
        update_job_status(session, job)


def get_ready_tinker_jobs(session: Session) -> list[RdxJob]:
    statement = select(RdxJob).where(RdxJob.status == JobStatus.running)
    running_jobs = session.exec(statement)
    running_jobs = list(running_jobs)
    running_tinker_jobs = []
    for job in running_jobs:
        dataset = session.get(
            RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
        ).dataset
        if dataset.tinker_license():
            running_tinker_jobs.append(job)
    return running_tinker_jobs


def get_finished_jobs(session: Session) -> list[RdxJob]:
    statement = select(RdxJob).where(
        or_(
            RdxJob.status == JobStatus.finished,
            RdxJob.status == JobStatus.failed,
        )
    )
    finished_jobs = session.exec(statement)
    return list(finished_jobs)


def send_tinker_ready_email(session: Session, job: RdxJob) -> bool:
    rdx_analyst_dataset_link = session.get(
        RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
    )
    dataset = rdx_analyst_dataset_link.dataset
    analyst = rdx_analyst_dataset_link.analyst

    mail_client = MailClient(
        receiver=analyst.email,
        subject=f"Tinker workspace ready for dataset {dataset.title}",
        message=get_workspace_message_for_analyst(analyst, job, dataset),
    )

    try:
        mail_client.mail()
    except Exception as error:
        print(f"Failed to send email for job (id={job.id}: {error}")
        return False
    return True


def send_finished_job_email(session: Session, job: RdxJob) -> bool:
    rdx_analyst_dataset_link = session.get(
        RdxAnalystDatasetLink, job.rdx_analyst_dataset_link_id
    )
    dataset = rdx_analyst_dataset_link.dataset
    analyst = rdx_analyst_dataset_link.analyst
    if dataset.access_license in [
        AccessLicense.analyze_blind_with_output_check,
        AccessLicense.analyze_tinker_with_output_check,
    ]:
        researcher = session.get(
            RdxUser, rdx_analyst_dataset_link.dataset.researcher_id
        )
        if not researcher:
            print(f"Could not find researcher for dataset (id={dataset.id})")
            return False
        receiver = researcher.email
        message = get_message_for_researcher(researcher, analyst, job, dataset)
    if dataset.access_license in [
        AccessLicense.analyze_blind_no_output_check,
        AccessLicense.analyze_tinker_no_output_check,
    ]:
        receiver = analyst.email
        message = get_message_for_analyst(analyst, job, dataset)

    mail_client = MailClient(
        receiver=receiver,
        subject=f"Analysis job for dataset {dataset.title} is finished",
        message=message,
    )

    try:
        mail_client.mail()
    except Exception as error:
        print(f"Failed to send email for job (id={job.id}: {error}")
        return False
    return True


def update_job_status(session: Session, job: RdxJob):
    if job.status == JobStatus.running:
        job.status = JobStatus.running_notified
    if job.status == JobStatus.finished:
        job.status = JobStatus.finished_notified
    if job.status == JobStatus.failed:
        job.status = JobStatus.failed_notified
    session.add(job)
    session.commit()
