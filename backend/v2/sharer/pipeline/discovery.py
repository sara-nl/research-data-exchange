import owncloud
import yaml
from pydantic import ValidationError, parse_obj_as
from sqlmodel import select

from common.db.db_client import DBClient
from common.models.rdx_models import RdxShare, ShareStatus
from common.owncloud.owncloud_client import OwnCloudClient, ShareInfo

from .dataset import DatasetConfig


def get_eligible_shared_dirs(db: DBClient) -> list[ShareInfo]:
    with OwnCloudClient() as oc_client:
        shared_dirs = oc_client.get_shared_dirs()

    print(f"Found shared dirs: {shared_dirs}")

    shared_dirs = add_rdx_models(db, shared_dirs)
    shared_dirs = map(check_resharing_permissions, shared_dirs)
    shared_dirs = map(add_files, shared_dirs)
    shared_dirs = map(check_for_conditions_file, shared_dirs)
    shared_dirs = map(check_for_dataset_config_file, shared_dirs)
    shared_dirs = map(add_dataset_config, shared_dirs)
    shared_dirs = list(shared_dirs)
    save_rdx_models(db, shared_dirs)

    return shared_dirs


def add_rdx_models(
    db: DBClient, shared_dirs: list[owncloud.ShareInfo]
) -> list[ShareInfo]:
    for shared_dir in shared_dirs:
        with db.get_session() as session:
            statement = select(RdxShare).where(RdxShare.share_id == shared_dir.get_id())
            rdx_share = session.exec(statement).first()
            if not rdx_share:
                print(f"Share ({shared_dir.get_id()}: {shared_dir.get_path()}) is new")
                rdx_share = RdxShare(
                    share_id=shared_dir.get_id(),
                    path=shared_dir.get_path(),
                    uid_owner=shared_dir.get_uid_owner(),
                    additional_info_owner=shared_dir.share_info[
                        "additional_info_owner"
                    ],
                    permissions=shared_dir.get_permissions(),
                    share_time=shared_dir.get_share_time(),
                    share_status=ShareStatus.share_discovered,
                )
            rdx_share.initialize_private_attributes()
            rdx_share.permissions = shared_dir.get_permissions()
        setattr(shared_dir, "rdx_share", rdx_share)
    return shared_dirs


def check_resharing_permissions(directory: ShareInfo) -> ShareInfo:
    if directory.get_permissions() < OwnCloudClient.MINIMUM_PERMISSION_LEVEL:
        print(
            f"Share ({directory.get_id()}: {directory.get_path()}) does not have re-sharing permissions"
        )
        directory.rdx_share.set_new_share_status(ShareStatus.invalid_permissions)
    return directory


def add_files(directory: ShareInfo) -> ShareInfo:
    with OwnCloudClient() as oc_client:
        files = oc_client.list_dir_contents(directory.get_path())
        setattr(
            directory,
            "files",
            [normalize_files(directory, file) for file in files if not file.is_dir()],
        )
    return directory


def normalize_files(directory: ShareInfo, file: owncloud.FileInfo) -> str:
    dir_path = file.get_path().replace(f"{directory.get_path()}", "")
    if dir_path:
        dir_path = f"{dir_path}/"
    return f"{dir_path}{file.get_name()}"


def check_for_conditions_file(directory: ShareInfo) -> ShareInfo:
    # TODO: get conditions.pdf from config settings?
    if "conditions.pdf" not in directory.files:
        print(
            f"Share ({directory.get_id()}: {directory.get_path()}) does not have a conditions.pdf"
        )
        directory.rdx_share.set_new_share_status(ShareStatus.missing_conditions)
    return directory


def check_for_dataset_config_file(directory: ShareInfo) -> ShareInfo:
    # TODO: get dataset.yml from config settings?
    if "dataset.yml" not in directory.files:
        print(
            f"Share ({directory.get_id()}: {directory.get_path()}) does not have a dataset.yml"
        )
        directory.rdx_share.set_new_share_status(ShareStatus.missing_dataset_config)
    return directory


def add_dataset_config(directory: ShareInfo) -> ShareInfo:
    if "dataset.yml" not in directory.files:
        return directory

    # TODO: get dataset.yml from config settings?
    with OwnCloudClient() as oc_client:
        dataset_config = oc_client.get_file_contents(
            f"{directory.get_path()}/dataset.yml"
        )

    try:
        print(f"Parsing {directory.get_path()}/dataset.yml as yaml")
        dataset_config = yaml.safe_load(dataset_config)
    except yaml.YAMLError as error:
        print(f"Failed to parse yaml for {directory.get_path()}/dataset.yml: {error}")
        directory.rdx_share.set_new_share_status(ShareStatus.invalid_dataset_config)
        return directory

    try:
        print(f"Validating {directory.get_path()}/dataset.yml")
        dataset_config = parse_obj_as(DatasetConfig, dataset_config)
    except ValidationError as error:
        print(f"Validation of {directory.get_path()}/dataset.yml failed: {error}")
        directory.rdx_share.set_new_share_status(ShareStatus.invalid_dataset_config)
        return directory

    setattr(directory, "dataset_config", dataset_config)
    return directory


def save_rdx_models(db: DBClient, shared_dirs: list[ShareInfo]):
    for shared_dir in shared_dirs:
        print(
            f"Saving share ({shared_dir.get_id()}: {shared_dir.get_path()}) to the database"
        )
        shared_dir.rdx_share.update_share_status()
        with db.get_session() as session:
            session.add(shared_dir.rdx_share)
            session.commit()
            session.refresh(shared_dir.rdx_share)


def compare_shared_dirs_with_stored_dirs(
    db: DBClient, shared_dirs: list[ShareInfo]
) -> tuple[list[ShareInfo], list[ShareInfo], list[RdxShare]]:
    with db.get_session() as session:
        statement = select(RdxShare)
        stored_shares = session.exec(statement).all()

    stored_share_ids = set(map(lambda x: x.share_id, stored_shares))
    discovered_share_ids = set(map(lambda x: x.get_id(), shared_dirs))

    removed_share_ids = stored_share_ids.difference(discovered_share_ids)
    removed_shared_dirs = [x for x in stored_shares if x.share_id in removed_share_ids]

    new_shared_dirs = [
        x
        for x in shared_dirs
        if x.rdx_share.is_eligible() and not x.rdx_share.is_finished()
    ]
    ineligble_shared_dirs = [x for x in shared_dirs if not x.rdx_share.is_eligible()]

    return (new_shared_dirs, ineligble_shared_dirs, removed_shared_dirs)
