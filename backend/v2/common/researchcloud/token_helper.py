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
        "https://proxy.sram.surf.nl/saml2sp/disco?entityID=https://login.eduid.nl",
        cookies=COOKIES,
    )

    for r in r2.history:
        if "engine.surfconext.nl" in r.url:
            SURF_CONEXT_SERVER_ID = r.cookies.get("HTTPSERVERID")
            parsed_url = urllib.parse.urlparse(r.url)
            SRAM_RELAY_STATE = urllib.parse.parse_qs(parsed_url.query)["RelayState"][0]
        COOKIES.update(r.cookies.get_dict())

    request_id = r2.url.split("login/")[1]

    data = {"email": "rdx@surf.nl"}
    r3 = requests.post(
        "https://login.eduid.nl/myconext/api/idp/service/email",
        json=data,
        headers={"referer": r2.url},
        cookies=COOKIES,
    )

    COOKIES.update(r3.cookies.get_dict())
    COOKIES["username"] = "rdx@surf.nl"
    COOKIES["login_preference"] = "usePassword"
    COOKIES["REMEMBER_ME_QUESTION_ASKED_COOKIE"] = "true"
    COOKIES["BROWSER_SESSION"] = "true"

    data = {
        "user": {
            "email": research_cloud_settings.user,
            "password": research_cloud_settings.password,
        },
        "authenticationRequestId": request_id,
        "usePassword": "true",
    }
    r4 = requests.put(
        "https://login.eduid.nl/myconext/api/idp/magic_link_request",
        headers={
            "content-type": "application/json",
            "referer": f"https://login.eduid.nl/usepassword/{request_id}",
        },
        json=data,
        cookies=COOKIES,
    )

    body = json.loads(r4.content)
    magic_url = body["url"]
    magic_url_reference = magic_url.split("?")[1]

    r5 = requests.get(
        f"{magic_url}&force=true",
        headers={
            "content-type": "application/json",
            "referer": f"https://login.eduid.nl/remember?{magic_url_reference}&redirect=https%3A%2F%2Flogin.eduid.nl%2Fsaml%2Fguest-idp%2Fmagic",
        },
        cookies=COOKIES,
    )
    r5_saml_response = BeautifulSoup(r5.content, features="html.parser").find(
        "input", {"name": "SAMLResponse"}
    )["value"]
    data = {"SAMLResponse": r5_saml_response}
    COOKIES["HTTPSERVERID"] = SURF_CONEXT_SERVER_ID

    r6 = requests.post(
        "https://engine.surfconext.nl/authentication/sp/consume-assertion",
        headers={"content-type": "application/x-www-form-urlencoded"},
        data=data,
        cookies=COOKIES,
    )
    r6_saml_response = BeautifulSoup(r6.content, features="html.parser").find(
        "input", {"name": "SAMLResponse"}
    )["value"]
    data = {
        "SAMLResponse": r6_saml_response,
        "RelayState": SRAM_RELAY_STATE,
    }

    r7 = requests.post(
        "https://proxy.sram.surf.nl/saml2sp/acs/post", data=data, cookies=COOKIES
    )
    r7_saml_response = BeautifulSoup(r7.content, features="html.parser").find(
        "input", {"name": "SAMLResponse"}
    )["value"]
    r7_relay_state = BeautifulSoup(r7.content, features="html.parser").find(
        "input", {"name": "RelayState"}
    )["value"]
    data = {"SAMLResponse": r7_saml_response, "RelayState:": r7_relay_state}

    r8 = requests.post(
        "https://oauth2.live.surfresearchcloud.nl/saml/acs/",
        headers={"content-type": "application/x-www-form-urlencoded"},
        data=data,
        allow_redirects=False,
    )
    RSC_COOKIES = r8.cookies.get_dict()

    r9 = requests.get(
        "https://oauth2.live.surfresearchcloud.nl/oauth2/",
        params=params1,
        cookies=RSC_COOKIES,
    )
    normalized_url = r9.url.replace(
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
