from common.email.mail_client import MailClient
from common.models.rdx_models import (
    AccessLicense,
    RdxAnalyst,
    RdxAnalystDatasetLink,
    RdxDataset,
    RdxUser,
)
from common.settings.app_settings import app_settings


def get_publication_message(rdx_user: RdxUser, rdx_dataset: RdxDataset) -> str:
    access_url = f"{app_settings.web_url}/access/{rdx_dataset.doi}"
    policies_url = f"{app_settings.web_url}/policies"

    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_user.email},</p>

    <p>Your dataset {rdx_dataset.title} has been published on RDX.</p>

    <p>The dataset has the following metadata associated with it:</p>
    <p style="margin-left:2em">Title: {rdx_dataset.title}</p>
    <p style="margin-left:2em">Author(s): {rdx_dataset.authors}</p>
    <p style="margin-left:2em">Description: {rdx_dataset.description}</p>
    <p style="margin-left:2em">DOI: {rdx_dataset.doi}</p>
    <p style="margin-left:2em">Access license: {AccessLicense.print_friendly_access_license(rdx_dataset.access_license)}. (You can learn more about the access license <a href="{policies_url}">here</a>.)</p>

    <p>What happens next?</p>

    <p>1. Your dataset is published</p>
    <p>The access page to your dataset: <samp>{access_url}</samp></p>
    <p>The above link points to the public page containing the use conditions of the dataset and helps
        other researchers obtain access to the dataset. The researchers will only receive the dataset after RDX
        ensures that the request meets all necessary conditions.</p>

    <p>2. Spread the word</p>
    <p>Anyone can request access to the dataset, but researchers may need help finding the access page.
        Please share the link with the relevant audience yourself (e.g. by adding it to the dataset
        page in the catalogue or repository).</p>

    <p>3. Get notified</p>
    <p>You'll get an email when someone requests access to this dataset.</p>
    <hr>
    <p><a href="{access_url}">Go to the dataset access page</a>.</p>
    {MailClient.BODY_CLOSE}
    """


def send_publication_email(rdx_user: RdxUser, rdx_dataset: RdxDataset):
    mail_client = MailClient(
        receiver=rdx_user.email,
        subject="Your dataset is published on RDX",
        message=get_publication_message(rdx_user, rdx_dataset),
    )
    mail_client.mail()


def get_download_message(
    rdx_analyst: RdxAnalyst,
    rdx_dataset: RdxDataset,
    rdx_dataset_analyst_link: RdxAnalystDatasetLink,
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_analyst.name},</p>

    <p>You can now download dataset '{rdx_dataset.title}'. Please use it responsibly and according to agreed conditions.</p>

    <a style="font-size: 18px; color: #008cba;" href="{rdx_dataset_analyst_link.download_url}">Download dataset</a>

    <p>You can find the conditions document in the attachment.</p>

    <p>Need help? Contact RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    {MailClient.BODY_CLOSE}
    """


def get_analyze_message(
    rdx_analyst: RdxAnalyst,
    rdx_dataset: RdxDataset,
    rdx_dataset_analyst_link: RdxAnalystDatasetLink,
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_analyst.name},</p>

    <p>You have requested access to the dataset '{rdx_dataset.title}'.</p>

    <p>This dataset can only be accessed in a secure analysis environment.</p>

    <a style="font-size: 18px; color: #008cba;" href="{app_settings.web_url}/analyze/{rdx_dataset_analyst_link.id}?token={rdx_analyst.token}">Proceed to secure analysis environment</a>

    <p>Please note that the link above is valid for a limited amount of time and will expire on <span style="white-space: nowrap">
    {rdx_analyst.token_expires_at.strftime("%c")}</span>. If you need more time, request access to the dataset again, and you will receive an email like this one with a new link.
    </p>

    <p>For more information, please contact the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    {MailClient.BODY_CLOSE}
    """


def send_access_email(
    rdx_analyst: RdxAnalyst,
    rdx_dataset: RdxDataset,
    rdx_dataset_analyst_link: RdxAnalystDatasetLink,
    conditions_path: str,
):
    if rdx_dataset.access_license == AccessLicense.download:
        message = get_download_message(
            rdx_analyst, rdx_dataset, rdx_dataset_analyst_link
        )
    if rdx_dataset.access_license in [
        AccessLicense.analyze_blind_with_output_check,
        AccessLicense.analyze_blind_no_output_check,
    ]:
        message = get_analyze_message(
            rdx_analyst, rdx_dataset, rdx_dataset_analyst_link
        )

    mail_client = MailClient(
        receiver=rdx_analyst.email,
        subject=f"Your access to {rdx_dataset.title}",
        message=message,
        attachment=conditions_path,
    )
    mail_client.mail()


def get_access_notification_message(
    rdx_analyst: RdxAnalyst,
    rdx_dataset: RdxDataset,
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear Data Owner,</p>

    <p>For your information, we received an access request for your dataset '{rdx_dataset.rdx_share.path}' from {rdx_analyst.name} &lt;{rdx_analyst.email}&gt;.</p>
    <p>This is an informational email, no action is required.</p>

    <p>Need help? Contact RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    {MailClient.BODY_CLOSE}
    """


def send_access_notification_email(
    rdx_user: RdxUser,
    rdx_analyst: RdxAnalyst,
    rdx_dataset: RdxDataset,
):
    mail_client = MailClient(
        receiver=rdx_user.email,
        subject=f"Access to dataset '{rdx_dataset.rdx_share.path}' was requested",
        message=get_access_notification_message(rdx_analyst, rdx_dataset),
    )
    mail_client.mail()
