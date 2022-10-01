import typing

import owncloud
from pydantic import BaseModel, validator

from .settings import own_cloud_settings


class OwnCloudClient(BaseModel):
    oc: typing.Any

    @classmethod
    @validator("oc")
    def oc_must_be_oc_client_instance(cls, val):
        if not isinstance(val, owncloud.Client):
            raise ValueError("oc must be  instance of owncloud.Clientcontain")
        return val

    def __init__(self, **kwargs):
        oc = owncloud.Client(own_cloud_settings.webdav_server_uri)

        super().__init__(oc=oc, **kwargs)
        try:
            self.oc.login(
                own_cloud_settings.rdx_webdav_user,
                own_cloud_settings.rdx_webdav_password,
            )
        except Exception as error:
            print(f"Failed to login to owncloud {error}")
            raise error

    def __enter__(self):
        print("entering")
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        print("exiting")
        self.oc.logout()

    def get_shares(self, path: str = "") -> list[owncloud.ShareInfo]:
        print(f"Getting shares from owncloud for path { path or '/'}")
        try:
            return self.oc.get_shares(path=path, shared_with_me=True)
        except Exception as error:
            print(f"Error getting shares: {error}")
            raise error

    def get_shared_dirs(self) -> list[str]:
        shares = self.get_shares()
        return [
            s.get_path()
            for s in shares
            if s.share_info["mimetype"] == "httpd/unix-directory"
        ]

    def list_dir_contents(self, path: str = "") -> list[owncloud.FileInfo]:
        print(f"Listing contents for directory { path or '/'}")
        try:
            return self.oc.list(path)
        except Exception as error:
            print(f"Error listing contents in dir {path}: {error}")
            raise

    def dir_has_file(self, path: str, filename: str) -> bool:
        print(f"Checking directory {path} for {filename}")
        files = self.list_dir_contents(path)
        for file in files:
            if file.get_name() == filename:
                return True
        return False

    def make_public_link(self, file_path: str) -> str:
        print(f"Creating public link for {file_path}")
        try:
            link_info = self.oc.share_file_with_link(file_path)
        except Exception as error:
            print(f"Failed to create download link {error}")
            raise error
        print("link_info", link_info)
        return link_info.get_link()

    def delete_share(self, share_id: int) -> bool:
        print(f"Deleting share with ID {share_id}")
        try:
            result = self.oc.delete_share(share_id)
            return result
        except Exception as error:
            print(f"Failed to delete share with ID {share_id}: {error}")
            raise error

    def download_conditions(self, src_dir: str, target_dir: str) -> str:
        src_path = f"{src_dir}/conditions.pdf"
        target_path = f"{target_dir}/conditions.pdf"

        print(f"Downloading conditions {src_path} to {target_path}")
        try:
            result = self.oc.get_file(src_path, target_path)
            if not result:
                raise Exception(f"Download of {src_path} failed")
            return target_path
        except Exception as error:
            print(f"Failed to download conditions {src_path}: {error}")
            raise error
