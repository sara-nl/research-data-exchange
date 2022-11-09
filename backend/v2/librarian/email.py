from common.email.mail_client import MailClient
from common.models.rdx_models import RdxDataset, RdxUser
from common.settings.app_settings import app_settings


def get_message(rdx_user: RdxUser, rdx_dataset: RdxDataset) -> str:
    access_url = f"{app_settings.web_url}/access/{rdx_dataset.doi}"

    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_user.email},</p>

    <p>Your dataset has been published on RDX with a {rdx_dataset.access_license} access license.</p>

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
        subject=f"Your dataset is published on RDX",
        message=get_message(rdx_user, rdx_dataset),
    )
    mail_client.mail()
