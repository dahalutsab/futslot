# The player app, owner dashboard and admin, served by Caddy behind the edge server. Built from the repo root.
FROM node:26-alpine AS build
WORKDIR /src
COPY player/package.json player/package-lock.json player/
COPY dashboard/package.json dashboard/package-lock.json dashboard/
COPY admin/package.json admin/package-lock.json admin/
RUN cd player && npm ci && cd ../dashboard && npm ci && cd ../admin && npm ci
COPY player player
COPY dashboard dashboard
COPY admin admin
RUN cd player && npx ng build && cd ../dashboard && npx ng build && cd ../admin && npx ng build

FROM caddy:2.10-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /src/player/dist/player/browser /srv/player
COPY --from=build /src/dashboard/dist/dashboard/browser /srv/dashboard
COPY --from=build /src/admin/dist/admin/browser /srv/admin
