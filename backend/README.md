# RDX Backend

## Requirements

* Java 16
* SBT 1.5+

## How do I as developer ...

* Build jars `sbt assembly`
* Build Docker image `docker build . -t backend:latest`
* Start Docker container (see `../deploy/README.md`)
* Add DB migrations (check `db/README.md`)