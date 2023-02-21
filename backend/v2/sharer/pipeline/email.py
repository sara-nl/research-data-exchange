from common.email.mail_client import MailClient
from common.models.rdx_models import RdxShare, RdxUser
from common.settings.app_settings import app_settings


def get_publication_message_researcher(user: RdxUser, rdx_share: RdxShare) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {user.email},</p>

    <p>A new dataset, "{rdx_share.path}", has just been shared with RDX.</p>

    <p>You can now add/review the required metadata and publish it. That will make it available for download upon agreement to the associated use conditions.</p>

    <a style="font-size: 18px; color: #008cba;" href="{app_settings.web_url}/publish/{rdx_share.rdx_dataset_id}?token={user.token}">Proceed to publication screen</a>


    <p>Please note that the link above is valid for a limited amount of time and will expire on <span style="white-space: nowrap">
    {user.token_expires_at.strftime("%c")}</span>. If you need more time, unshare the dataset, wait for a confirmation email and share it again.
    </p>
    <p>If you want to make changes to metadata after publication or need any other kind of support, please get in touch with the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """


def get_publication_message_share_owner(rdx_share: RdxShare) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_share.additional_info_owner},</p>

    <p>A new dataset, "{rdx_share.path}", has just been successfully shared with RDX.</p>

    <p>An email has been sent to the researcher to add/review the metadata and publish the dataset.</p>

    <p>If you need any other kind of support, please get in touch with the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """


def get_misconfiguration_message(
    rdx_share: RdxShare,
    problem: str,
    solution: str,
) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_share.additional_info_owner},</p>

    <p>A new dataset, "{rdx_share.path}", has just been shared with RDX.</p>

    <p>Unfortunately the dataset is not yet ready for publication because {problem}.</p>

    <p>To fix this issue, you will need to {solution}.</p>

    <p>If you need support, please get in touch with the RDX team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """
