import json
import os

import requests
from pydantic import BaseModel

from .token_helper import check_token
from .workspace_payload import get_workspace_payload


class ResearchCloudClient(BaseModel):
    def create_workspace(
        self, name: str, script_location: str, researchdrive_path: str
    ) -> str:
        check_token()

        payload = json.loads(
            get_workspace_payload(name, script_location, researchdrive_path)
        )
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
        return body["results"][0]["status"]

    def delete_workspace(self, workspace_id: str):
        status = self.get_workspace_status(workspace_id)
        if status != "running":
            print(
                f"Cannot delete workspace (id={workspace_id}) because status is {status} instead of 'running'."
            )
            raise Exception("Cannot delete workspace that is not 'running'")
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
