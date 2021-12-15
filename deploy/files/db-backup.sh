#!/bin/sh

BASEDIR=$(dirname "$0")
docker exec -it rdx_postgres_1 pg_dump -U postgres | gzip > "$BASEDIR"/db-backup-"`date +%d-%m-%Y_%H:%M:%S`".sql.gz