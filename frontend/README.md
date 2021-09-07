# frontend

## Development

### Install tools

- `brew install nvm`
- `nvm install lts`

### VScode

- Install [Prettier - Code formatter](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode) extension;

### Running the app

There are some environment variables required for startup. For local development use `.env.local` (_NOTE_ this file shouldn't be commited to git) to override `RDX_BACKEND_URL` f.e. :

```
RDX_BACKEND_URL="http://localhost:8081"
```

This way you don't need to set any variables manually. For other non-secret environment variables use `.env`.

[Read More on this topic>>](https://frontend-digest.com/environment-variables-in-next-js-9a272f0bf655)

### Requirements

- NodeJS 14.x LTS

### Troubleshooting

**ERR! sharp Prebuilt libvips 8.10.0 binaries are not yet available for darwin-arm64v8**
Npm install fails on M1 Macbook: [check this tip](https://stackoverflow.com/a/67566332)

**npm install still fails**
Check that you are using NodeJS 14.

### Build and Run

Build locally using `npm run build` and then start the frontend service with `npm run start` or `npm run dev` (for dev mode)

Build image: `docker build -t rdx-frontend .`.

Run in Docker locally, without docker-compose: `docker run -p 3000:3000 -e RDX_BACKEND_URL="http://docker.for.mac.localhost:8081" rdx-frontend`
