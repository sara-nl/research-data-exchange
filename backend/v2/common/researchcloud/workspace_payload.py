from datetime import datetime, timedelta

from .settings import research_cloud_settings


def get_workspace_payload(name: str, script_location: str, researchdrive_path) -> str:
    end_time = datetime.now() + timedelta(hours=12)

    return f"""
    {{
        "co_id": "{research_cloud_settings.co_id}",
        "wallet_id": "{research_cloud_settings.wallet_id}",
        "interactive_parameters": [
            {{
                "key": "script_folder_name",
                "value": "{script_location}"
            }},
            {{
                "key": "webdav_base_folder_dataset",
                "value": "{researchdrive_path}"
            }}
        ],
        "send_early_deletion_notification": false,
        "name": "{name}",
        "host_name": "{name}",
        "owner_id": "{research_cloud_settings.owner_id}",
        "meta": {{
            "application_offering_id": "0d7499d1-3085-4379-9447-23c3f091aef7",
            "application_name": "Tools2Data RDX",
            "application_type": "Compute",
            "subscription_tag": "rsc-live_f21ee29a-6301-4d68-b22c-0d2ec3bde491",
            "subscription_group_id": "e6ea9ca7-e833-4150-8dfd-1dcd6c49f75c",
            "co_name": "{research_cloud_settings.co_name}",
            "host_name": "tools2data2",
            "subscription_resource_type": "VM",
            "flavours": [
                {{
                    "id": "c2a138f6-d3c1-4eaa-84ba-a35432f2f133",
                    "tags": [],
                    "description": "Ubuntu 20",
                    "subtitle": "",
                    "support_url": "",
                    "created_at": "2022-10-05T12:59:45.264340Z",
                    "modified_at": "2022-11-02T10:19:34.636058Z",
                    "status": "active",
                    "name": "Ubuntu 20",
                    "category": "os",
                    "accounting_products": []
                }},
                {{
                    "id": "089d2661-c91b-48d7-b0d5-c6f8f988adfc",
                    "tags": [
                        {{
                            "id": 246,
                            "key": "CPU",
                            "value": "1",
                            "is_public": true
                        }},
                        {{
                            "id": 247,
                            "key": "credits/day",
                            "value": "24",
                            "is_public": true
                        }},
                        {{
                            "id": 248,
                            "key": "RAM [GB]",
                            "value": "8",
                            "is_public": true
                        }},
                        {{
                            "id": 249,
                            "key": "storage [GB]",
                            "value": "20",
                            "is_public": true
                        }},
                        {{
                            "id": 250,
                            "key": "storage type boot",
                            "value": "CEPH nvme",
                            "is_public": true
                        }}
                    ],
                    "description": "1 core - 8 GB",
                    "subtitle": ".",
                    "support_url": "",
                    "created_at": "2022-09-29T07:33:15.906477Z",
                    "modified_at": "2022-09-29T07:33:15.930991Z",
                    "status": "disabled",
                    "name": "1 core - 8 GB",
                    "category": "size",
                    "accounting_products": []
                }}
            ],
            "storages": [],
            "ips": [],
            "networks": [],
            "dataset_names": [],
            "dataset_ids": [],
            "interactive_parameters": [
                {{
                    "key": "script_folder_name",
                    "value": "{script_location}"
                }},
                {{
                    "key": "webdav_base_folder_dataset",
                    "value": "{researchdrive_path}"
                }}
            ],
            "wallet_name": "{research_cloud_settings.wallet_name}",
            "wallet_id": "{research_cloud_settings.wallet_id}"
        }},
        "end_time": "{end_time.strftime('%Y-%m-%dT%H:%M:%SZ')}"
    }}
    """
