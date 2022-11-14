from sqlmodel import select
from common.models.rdx_models import RdxShare, RdxUser, ShareStatus

from common.db.db_client import DBClient
from common.email.mail_client import MailClient
from common.owncloud.owncloud_client import ShareInfo

from .email import (
    get_misconfiguration_message,
    get_publication_message_researcher,
    get_publication_message_share_owner,
)


def create_users(db: DBClient, new_shares: list[ShareInfo]) -> list[ShareInfo]:
    for new_share in new_shares:
        data_steward = create_user(db, new_share.dataset_config.data_steward_email)
        setattr(new_share, "data_steward", data_steward)
        researcher = create_user(db, new_share.dataset_config.researcher_email)
        setattr(new_share, "researcher", researcher)
    return new_shares


def create_user(db: DBClient, email: str) -> RdxUser:
    with db.get_session() as session:
        statement = select(RdxUser).where(RdxUser.email == email)
        result = session.exec(statement).first()

    if result:
        user: RdxUser = result
        print(f"User {user.email} already exists")
        if user.has_token_expired():
            print(f"User access token has expired for {user.email}")
            new_token, token_expires_at = RdxUser.create_new_token()
            user.token = new_token
            user.token_expires_at = token_expires_at
            with db.get_session() as session:
                print(f"Updating user access token for {user.email}")
                session.add(user)
                session.commit()
                session.refresh(user)
                print(f"Finished updating user access token for {user.email}")
        return user

    if not result:
        print(f"Creating new user for {email}")
        token, token_expires_at = RdxUser.create_new_token()
        new_user = RdxUser(email=email, token=token, token_expires_at=token_expires_at)
        with db.get_session() as session:
            session.add(new_user)
            session.commit()
            session.refresh(new_user)
        return new_user


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
        mail_clients.append(get_dataset_ready_email_researcher(shared_dir))
        mail_clients.append(get_dataset_ready_email_share_owner(shared_dir))
    if shared_dir.rdx_share.share_status == ShareStatus.invalid_permissions:
        problem = "the RDX user does not have permission to reshare the dataset"
        solution = "check the 'can share' box for the RDX user in Research Drive"
        mail_clients.append(get_misconfiguration_email(shared_dir, problem, solution))
    if shared_dir.rdx_share.share_status == ShareStatus.missing_conditions:
        problem = "the shared directory is missing a conditions.pdf file"
        solution = "add a conditions.pdf file to the directory shared with RDX"
        mail_clients.append(get_misconfiguration_email(shared_dir, problem, solution))
    if shared_dir.rdx_share.share_status == ShareStatus.missing_dataset_config:
        problem = "the shared directory is missing a dataset.yml file with metadata and configuration options for the dataset"
        solution = "add a dataset.yml file to the directory shared with RDX"
        mail_clients.append(
            get_misconfiguration_email(
                shared_dir, problem, solution, show_dataset_config_example=True
            )
        )
    if shared_dir.rdx_share.share_status == ShareStatus.invalid_dataset_config:
        problem = "the configuration specified in the dataset.yml file is incomplete or invalid"
        solution = "change the contents of the dataset.yml file, see the example below for a valid example"
        mail_clients.append(
            get_misconfiguration_email(
                shared_dir, problem, solution, show_dataset_config_example=True
            )
        )

    for mail_client in mail_clients:
        mail_client.mail()

    return bool(mail_clients)


def get_dataset_ready_email_researcher(shared_dir: ShareInfo) -> MailClient:
    researcher: RdxUser = shared_dir.researcher
    return MailClient(
        receiver=researcher.email,
        subject=f"Dataset {shared_dir.rdx_share.path} available for publication",
        message=get_publication_message_researcher(researcher, shared_dir.rdx_share),
    )


def get_dataset_ready_email_share_owner(shared_dir: ShareInfo) -> MailClient:
    return MailClient(
        receiver=shared_dir.rdx_share.additional_info_owner,
        subject=f"Dataset {shared_dir.rdx_share.path} successfully shared with RDX",
        message=get_publication_message_share_owner(shared_dir.rdx_share),
    )


def get_misconfiguration_email(
    shared_dir: ShareInfo,
    problem: str,
    solution: str,
    show_dataset_config_example: bool = False,
) -> MailClient:
    return MailClient(
        receiver=shared_dir.rdx_share.additional_info_owner,
        subject=f"Problem adding dataset {shared_dir.rdx_share.path} to RDX",
        message=get_misconfiguration_message(
            shared_dir.rdx_share, problem, solution, show_dataset_config_example
        ),
    )
