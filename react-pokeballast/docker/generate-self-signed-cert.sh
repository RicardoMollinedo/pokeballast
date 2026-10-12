#!/bin/sh
# Runs before nginx starts (official image: /docker-entrypoint.d). Creates a self-signed certificate for
# local HTTPS only when no certificate was mounted. Keys are never stored in the repository or the image.
set -eu
CERT_DIR=/etc/nginx/certs
if [ -f "$CERT_DIR/tls.crt" ] && [ -f "$CERT_DIR/tls.key" ]; then
    echo "TLS certificate found in $CERT_DIR, using it"
    exit 0
fi
mkdir -p "$CERT_DIR"
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
    -keyout "$CERT_DIR/tls.key" -out "$CERT_DIR/tls.crt" 2>/dev/null
chmod 600 "$CERT_DIR/tls.key"
echo "Generated a self-signed TLS certificate for localhost (development only)"
