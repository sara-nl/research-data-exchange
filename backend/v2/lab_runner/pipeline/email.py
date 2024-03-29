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
    analyst: RdxAnalyst, job: RdxJob, dataset: RdxDataset, researcher: RdxUser
) -> str:
    if dataset.access_license == AccessLicense.analyze_tinker_with_output_check:
        results_message = f"After your workspace is automatically deleted, in 8 hours, you will receive your results after the data owner ({researcher.email}) has verified the output."
    else:
        results_message = "After your workspace is automatically deleted, in 8 hours, you will receive your results."

    backslash = "\\"

    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {analyst.name},</p>

    <p>A tinker workspace to analyze the dataset {dataset.title} is ready for you. The workspace will be automatically deleted in 8 hours.</p>

    <h3>Access</h3>
    <p>You can access the workspace with an RDP client, such as <a href="https://apps.apple.com/nl/app/microsoft-remote-desktop/id1295203466">Microsoft Remote Desktop for Mac</a> and <a href="https://apps.microsoft.com/store/detail/microsoft-remote-desktop/9WZDNCRFJ3PS">Microsoft Remote Desktop for Windows</a>.</p>

    <p>
        You can use the following information to connect to the workspace:
        <ul>
            <li>IP/Hostname: {job.workspace_ip}</li>
            <li>Username: {job.workspace_username}@src.local</li>
            <li>Password: {job.get_password()}</li>
        </ul>
    </p>

    <p>
        To access the workspace with an RDP client follow these steps:
        <ol>
            <li>Open <a href="https://apps.apple.com/nl/app/microsoft-remote-desktop/id1295203466">Microsoft Remote Desktop for Mac</a> or <a href="https://apps.microsoft.com/store/detail/microsoft-remote-desktop/9WZDNCRFJ3PS">Microsoft Remote Desktop for Windows</a>.</li>
            <li>Click on the '+' sign and then on "Add PC".</li>
            <li>Enter {job.workspace_ip} in the "PC name" field.</li>
            <li>Click on "Add".</li>
            <li>Double click on the thumbnail for the newly created PC in the overview.</li>
            <li>Enter {job.workspace_username}@src.local in the "Username" field.</li>
            <li>Enter {job.get_password()} in the "Password" field.</li>
            <li>Click on "Continue".</li>
        </ol>


    <h3>Analyzing the data</h3>
    <p>
        The workspace is protected. It is not possible to copy and paste to and from it and most outside internet access is restricted.
    </p>
    <p>
        You can find the dataset in this location c:{backslash}rdx{backslash}dataset. RStudio and Jupyter Lab (with Python) are installed in the workspace.
    </p>

    <h3>Exporting your results</h3>
    <p>
        If you want to export your results from the workspace, you can upload files to a dedicated website, within the workspace you can open this upload page at c:{backslash}rdx{backslash}upload.url.
        You will need your workspace password to upload files. Note: you can copy+paste the password from this file: c:{backslash}rdx{backslash}upload-password.txt.
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
