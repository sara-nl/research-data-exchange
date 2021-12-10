# Running RDX on your machine

This document describes how to run RDX locally for development purposes. Please note that because we want to keep the development setup as lightweight as possible there are some differences between this setup and the setup used for production or demo environments.

There are, however, quite a few things that you'll need for starting up RDX locally.

## 1. Basics

- Check and satisfy [RDX Frontend Requirements](../../frontend/README.md#software-requirements);
- Check and satisfy [RDX Backend Requirements](../../backend/README.md#software-requirements).

## 2. Docker

- Install Docker (tested with v20.10.11) and Docker Compose (tested with v1.29.2).

## 3. Database

You'll need a PostrgreSQL database and Docker Compose will install and start it for you. When it's there, you can access is with `psql` by running:

```
sudo docker exec -it rdx_postgres_1 psql -U postgres
```

## 4. SMTP server

For the sake of simplicity, you can use mailtrap (via mailtrap.io or docker container).

You can skip this step, but then you should also configure the SMTP server.

## 5. Research Drive (Owncloud) WebDav access

Make sure that you have it and specify it in the environment variables.

## 6. Environment variables

Ensure these environment variables are configured and are visible to `docker-compose`. Read about [Environment variables in Compose](https://docs.docker.com/compose/environment-variables/).

```
RDX_PDF_HEADERS_PROXY_URL=https://rdx.lab.surf.nl/conditions
RDX_SMTP_HOST=smtp.mailtrap.io
RDX_SMTP_PORT=2525
RDX_SMTP_USER="<e.g. make a free account on mailtrap.io>"
RDX_SMTP_PASSWORD="<e.g. make a free account on mailtrap.io>"
RDX_WEB_URL="http://localhost:3000"
RDX_WEBDAV_PASSWORD="<configure in Research Drive>"
RDX_WEBDAV_USER="<configure in Research Drive>"
```

## 7. Build Backend and Frontend Docker Images

```
$ cd backend && sbt assembly && docker build . -t rdx-backend:latest
$ cd ../frontend
$ docker build --build-arg=RDX_PDF_HEADERS_PROXY_URL=https://rdx.lab.surf.nl/conditions \
--build-arg=RDX_BACKEND_URL=http://librarian:8081 -t rdx-frontend:latest .
```

## 8. Run Docker Compose

```
$ cd .. && docker-compose up --remove-orphans
```

☝️ Eventually add `--env-file .env.local` if you chose to store environment variables in a file and its not picked up by Docker Compose automatically.

⚠️ You should not see any warnings about environment variables!

✔︎ At the end of this, you should be able to run `docker ps` and see your RDX services happily running.

## 10. Test that it works

Open http://localhost:3000
