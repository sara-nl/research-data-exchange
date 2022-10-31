import owncloud
from common.db.db_client import DBClient
from common.email.mail_client import MailClient
from common.models.rdx_models import RdxDataset, RdxShare, RdxUser
from sqlmodel import select

from .email import get_message


def create_users(
    db: DBClient, new_shares: list[owncloud.ShareInfo]
) -> list[owncloud.ShareInfo]:
    for new_share in new_shares:
        data_steward = create_user(db, new_share.dataset_config.data_steward_email)
        setattr(new_share, "data_steward", data_steward)
        researcher = create_user(db, new_share.dataset_config.researcher_email)
        setattr(new_share, "researcher", researcher)
    return new_shares


def create_user(db: DBClient, email: str) -> RdxUser:
    with db.get_session() as session:
        statement = select(RdxUser).where(RdxUser.email == email)
        result = session.execute(statement).first()

    if result:
        user: RdxUser = result[0]
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


def notify(new_shares: list[owncloud.ShareInfo]):
    for new_share in new_shares:
        send_email(new_share)


def send_email(new_share: owncloud.ShareInfo):
    rdx_share: RdxShare = new_share.rdx_share
    researcher: RdxUser = new_share.researcher
    mail_client = MailClient(
        receiver=researcher.email,
        subject=f"Dataset {rdx_share.path} available for publication",
        message=get_message(researcher, rdx_share),
    )
    mail_client.mail()
