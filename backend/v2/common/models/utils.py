from sqlmodel import select

from common.db.db_client import DBClient

from .rdx_models import RdxUser


def create_rdx_user(db: DBClient, email: str) -> RdxUser:
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
