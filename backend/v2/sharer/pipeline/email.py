from common.email.mail_client import MailClient
from common.models.rdx_models import RdxShare, RdxUser
from common.settings.app_settings import app_settings

DATASET_CONFIG_EXAMPLE = """
<p>
The dataset.yml file should have the following structure:
</p>
<pre>
---
data_steward_email: alice@example.nl
researcher_email: bill@example.nl
access_license: sign+analyze
metadata:
  doi: 10.1000/182
  title: Test title
  description: Test description
  authors: Author 1, Author 2, Author 3
</pre>
<p>
Note: the metadata section is optional, but if it is included all four fields need to be entered.
</p>
"""


def get_message(user: RdxUser, rdx_share: RdxShare) -> str:
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


def get_misconfiguration_message(
    rdx_share: RdxShare,
    problem: str,
    solution: str,
    show_dataset_config_example: bool = False,
) -> str:
    if show_dataset_config_example:
        example = DATASET_CONFIG_EXAMPLE
    else:
        example = ""

    return f"""
    {MailClient.BODY_OPEN}
    <p>Dear {rdx_share.additional_info_owner},</p>

    <p>A new dataset, "{rdx_share.path}", has just been shared with RDX.</p>

    <p>Unfortunately the dataset is not yet ready for publication because {problem}.</p>

    <p>To fix this issue, you will need to {solution}.</p>

    {example}

    <p>If you need support, please get in touch with the RDX team: <a href="mailto:{MailClient.SENDER}">{MailClient.SENDER}</a>
    </p>
    {MailClient.BODY_CLOSE}
    """
