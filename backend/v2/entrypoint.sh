#!/bin/bash -e

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
elif [[ ${APPLICATION} == "api" ]]; then
    echo "Starting api..."
    pipenv run uvicorn --host 0.0.0.0 api.main:app
    exit 0
else
    echo "Unknown APPLICATION environment variable... exiting"
    exit 1
fi
