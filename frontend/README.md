# frontend

## Development

### Requirements

- NodeJS 14.x LTS

### Install tools

- `brew install nvm`
- `nvm install --lts=fermium`

### Build and Run

There are some environment variables required for startup. For local development use `.env.local` (_NOTE_ this file shouldn't be commited to git) to override `RDX_BACKEND_URL` and `RDX_PDF_HEADERS_PROXY_URL` f.e. :

```
RDX_BACKEND_URL=http://localhost:8081
RDX_PDF_HEADERS_PROXY_URL=http://some.rdx.host/conditions
```

This way you don't need to set any variables manually. For other non-secret environment variables use `.env`.

[Read More on this topic>>](https://frontend-digest.com/environment-variables-in-next-js-9a272f0bf655)

#### Run with NPM

Build locally using `npm run build` and then start the frontend service with `npm run start` or `npm run dev` (for dev mode)

#### Run with Docker

Build image: `docker build -t rdx-frontend .`.

Run in Docker locally, without docker-compose: `docker run -p 3000:3000 -e RDX_BACKEND_URL="http://docker.for.mac.localhost:8081" rdx-frontend`

### Editing source code: VScode

- Install [Prettier - Code formatter](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode) extension;
- Enable format on save;
- Ensure that Prettier is configured as the formatter for the project;

### Troubleshooting

**ERR! sharp Prebuilt libvips 8.10.0 binaries are not yet available for darwin-arm64v8**
Npm install fails on M1 Macbook: [check this tip](https://stackoverflow.com/a/67566332)

**npm install still fails**
Check that you are using NodeJS 14.

### End to end tests

We use Cypress for end to end tests to verify that our pages do what we need them to. The idea is simple: we start _only_ frontend, intercept all HTTP requests by Cypress, so that it can control, ovserve and make assertions on all inteactions between frontend and backend.

Start the server for cypress tests:

```
RDX_BACKEND_URL="http://127.0.0.1:8081" RDX_PDF_HEADERS_PROXY_URL=https://rdx-test-1.dataex-sara.surf-hosted.nl/conditions npm run start
```

Run cypress tests:

```
CYPRESS_RDX_BACKEND_URL="http://127.0.0.1:8081" npm run cypress:run
```
