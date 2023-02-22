import time

from fastapi import FastAPI
from pipeline.active_jobs import update_active_jobs
from pipeline.finished_jobs import cleanup_jobs
from pipeline.new_jobs import create_new_jobs
from pipeline.notification import notify_users

from common.db.db_client import DBClient

app = FastAPI()

db = DBClient()


@app.on_event("startup")
def start_lab_runner():
    print("Starting lab runner")
    interval = 30
    while True:
        run_runner()
        time.sleep(interval)


def run_runner():
    with db.get_session() as session:
        create_new_jobs(session)
    with db.get_session() as session:
        update_active_jobs(session)
    with db.get_session() as session:
        notify_users(session)
    with db.get_session() as session:
        cleanup_jobs(session)
    return


if __name__ == "__main__":
    start_lab_runner()
