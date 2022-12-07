from common.db.db_client import DBClient
from common.email.mail_client import MailClient
from common.models.rdx_models import RdxUser, ShareStatus
from common.models.utils import create_rdx_user
from common.owncloud.owncloud_client import ShareInfo

from .email import get_misconfiguration_message, get_publication_message_researcher


def create_users(db: DBClient, new_shares: list[ShareInfo]) -> list[ShareInfo]:
    for new_share in new_shares:
        data_steward = create_rdx_user(db, new_share.rdx_share.additional_info_owner)
        setattr(new_share, "data_steward", data_steward)
    return new_shares


def notify(db: DBClient, shared_dirs: list[ShareInfo]):
    for shared_dir in shared_dirs:
        try:
            notification_sent = send_email(shared_dir)
        except Exception as error:
            print(
                f"Failed to send email for share ({shared_dir.get_id()}: {shared_dir.get_path()}): {error}"
            )
            continue
        if not notification_sent:
            continue
        with db.get_session() as session:
            new_status = shared_dir.rdx_share.share_status + 1
            shared_dir.rdx_share.set_new_share_status(new_status, notification=True)
            shared_dir.rdx_share.update_share_status()
            session.add(shared_dir.rdx_share)
            session.commit()
            session.refresh(shared_dir.rdx_share)


def send_email(shared_dir: ShareInfo) -> bool:
    mail_clients = []

    if shared_dir.rdx_share.share_status == ShareStatus.dataset_accepted:
        mail_clients.append(get_dataset_ready_email_data_steward(shared_dir))
    if shared_dir.rdx_share.share_status == ShareStatus.invalid_permissions:
        problem = "the RDX user does not have permission to reshare the dataset"
        solution = "check the 'can share' box for the RDX user in Research Drive"
        mail_clients.append(get_misconfiguration_email(shared_dir, problem, solution))
    if shared_dir.rdx_share.share_status == ShareStatus.missing_conditions:
        problem = "the shared directory is missing a conditions.pdf file"
        solution = "add a conditions.pdf file to the directory shared with RDX"
        mail_clients.append(get_misconfiguration_email(shared_dir, problem, solution))

    for mail_client in mail_clients:
        mail_client.mail()

    return bool(mail_clients)


def get_dataset_ready_email_data_steward(shared_dir: ShareInfo) -> MailClient:
    researcher: RdxUser = shared_dir.data_steward
    return MailClient(
        receiver=researcher.email,
        subject=f"Dataset {shared_dir.rdx_share.path} available for publication",
        message=get_publication_message_researcher(researcher, shared_dir.rdx_share),
    )


def get_misconfiguration_email(
    shared_dir: ShareInfo,
    problem: str,
    solution: str,
) -> MailClient:
    return MailClient(
        receiver=shared_dir.rdx_share.additional_info_owner,
        subject=f"Problem adding dataset {shared_dir.rdx_share.path} to RDX",
        message=get_misconfiguration_message(shared_dir.rdx_share, problem, solution),
    )
