from datetime import datetime, timedelta

from .settings import research_cloud_settings


def get_workspace_payload(
    name: str, script_location: str, researchdrive_path: str, results_dir: str
) -> str:
    end_time = datetime.now() + timedelta(hours=12)
    backslash = "\\"

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
            }},
            {{
                "key": "webdav_base_folder_results",
                "value": "{results_dir}"
            }},
            {{
                "key": "webdav_password",
                "value": "{{{backslash}"key{backslash}":{backslash}"{research_cloud_settings.webdav_password_co_secret}{backslash}", {backslash}"sensitive{backslash}": 0}}"
            }},
            {{
                "key": "webdav_user",
                "value": "{{{backslash}"key{backslash}":{backslash}"{research_cloud_settings.webdav_user_co_secret}{backslash}", {backslash}"sensitive{backslash}": 0}}"
            }}
        ],
        "send_early_deletion_notification": false,
        "name": "{name}",
        "host_name": "{name}",
        "owner_id": "{research_cloud_settings.owner_id}",
        "meta": {{
            "application_offering_id": "47beca8d-9ed8-4540-a782-e327a3bb7931",
            "application_name": "Tools2Data RDX V2",
            "application_type": "Compute",
            "subscription_tag": "rsc-live_f21ee29a-6301-4d68-b22c-0d2ec3bde491",
            "subscription_name": "SURF HPC Cloud",
            "subscription_group_id": "e6ea9ca7-e833-4150-8dfd-1dcd6c49f75c",
            "co_name": "{research_cloud_settings.co_name}",
            "host_name": "{name}",
            "subscription_resource_type": "VM",
            "flavours": [
                {{
                    "id": "c2a138f6-d3c1-4eaa-84ba-a35432f2f133",
                    "tags": [],
                    "description": "Ubuntu 20.04",
                    "subtitle": "",
                    "support_url": "",
                    "created_at": "2022-10-05T12:59:45.264340Z",
                    "modified_at": "2023-04-14T07:09:28.613217Z",
                    "status": "active",
                    "name": "Ubuntu 20.04",
                    "category": "os",
                    "accounting_products": []
                }},
                {{
                    "id": "0f71f7ab-da03-4c98-9074-a03d84b6c235",
                    "tags": [
                        {{
                            "id": 368,
                            "key": "Cost EINF (cpu-hrs/day)",
                            "value": "24",
                            "is_public": true
                        }},
                        {{
                            "id": 526,
                            "key": "Cost RCCS (credits/day)",
                            "value": "25.2",
                            "is_public": true
                        }},
                        {{
                            "id": 367,
                            "key": "CPU",
                            "value": "1",
                            "is_public": true
                        }},
                        {{
                            "id": 369,
                            "key": "RAM [GB]",
                            "value": "8",
                            "is_public": true
                        }},
                        {{
                            "id": 370,
                            "key": "storage [GB]",
                            "value": "20",
                            "is_public": true
                        }}
                    ],
                    "description": "Small VM - 1 core - 8 GB RAM",
                    "subtitle": "",
                    "support_url": "",
                    "created_at": "2022-11-01T15:07:49.538241Z",
                    "modified_at": "2023-04-14T11:46:41.583148Z",
                    "status": "active",
                    "name": "1 core - 8 GB RAM",
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
                }},
                {{
                    "key": "webdav_base_folder_results",
                    "value": "{results_dir}"
                }},
                {{
                    "key": "webdav_password",
                    "value": "{{{backslash}"key{backslash}":{backslash}"{research_cloud_settings.webdav_password_co_secret}{backslash}", {backslash}"sensitive{backslash}": 0}}"
                }},
                {{
                    "key": "webdav_user",
                    "value": "{{{backslash}"key{backslash}":{backslash}"{research_cloud_settings.webdav_user_co_secret}{backslash}", {backslash}"sensitive{backslash}": 0}}"
                }}
            ],
            "wallet_name": "{research_cloud_settings.wallet_name}",
            "wallet_id": "{research_cloud_settings.wallet_id}"
        }},
        "end_time": "{end_time.strftime('%Y-%m-%dT%H:%M:%SZ')}"
    }}
    """
