FROM python:3.10.8-bullseye as base

# Setup env
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONFAULTHANDLER 1


FROM base AS python-deps

RUN \
 apt-get update && \
 apt-get install postgresql -y && \
 apt-get install gcc musl-dev -y

RUN pip install pipenv

WORKDIR /app

# Install python dependencies in /.venv
COPY Pipfile .
COPY Pipfile.lock .
ARG PIPENV_DEPLOYMENT=deploy
# Build with PIPENV_DEPLOYMENT=dev to install dev packages
RUN PIPENV_VENV_IN_PROJECT=1 pipenv install --${PIPENV_DEPLOYMENT}

COPY . .
# Install common library
RUN python setup.py sdist && pipenv install dist/common-0.1.0.tar.gz

RUN chmod +x entrypoint.sh

CMD [ "./entrypoint.sh" ]
