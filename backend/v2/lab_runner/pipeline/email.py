from common.email.mail_client import MailClient
from common.models.rdx_models import RdxAnalyst, RdxDataset, RdxJob, RdxUser
from common.settings.app_settings import app_settings


def get_message_for_researcher(
    user: RdxUser, analyst: RdxAnalyst, job: RdxJob, dataset: RdxDataset
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {user.email},</p>

    <p>A job on RDX for your dataset {dataset.title} has just finished with status: {job.status}.</p>

    <p>You can view the results of the analysis here: TODO</p>

    <p>
        After you have verified the output and made sure it has satisfied your requirements, please share the results folder in ResearchDrive with the researcher.
        Their email address is {analyst.email}
    </p>

    <p>If you need any other kind of support, please get in touch with the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """


def get_message_for_analyst(
    analyst: RdxAnalyst, job: RdxJob, dataset: RdxDataset
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {analyst.name},</p>

    <p>Your job on RDX for the dataset {dataset.title} has just finished with status: {job.status}.</p>

    <p>You can view the results of your analysis here: TODO</p>

    <p>If you need any other kind of support, please get in touch with the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """
