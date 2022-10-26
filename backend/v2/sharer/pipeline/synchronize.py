import owncloud
from sqlmodel import select

from common.db.db_client import DBClient
from common.models.rdx_dataset import RdxDataset
from common.models.rdx_share import RdxShare
from common.owncloud.owncloud_client import OwnCloudClient


def add_new_shares(db: DBClient, new_shares: list[owncloud.ShareInfo]):
    for new_share in new_shares:
        create_new_share(db, new_share)


def create_new_share(db: DBClient, new_share: owncloud.ShareInfo):
    f"Adding share ({new_share.get_id()}: {new_share.get_path()}) to the database"

    rdx_dataset = create_dataset(db, new_share)

    with db.get_session() as session:
        rdx_share = RdxShare(
            share_id=new_share.get_id(),
            path=new_share.get_path(),
            uid_owner=new_share.get_uid_owner(),
            additional_info_owner=new_share.share_info["additional_info_owner"],
            permissions=new_share.get_permissions(),
            rdx_dataset_id=rdx_dataset.id,
        )

        session.add(rdx_share)
        session.commit()
        session.refresh(rdx_share)
    setattr(new_share, "rdx_share", rdx_share)
    f"Finished adding share ({new_share.get_id()}: {new_share.get_path()}) to the database"


def create_dataset(db: DBClient, new_share: RdxShare) -> RdxDataset:
    with OwnCloudClient() as oc_client:
        try:
            conditions_share_id, conditions_url = oc_client.make_public_link(
                f"{new_share.get_path()}/conditions.pdf"
            )
        except Exception as error:
            print(
                f"Failed to create download link for share {new_share.get_id()}: {new_share.get_path()}/conditions.pdf"
            )
            raise error

    with db.get_session() as session:
        statement = select(RdxDataset).where(RdxDataset.share_id == new_share.get_id())
        rdx_dataset = session.exec(statement).first()
        if not rdx_dataset:
            rdx_dataset = RdxDataset(
                share_id=new_share.get_id(),
                files=new_share.files,
                conditions_url=conditions_url,
                condtions_share_id=conditions_share_id,
                data_steward_id=new_share.data_steward.id,
                researcher_id=new_share.researcher.id,
            )
            if new_share.dataset_config.metadata:
                metadata = new_share.dataset_config.metadata
                rdx_dataset.doi = metadata.doi
                rdx_dataset.title = metadata.title
                rdx_dataset.description = metadata.description
                rdx_dataset.authors = metadata.authors

            session.add(rdx_dataset)
            session.commit()
            session.refresh(rdx_dataset)

        return rdx_dataset


def remove_deleted_shares(db: DBClient, deleted_shares: list[RdxShare]):
    for rdx_share in deleted_shares:
        print(f"Deleting Share {rdx_share.share_id}: {rdx_share.path}")
        with db.get_session() as session:
            session.delete(rdx_share)
            session.commit()
        remove_dataset(db, rdx_share)


def remove_dataset(db: DBClient, rdx_share: RdxShare):
    with db.get_session() as session:
        rdx_dataset = session.get(RdxDataset, rdx_share.rdx_dataset_id)

    if not rdx_dataset:
        return

    with OwnCloudClient() as oc_client:
        print(f"Deleting shared conditions url for rdx_dataset {rdx_dataset.id}")
        oc_client.delete_share(rdx_dataset.condtions_share_id)

    with db.get_session() as session:
        print(f"Deleting dataset {rdx_dataset.id}")
        session.delete(rdx_dataset)
        session.commit()
