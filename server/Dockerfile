# syntax=docker/dockerfile:1

# ---------- deps ----------
# Uses `npm install` (not `npm ci`): npm ci hard-fails on any lockfile/platform
# skew (e.g. optional @emnapi bindings), while install self-heals the graph in-image.
FROM node:24-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install --no-audit --no-fund

# ---------- build ----------
FROM node:24-slim AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# ---------- runner ----------
FROM node:24-slim AS runner
RUN apt-get update && apt-get install -y --no-install-recommends \
      ffmpeg \
      python3 \
      python3-pip \
      ca-certificates \
    && pip3 install --break-system-packages --no-cache-dir yt-dlp \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
ENV PYTHON_PATH=python3
ENV FFMPEG_PATH=/usr/bin
ENV VIDEO_CACHE_DIR=/var/cache/social-music

COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

RUN mkdir -p /var/cache/social-music && chmod -R a+rw /var/cache/social-music

EXPOSE 3000
CMD ["node", "server.js"]