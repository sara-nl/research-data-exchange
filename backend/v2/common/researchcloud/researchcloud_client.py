import json
import os

import requests
from pydantic import BaseModel

from .token_helper import check_token
from .workspace_payload import get_blind_workspace_payload, get_tinker_workspace_payload


class ResearchCloudClient(BaseModel):
    def create_blind_workspace(
        self, name: str, script_location: str, researchdrive_path: str, results_dir: str
    ) -> str:
        payload = json.loads(
            get_blind_workspace_payload(
                name, script_location, researchdrive_path, results_dir
            )
        )

        return self.create_workspace(payload)

    def create_tinker_workspace(
        self,
        name: str,
        username: str,
        password: str,
        researchdrive_path: str,
        upload_url: str,
    ) -> str:
        payload = json.loads(
            get_tinker_workspace_payload(
                name, username, password, researchdrive_path, upload_url
            )
        )

        return self.create_workspace(payload)

    def create_workspace(self, payload: str) -> str:
        check_token()

        headers = {
            "authorization": os.getenv("RSC_ACCESS_TOKEN"),
            "content-type": "application/json",
        }

        try:
            r = requests.post(
                "https://gw.live.surfresearchcloud.nl/workspace/workspaces/",
                headers=headers,
                json=payload,
            )
        except Exception as error:
            print(f"Could not create workspace in ResearchCloud: {error}")
            raise error

        if r.status_code != 201:
            print(f"Could not create workspace in ResearchCloud: {r.content}")
            raise Exception(f"Could not create workspace in ResearchCloud: {r.content}")

        body = json.loads(r.content)
        return body["id"]

    def get_workspace_status(self, workspace_id: str) -> str:
        workspace = self.get_workspace(workspace_id)
        return workspace["status"]

    def get_workspace_ip(self, workspace_id: str) -> str:
        workspace = self.get_workspace(workspace_id)
        if "resource_meta" in workspace.keys():
            return workspace["resource_meta"]["ip"]
        return False

    def get_workspace(self, workspace_id: str) -> dict:
        check_token()
        headers = {"authorization": os.getenv("RSC_ACCESS_TOKEN")}
        try:
            r = requests.get(
                f"https://gw.live.surfresearchcloud.nl/workspace/workspaces/?id={workspace_id}",
                headers=headers,
            )
        except Exception as error:
            print(
                f"Could not get workspace (id={workspace_id}) from ResearchCloud: {error}"
            )
            raise error

        body = json.loads(r.content)
        return body["results"][0]

    def delete_workspace(self, workspace_id: str):
        status = self.get_workspace_status(workspace_id)
        if status not in ["running", "failed"]:
            print(
                f"Cannot delete workspace (id={workspace_id}) because status is {status} instead of 'running' or 'failed'."
            )
            raise Exception("Cannot delete workspace that is not 'running' or 'failed'")
        check_token()
        headers = {"authorization": os.getenv("RSC_ACCESS_TOKEN")}
        try:
            r = requests.delete(
                f"https://gw.live.surfresearchcloud.nl/workspace/workspaces/{workspace_id}/",
                headers=headers,
            )
        except Exception as error:
            print(
                f"Could not delete workspace (id={workspace_id}) from ResearchCloud: {error}"
            )
            raise error
