# frontend

## Development

### Install tools

- `brew install nvm`
- `nvm install lts`

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
