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
    shared_dirs = discovery.get_eligible_shared_dirs(db)
    (
        eligible_shares,
        ineligble_shared_dirs,
        deleted_shares,
    ) = discovery.compare_shared_dirs_with_stored_dirs(db, shared_dirs)
    print(
        f"Found {len(eligible_shares)} new share(s), {len(ineligble_shared_dirs)} ineligible shares, and {len(deleted_shares)} deleted share(s)"
    )
    eligible_shares = users.create_users(db, eligible_shares)
    synchronize.update_eligible_shares(db, eligible_shares)
    synchronize.remove_deleted_shares(db, deleted_shares)
    users.notify(db, eligible_shares)
    users.notify(db, ineligble_shared_dirs)
    return len(eligible_shares) or len(deleted_shares)


def determine_interval(current_interval: int, found_changes: bool) -> int:
    if not found_changes:
        # Return double the current interval or 5 minutes, whichever is lower
        return min(current_interval * 2, 5 * 60)
    return 30


if __name__ == "__main__":
    start_sharer()
