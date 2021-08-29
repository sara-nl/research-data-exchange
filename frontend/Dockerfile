# Install dependencies only when needed
FROM node:14-alpine AS deps
ENV NEXT_TELEMETRY_DISABLED 1
# Check https://github.com/nodejs/docker-node/tree/b4117f9333da4138b03a546ec926ef50a31506c3#nodealpine to understand why libc6-compat might be needed.
RUN apk --no-cache --virtual build-dependencies add \
    python \
    make \
    g++
RUN apk add --update --no-cache libc6-compat 
WORKDIR .
COPY package.json package-lock.json ./
RUN npm install --frozen-lockfile

# Rebuild the source code only when needed
FROM node:14-alpine AS builder
ENV NEXT_TELEMETRY_DISABLED 1
# This env variable will be overriden when starting app
ENV RDX_BACKEND_URL=http://noop
WORKDIR .
COPY . .
COPY --from=deps node_modules ./node_modules
RUN npm install --verbose sharp
RUN npm run build


# Production image, copy all the files and run next
FROM node:14-alpine AS runner
ENV NEXT_TELEMETRY_DISABLED 1
WORKDIR .

ENV NODE_ENV production

RUN addgroup -g 1001 -S nodejs
RUN adduser -S nextjs -u 1001

COPY --from=builder next.config.js ./
COPY --from=builder public ./public
COPY --from=builder --chown=nextjs:nodejs .next ./.next
COPY --from=builder node_modules ./node_modules
COPY --from=builder package.json ./package.json

USER nextjs

EXPOSE 3000

CMD ["npm", "start"]
