# ResearchCloud

TODO

## Packages used

TODO

## Example

```{python}
from common.researchcloud.researchcloud_client import ResearchCloudClient

rsc_client = ResearchCloudClient()

# Create workspace
workspace_id = rsc_client.create_workspace('test_rsc_library', "https://github.com/example/example.git", "https://researchdrive.surfsara.nl/remote.php/webdav/example")

# Get
status = rsc_client.get_workspace_status(workspace_id)
print(workspace_id, status)

# Delete workspace after it is done creating
while status == "creating":
    status = rsc_client.get_workspace_status(workspace_id)
    print(workspace_id, status)

rsc_client.delete_workspace(workspace_id)

```
