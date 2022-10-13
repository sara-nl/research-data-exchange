import owncloud
import yaml
from pydantic import ValidationError, parse_obj_as
from sqlmodel import select

from common.db.db_client import DBClient
from common.models.rdx_share import RdxShare
from common.owncloud.owncloud_client import OwnCloudClient

from .dataset import DatasetConfig


def get_eligible_shared_dirs() -> list[owncloud.ShareInfo]:
    with OwnCloudClient() as oc_client:
        shared_dirs = oc_client.get_shared_dirs()
    print("shared", shared_dirs)

    shared_dirs = filter(check_resharing_permissions, shared_dirs)
    shared_dirs = map(add_files, shared_dirs)
    shared_dirs = filter(check_for_conditions_file, shared_dirs)
    shared_dirs = filter(check_for_dataset_config_file, shared_dirs)
    shared_dirs = map(add_dataset_config, shared_dirs)
    shared_dirs = filter(lambda item: item is not None, shared_dirs)
    shared_dirs = list(shared_dirs)

    return shared_dirs


def check_resharing_permissions(directory: owncloud.ShareInfo) -> bool:
    if directory.get_permissions() >= OwnCloudClient.MINIMUM_PERMISSION_LEVEL:
        return True
    print(
        f"Share ({directory.get_id()}: {directory.get_path()}) does not have re-sharing permissions"
    )
    return False


def add_files(directory: owncloud.ShareInfo) -> owncloud.ShareInfo:
    with OwnCloudClient() as oc_client:
        files = oc_client.list_dir_contents(directory.get_path())
        setattr(
            directory,
            "files",
            [normalize_files(directory, file) for file in files if not file.is_dir()],
        )
    return directory


def normalize_files(directory: owncloud.ShareInfo, file: owncloud.FileInfo) -> str:
    dir_path = file.get_path().replace(f"{directory.get_path()}", "")
    if dir_path:
        dir_path = f"{dir_path}/"
    return f"{dir_path}{file.get_name()}"


def check_for_conditions_file(directory: owncloud.ShareInfo) -> bool:
    # TODO: get conditions.pdf from config settings?
    print(directory.files)
    if "conditions.pdf" in directory.files:
        return True
    print(
        f"Share ({directory.get_id()}: {directory.get_path()}) does not have a conditions.pdf"
    )
    return False


def check_for_dataset_config_file(directory: owncloud.ShareInfo) -> bool:
    # TODO: get dataset.yml from config settings?
    if "dataset.yml" in directory.files:
        return True
    print(
        f"Share ({directory.get_id()}: {directory.get_path()}) does not have a dataset.yml"
    )
    return False


def add_dataset_config(directory: owncloud.ShareInfo) -> owncloud.ShareInfo:
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
        return None

    try:
        print(f"Validating {directory.get_path()}/dataset.yml")
        dataset_config = parse_obj_as(DatasetConfig, dataset_config)
    except ValidationError as error:
        print(f"Validation of {directory.get_path()}/dataset.yml failed: {error}")
        return None

    setattr(directory, "dataset_config", dataset_config)
    return directory


def compare_shared_dirs_with_stored_dirs(
    db: DBClient, shared_dirs: list[owncloud.ShareInfo]
) -> tuple[list[owncloud.ShareInfo], list[RdxShare]]:
    with db.get_session() as session:
        statement = select(RdxShare)
        stored_shares = session.exec(statement).all()

    stored_share_ids = set(map(lambda x: x.share_id, stored_shares))
    discovered_share_ids = set(map(lambda x: x.get_id(), shared_dirs))

    new_share_ids = discovered_share_ids.difference(stored_share_ids)
    removed_share_ids = stored_share_ids.difference(discovered_share_ids)

    new_shared_dirs = [x for x in shared_dirs if x.get_id() in new_share_ids]
    removed_shared_dirs = [x for x in stored_shares if x.share_id in removed_share_ids]

    return (new_shared_dirs, removed_shared_dirs)
