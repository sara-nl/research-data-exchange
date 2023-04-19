from common.email.mail_client import MailClient
from common.models.rdx_models import (
    AccessLicense,
    RdxAnalyst,
    RdxAnalystDatasetLink,
    RdxDataset,
    RdxUser,
)
from common.settings.app_settings import app_settings


def get_dashboard_signin_message(rdx_user: RdxUser, role: str) -> str:
    return f"""
    {MailClient.BODY_OPEN}
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_user.email},</p>

    <p>Here is your link to the RDX Dashboard.</p>

    <p>Please note that the link above is valid for a limited amount of time and will expire on <span style="white-space: nowrap">
    {rdx_user.token_expires_at.strftime("%c")}</span>. If you need more time, sign in to the dashboard again <a href="{app_settings.web_url}/dashboard">here</a>, and you will receive an email like this one with a new link.
    </p>

    <a style="font-size: 18px; color: #008cba;" href="{app_settings.web_url}/dashboard/{role}?token={rdx_user.token}">Open RDX Dashboard</a>

    <p>Need help? Contact the RDX support team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    {MailClient.BODY_CLOSE}
    """


def send_dashboard_signin_email(rdx_user: RdxUser, role: str):
    mail_client = MailClient(
        receiver=rdx_user.email,
        subject="Login to RDX Dashboard",
        message=get_dashboard_signin_message(rdx_user, role),
    )
    mail_client.mail()
