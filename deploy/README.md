# How to deploy

See: [.gitlab-ci.yml](../.gitlab-ci.yml)

# How to run development server

- Install `docker` locally.

```
$ cd backend && sbt assembly && docker build . -t backend:latest
$ echo .env > "<environment variables and their values>"
$ cd deploy && docker-compose up --remove-orphans
```

### Environment variables

```
DOCKER_IMAGE_TAG = "latest"
RDX_SMTP_HOST = "smtp.mailtrap.io"
RDX_SMTP_PORT = 2525
RDX_SMTP_USER = "<make a free account on mailtrap.io>"
RDX_SMTP_PASSWORD = "<make a free account on mailtrap.io>"
RDX_WEB_URL="http://google.com"
RDX_WEBDAV_PASSWORD = "<ask someone>"
RDX_WEBDAV_USER = "<ask someone>"
DB_USER = "postgres"
```
