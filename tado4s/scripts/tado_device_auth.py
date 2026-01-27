#!/usr/bin/env python3

import requests
import time
import sys
from datetime import datetime, timezone, timedelta

CLIENT_ID = "1bb50063-6b0c-4d11-bd99-387f4a91cc46"
DEVICE_AUTH_URL = "https://login.tado.com/oauth2/device_authorize"
TOKEN_URL = "https://login.tado.com/oauth2/token"

def initiate_device_flow():
    response = requests.post(
        DEVICE_AUTH_URL,
        data={
            "client_id": CLIENT_ID,
            "scope": "offline_access",
        }
    )
    response.raise_for_status()
    return response.json()

def poll_for_token(device_code, interval):
    while True:
        response = requests.post(
            TOKEN_URL,
            data={
                "client_id": CLIENT_ID,
                "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
                "device_code": device_code,
            }
        )

        if response.status_code == 200:
            return response.json()

        data = response.json()
        error = data.get("error", "")

        if error == "authorization_pending":
            time.sleep(interval)
            continue
        elif error == "slow_down":
            time.sleep(interval + 5)
            continue
        elif error == "expired_token":
            print("Device code expired. Please try again.")
            sys.exit(1)
        else:
            print(f"Error: {data}")
            sys.exit(1)

def main():
    print("Initiating Tado device authorization flow...\n")

    device_data = initiate_device_flow()

    verification_url = device_data.get("verification_uri_complete") or device_data["verification_uri"]
    user_code = device_data["user_code"]
    device_code = device_data["device_code"]
    interval = device_data.get("interval", 5)
    expires_in = device_data.get("expires_in", 300)

    print(f"Please visit: {verification_url}")
    print(f"And enter code: {user_code}")
    print(f"\nWaiting for authorization (expires in {expires_in} seconds)...")

    token_data = poll_for_token(device_code, interval)

    issue_time = (datetime.now(timezone.utc) - timedelta(seconds=5)).isoformat()

    print("\n" + "=" * 50)
    print("Authorization successful!")
    print("=" * 50)
    print(f"\nAdd this to your environment variables:\n")
    print(f"HOMEDATA_TADO_TOKEN=\"{token_data['refresh_token']}\"")
    print(f"HOMEDATA_TADO_TOKEN_ISSUE_TIME=\"{issue_time}\"")

if __name__ == "__main__":
    main()
