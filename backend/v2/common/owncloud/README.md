# Owncloud

The owncloud library implements an ownCloud client that can be used in other components to interact with ownCloud.
It uses the credentials and settings specified in the `.env` file.

## Packages used

[pyocclient](https://github.com/owncloud/pyocclient)

## Example

```{python}
with OwnCloudClient() as oc_client:

    # Create a public link for a file or directory in ownCloud
    public_link = oc_client.make_public_link("/Some Folder/words.txt")
    print(public_link)

    # Delete share with ShareID 42
    result = oc_client.delete_share(42)
    print("result", result)

    # Download the conditions.pdf from a directory in ownCloud to a local directory
    local_download_location = oc_client.download_conditions(
        "/Some Folder in ownCloud", "/tmp/dir"
    )
    print("download", local_download_location)

    # Get shares from ownCloud
    shares = oc_client.get_shares()
    for share in shares:
        print(share)

    # Get all the shared directories from ownCloud
    dirs = oc_client.get_shared_dirs()
    for dir in dirs:
        # List contents of a directory
        print(oc_client.list_dir_contents(dir))
        # Check for the existence of a specific file in a directory
        print(dir, oc_client.dir_has_file(dir, "conditions.pdf"))

```
