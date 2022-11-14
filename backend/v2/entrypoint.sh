#!/bin/sh -e

if [[ -z "${APPLICATION}" ]]; then
    echo "No APPLICATION environment variable set... exiting"
    exit 1
fi

if [[ ${APPLICATION} == "database-migrations" ]]; then
    echo "Running database migrations..."
    pipenv run alembic upgrade head
    exit 0
elif [[ ${APPLICATION} == "sharer" ]]; then
    echo "Starting sharer..."
    pipenv run python sharer/sharer.py
    exit 0
elif [[ ${APPLICATION} == "librarian" ]]; then
    echo "Starting librarian..."
    pipenv run uvicorn --host 0.0.0.0 librarian.librarian:app
    exit 0
elif [[ ${APPLICATION} == "linting" ]]; then
    echo "Running linting..."
    # pylint and flake8 not yet fixed
    pipenv run pylint --recursive=yes common sharer librarian || true
    pipenv run flake8 . || true
    pipenv run isort --check-only .
    pipenv run black --check .
    pipenv run bandit .
    exit 0
else
    echo "Unknown APPLICATION environment variable... exiting"
    exit 1
fi