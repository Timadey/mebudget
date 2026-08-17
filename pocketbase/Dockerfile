# MeBudget PocketBase image.
#
# Downloads the official PocketBase binary and bakes in this repository's
# pb_migrations + pb_hooks. Coolify deploys Docker images, so the webhook
# (pb_hooks/paystack.pb.js) and schema migrations are compiled into the image
# rather than existing on the host filesystem.
#
#   Version:     override with --build-arg PB_VERSION=x.y.z (default latest 0.39.x)
#   Port:        8090 (also update in Coolify's network settings)
#   Data volume: mount a persistent volume at /pb/pb_data

FROM alpine:latest

ARG TARGETARCH
ARG PB_VERSION=0.39.11

RUN apk add --no-cache \
    unzip \
    ca-certificates

# Download and unzip the PocketBase binary (TARGETARCH = amd64/arm64/arm).
ADD https://github.com/pocketbase/pocketbase/releases/download/v${PB_VERSION}/pocketbase_${PB_VERSION}_linux_${TARGETARCH}.zip /tmp/pb.zip
RUN unzip /tmp/pb.zip -d /pb/ && rm /tmp/pb.zip

# Bake in our migrations and hooks (loaded automatically at serve time).
COPY pb_migrations /pb/pb_migrations
COPY pb_hooks /pb/pb_hooks

EXPOSE 8090

# Data must persist across redeploys: mount a Coolify volume at /pb/pb_data.
VOLUME ["/pb/pb_data"]

CMD ["/pb/pocketbase", "serve", "--http=0.0.0.0:8090"]