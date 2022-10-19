import time

from fastapi import FastAPI
from pipeline import discovery, synchronize, users

from common.db.db_client import DBClient

app = FastAPI()

db = DBClient()


@app.on_event("startup")
def start_sharer():
    print("Starting sharer")
    interval = 30
    while True:
        found_changes = run_sharer()
        interval = determine_interval(interval, found_changes)
        print(f"Found changes: {found_changes}, current time interval: {interval}")
        time.sleep(interval)


def run_sharer():
    eligible_shared_dirs = discovery.get_eligible_shared_dirs()
    new_shares, deleted_shares = discovery.compare_shared_dirs_with_stored_dirs(
        db, eligible_shared_dirs
    )
    print(
        f"Found {len(new_shares)} new share(s) and {len(deleted_shares)} deleted share(s)"
    )
    new_shares = users.create_users(db, new_shares)
    synchronize.add_new_shares(db, new_shares)
    synchronize.remove_deleted_shares(db, deleted_shares)

    return len(new_shares) or len(deleted_shares)


def determine_interval(current_interval: int, found_changes: bool) -> int:
    if not found_changes:
        # Return double the current interval or 5 minutes, whichever is lower
        return min(current_interval * 2, 5 * 60)
    return 30


if __name__ == "__main__":
    start_sharer()
