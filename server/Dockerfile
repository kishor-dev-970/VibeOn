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
      curl \
      unzip \
    && pip3 install --break-system-packages --no-cache-dir --prefer-binary yt-dlp \
    && rm -rf /var/lib/apt/lists/*

# YouTube Proof-of-Origin provider (bgutil-pot) + yt-dlp plugin, to survive
# the "Sign in to confirm you're not a bot" wall on datacenter IPs.
# Auto-discovered by yt-dlp from ~/.config/yt-dlp/plugins (HOME=/root).
RUN curl -fsSL -o /usr/local/bin/bgutil-pot \
      https://github.com/jim60105/bgutil-ytdlp-pot-provider-rs/releases/latest/download/bgutil-pot-linux-x86_64 \
    && chmod +x /usr/local/bin/bgutil-pot \
    && mkdir -p /root/.config/yt-dlp/plugins/bgutil-ytdlp-pot-provider \
    && curl -fsSL -o /tmp/bgutil-pot-plugin.zip \
      https://github.com/jim60105/bgutil-ytdlp-pot-provider-rs/releases/latest/download/bgutil-ytdlp-pot-provider-rs.zip \
    && unzip -q /tmp/bgutil-pot-plugin.zip -d /root/.config/yt-dlp/plugins/bgutil-ytdlp-pot-provider \
    && rm /tmp/bgutil-pot-plugin.zip

WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
ENV PYTHON_PATH=python3
ENV FFMPEG_PATH=/usr/bin
ENV VIDEO_CACHE_DIR=/var/cache/social-music
ENV POT_SERVER_HOST=127.0.0.1
ENV POT_SERVER_PORT=4416
ENV TOKEN_TTL=6

COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

RUN mkdir -p /var/cache/social-music && chmod -R a+rw /var/cache/social-music

EXPOSE 3000
# Start the bgutil POT sidecar (proves origin to YouTube, bypassing the
# datacenter "Sign in to confirm you're not a bot" wall), wait up to 15s for
# its /ping, then run the Next server. yt-dlp auto-discovers it on 127.0.0.1:4416.
CMD ["sh", "-c", "bgutil-pot server --host 127.0.0.1 --port 4416 > /tmp/bgutil-pot.log 2>&1 & i=0; until curl -fsS http://127.0.0.1:4416/ping >/dev/null 2>&1 || [ $i -ge 15 ]; do sleep 1; i=$((i+1)); done; curl -fsS http://127.0.0.1:4416/ping && echo 'bgutil-pot ready' >> /tmp/bgutil-pot.log 2>&1; exec node server.js"]