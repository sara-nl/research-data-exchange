from common.email.mail_client import MailClient
from common.models.rdx_models import (
    AccessLicense,
    RdxAnalyst,
    RdxDataset,
    RdxJob,
    RdxUser,
)
from common.settings.app_settings import app_settings


def get_workspace_message_for_analyst(
    analyst: RdxAnalyst, job: RdxJob, dataset: RdxDataset
) -> str:
    if dataset.access_license == AccessLicense.analyze_tinker_with_output_check:
        results_message = "After your workspace is deleted, you will receive your results after the data owner has verified the output."
    else:
        results_message = (
            "After your workspace is deleted, you will receive your results."
        )

    backslash = "\\"

    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {analyst.name},</p>

    <p>A tinker workspace to analyze the dataset {dataset.title} is ready for you. The workspace will be automatically deleted in 8 hours.</p>

    <p>You can access the workspace with an RDP client, such as Microsoft Remote Desktop.</p>

    <p>
        You can use the following information to connect to the workspace:
        <ul>
            <li>IP/Hostname: {job.workspace_ip}</li>
            <li>Username: {job.workspace_username}@src.local</li>
            <li>Password: {job.get_password()}</li>
        </ul>
    </p>

    <p>
        The workspace is protected. It is not possible to copy and paste to and from it and most outside internet access is restricted.
    </p>
    <p>
        If you want to export your results, you can upload files to a website, within the workspace you can open the page at c:{backslash}rdx{backslash}upload.url.
        You will need your workspace password to upload files.
    </p>
    </p>
        {results_message}
    </p>

    <p>If you need any other kind of support, please get in touch with the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """


def get_message_for_researcher(
    user: RdxUser, analyst: RdxAnalyst, job: RdxJob, dataset: RdxDataset
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {user.email},</p>

    <p>A job on RDX for your dataset {dataset.title} has just finished with status: {job.status}.</p>

    <p>You can view the results of the analysis <a href="{job.results_url}">here</a>.</p>

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

    <p>You can view the results of your analysis <a href="{job.results_url}">here</a>.</p>

    <p>You can also see an overview of the datasets shared with RDX in the <a href="{app_settings.web_url}/dashboard">RDX Dashboard</a>.</p>

    <p>If you need any other kind of support, please get in touch with the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """
