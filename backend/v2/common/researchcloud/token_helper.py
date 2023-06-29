import json
import os
from datetime import datetime, timedelta

import requests

from .settings import research_cloud_settings


def check_token():
    if "RSC_ACCESS_TOKEN" not in os.environ:
        set_new_token()
    if "RSC_ACCESS_TOKEN_EXPIRES_AT" not in os.environ:
        set_new_token()

    access_token_expires_at = datetime.utcfromtimestamp(
        float(os.getenv("RSC_ACCESS_TOKEN_EXPIRES_AT"))
    )

    if access_token_expires_at < (datetime.now() - timedelta(hours=1)):
        print("ResearchCloud access token will expire in an hour")
        set_new_token()


def set_new_token():
    attempts = 0

    while attempts < 3:
        attempts += 1
        try:
            new_token_info = fetch_new_token()
            break
        except Exception as error:
            print(
                f"Error getting new research cloud token: {error}, attempt: {attempts}"
            )

    os.environ["RSC_ACCESS_TOKEN"] = new_token_info["access_token"]
    os.environ["RSC_ACCESS_TOKEN_EXPIRES_AT"] = str(
        new_token_info["access_token_expires_at"]
    )


def fetch_new_token():
    response = requests.post(
        "https://oauth2.live.surfresearchcloud.nl/token/",
        headers={"Content-Type": "application/json"},
        json={
            "client_id": f"{research_cloud_settings.client_id}",
            "client_secret": f"{research_cloud_settings.client_secret}",
            "grant_type": "client_credentials",
        },
    )

    body = json.loads(response.content)
    access_token = body["access_token"]
    access_token_expires_in = body["expires_in"]
    access_token_expires_at = datetime.now() + timedelta(
        seconds=int(access_token_expires_in)
    )

    return {
        "access_token": access_token,
        "access_token_expires_at": access_token_expires_at.timestamp(),
    }
