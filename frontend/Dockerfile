FROM node:18-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build:docker

FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

RUN apk add --no-cache curl \
    && rm -rf ./* \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup \
    && mkdir -p /var/cache/nginx /var/run/nginx \
    && chown -R appuser:appgroup /usr/share/nginx/html /var/cache/nginx /var/run/nginx /etc/nginx/conf.d

COPY --from=build --chown=appuser:appgroup /app/build ./
COPY --chown=appuser:appgroup nginx.conf /etc/nginx/nginx.conf

USER appuser

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:3000/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
