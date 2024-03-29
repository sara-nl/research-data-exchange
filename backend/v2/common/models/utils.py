from sqlmodel import Session, select

from common.db.db_client import DBClient
from common.owncloud.owncloud_client import OwnCloudClient

from .rdx_models import DatasetStat, RdxDataset, RdxUser


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


def create_dataset_stat_model_from_dataset(
    session: Session, dataset: RdxDataset
) -> DatasetStat:
    if not dataset.owncloud_private_link:
        with OwnCloudClient() as oc_client:
            try:
                dataset.owncloud_private_link = oc_client.get_private_link(
                    dataset.rdx_share.path
                )
                session.add(dataset)
                session.commit()
                session.refresh(dataset)
            except Exception as error:
                print(
                    f"Cannot retrieve private link for dataset (id={dataset.id}): {error}"
                )

    dataset_stat_model = DatasetStat(
        id=dataset.id,
        doi=dataset.doi,
        title=dataset.title,
        access_license_id=dataset.access_license_id,
        signed=len(dataset.analyst_links),
        owncloud_private_link=dataset.owncloud_private_link,
    )

    for analyst_link in dataset.analyst_links:
        dataset_stat_model.analyzed += len(analyst_link.jobs)

    return dataset_stat_model
