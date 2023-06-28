import json
import os
import urllib
from datetime import datetime, timedelta

import requests
from bs4 import BeautifulSoup

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
    params1 = {
        "client_id": "d8f8cd9109864babb2179c876801bcad",
        "redirect_uri": "https://portal.live.surfresearchcloud.nl/",
        "response_type": "token",
        "scope": "openid",
        "state": "ae3293c121174b59bf03ff2b2a5a6643",
    }
    r1 = requests.get(
        "https://oauth2.live.surfresearchcloud.nl/oauth2/", params=params1
    )

    COOKIES = r1.cookies.get_dict()
    for r in r1.history:
        COOKIES.update(r.cookies.get_dict())

    r2 = requests.get(
        "https://proxy.sram.surf.nl/saml2sp/disco?entityID=https://test-idp.sram.surf.nl/saml/saml2/idp/metadata.php",
        cookies=COOKIES,
    )

    for r in r2.history:
        if "test-idp.sram.surf.nl/saml/module.php" in r.url:
            parsed_url = urllib.parse.urlparse(r.url)
            SRAM_RELAY_STATE = urllib.parse.parse_qs(parsed_url.query)["RelayState"][0]
        COOKIES.update(r.cookies.get_dict())

    COOKIES.update(r2.cookies.get_dict())

    r3 = requests.get(r2.url, cookies=COOKIES)
    parsed_url = urllib.parse.urlparse(r3.url)
    auth_state = urllib.parse.parse_qs(parsed_url.query)["AuthState"][0]

    COOKIES.update(r3.cookies.get_dict())

    params = {"AuthState": auth_state}
    data = {
        "username": research_cloud_settings.user,
        "password": research_cloud_settings.password,
    }
    r4 = requests.post(
        "https://test-idp.sram.surf.nl/saml/module.php/core/loginuserpass",
        headers={
            "content-type": "application/x-www-form-urlencoded",
        },
        data=data,
        cookies=COOKIES,
        params=params,
    )

    r4_saml_response = BeautifulSoup(r4.content, features="html.parser").find(
        "input", {"name": "SAMLResponse"}
    )["value"]

    data = {
        "SAMLResponse": r4_saml_response,
        "RelayState": SRAM_RELAY_STATE,
    }

    r5 = requests.post(
        "https://proxy.sram.surf.nl/saml2sp/acs/post", data=data, cookies=COOKIES
    )
    r5_saml_response = BeautifulSoup(r5.content, features="html.parser").find(
        "input", {"name": "SAMLResponse"}
    )["value"]
    r5_relay_state = BeautifulSoup(r5.content, features="html.parser").find(
        "input", {"name": "RelayState"}
    )["value"]
    data = {"SAMLResponse": r5_saml_response, "RelayState:": r5_relay_state}

    r6 = requests.post(
        "https://oauth2.live.surfresearchcloud.nl/saml/acs/",
        headers={"content-type": "application/x-www-form-urlencoded"},
        data=data,
        allow_redirects=False,
    )
    RSC_COOKIES = r6.cookies.get_dict()

    r7 = requests.get(
        "https://oauth2.live.surfresearchcloud.nl/oauth2/",
        params=params1,
        cookies=RSC_COOKIES,
    )
    normalized_url = r7.url.replace(
        "portal.live.surfresearchcloud.nl/#", "portal.live.surfresearchcloud.nl/?"
    )
    parsed_rsc_url = urllib.parse.urlparse(normalized_url)
    access_token = urllib.parse.parse_qs(parsed_rsc_url.query)["access_token"][0]
    access_token_expires_in = urllib.parse.parse_qs(parsed_rsc_url.query)["expires_in"][
        0
    ]
    access_token_expires_at = datetime.now() + timedelta(
        seconds=int(access_token_expires_in)
    )

    return {
        "access_token": access_token,
        "access_token_expires_at": access_token_expires_at.timestamp(),
    }
