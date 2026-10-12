# ---- build stage ----
FROM node:24.21.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---- runtime stage: static files served by nginx over HTTP (80) and HTTPS (443) ----
FROM nginx:1.28-alpine
RUN apk add --no-cache openssl
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/locations.conf /etc/nginx/snippets/locations.conf
COPY docker/generate-self-signed-cert.sh /docker-entrypoint.d/40-generate-self-signed-cert.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/40-generate-self-signed-cert.sh \
    && chmod +x /docker-entrypoint.d/40-generate-self-signed-cert.sh
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
