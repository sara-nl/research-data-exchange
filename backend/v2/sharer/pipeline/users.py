import owncloud
from sqlmodel import select

from common.db.db_client import DBClient
from common.models.rdx_user import RdxUser


def create_users(
    db: DBClient, new_shares: list[owncloud.ShareInfo]
) -> list[owncloud.ShareInfo]:
    for new_share in new_shares:
        data_steward_id = create_user(db, new_share.dataset_config.data_steward_email)
        setattr(new_share, "data_steward_id", data_steward_id)
        researcher_id = create_user(db, new_share.dataset_config.researcher_email)
        setattr(new_share, "researcher_id", researcher_id)
    return new_shares


def create_user(db: DBClient, email: str) -> int:
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
        return user.id

    if not result:
        print(f"Creating new user for {email}")
        token, token_expires_at = RdxUser.create_new_token()
        new_user = RdxUser(email=email, token=token, token_expires_at=token_expires_at)
        with db.get_session() as session:
            session.add(new_user)
            session.commit()
            session.refresh(new_user)
        return new_user.id
