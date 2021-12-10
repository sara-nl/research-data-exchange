# How to provision VM

See `.gitlab-ci` to know the order of applying playbooks.

Applying a playbook:

```
$ cd deploy
$ pipenv install
$ pipenv run ansible-playbook -i inventory.yml -u ubuntu <playbook_you_want_to_run>
```

# How to deploy

See: [.gitlab-ci.yml](../.gitlab-ci.yml)

# How to setup brand new server

## Pre-requisites

1. Server with Ubuntu 18.04.X:

- with fully qualified domain name
- with user `ubuntu`
- with your public ssh key in `ubuntu`'s authorised keys
- with access to `git.ia.surfsara.nl`.

2. Generated [access token](https://git.ia.surfsara.nl/SOIL/RDX/-/settings/access_tokens) for Docker registry.

3. Webdav username and password.

4. Mail server configuration.

## Steps

1. Apply playbook [`system.yml`](playbooks/system.yml) (_setups machine configuration and installs all missing packages_)

2. Setup required [Environment variables](#environment_variables) on the machine where you are running `ansible` commands
3. Modify/Create [`inventory-prod.yml`](inventory-prod.yml) add/replace `host` name. Read [here](https://docs.ansible.com/ansible/latest/user_guide/intro_inventory.html#how-to-build-your-inventory) for more information regarding inventory.

4. Apply playbook [`containers.yml`](playbooks/containers) (_downloads components images and starts all required processes_)

```
pipenv run ansible-playbook -i inventory-prod.yml -u ubuntu playbooks/containers.yml

```

5. Validate setup

- Check that docker containers started and not producing any errors. On the machine run:

```
# To check running containers
docker ps

# To check logs in case of the continuous container restarts
 docker logs --tail 50 --follow --timestamps rdx_backend_1

```

- Check if UI works

Go to `$RDX_WEB_URL` in your browser to see the index page.

### Environment variables

All these variables must be set on the machine that runs Ansible. They will be propagated to the appropriate Docker containers.

```
DOCKER_IMAGE_TAG = "latest"
DOCKER_USERNAME = "<configure in GitLab>"
DOCKER_PASSWORD = "<configure in GitLab>"
RDX_SMTP_HOST = "<e.g. smtp.mailtrap.io>"
RDX_SMTP_PORT = 2525
RDX_SMTP_USER = "<e.g. make a free account on mailtrap.io>"
RDX_SMTP_PASSWORD = "<e.g. make a free account on mailtrap.io>"
RDX_WEB_URL="https://rdx.lab.surf.nl"  # Public web URL starting with protocol
RDX_WEBDAV_PASSWORD = "<configure in Research Drive>"
RDX_WEBDAV_USER = "<configure in Research Drive>"
DB_USER = "postgres"
```
