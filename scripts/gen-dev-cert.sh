#!/bin/bash
# Gera certificado auto-assinado para desenvolvimento local
# Uso: ./scripts/gen-dev-cert.sh

set -euo pipefail

KEYSTORE_DIR="src/main/resources/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/ford-challenge-dev.p12"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"

echo ">>> Gerando keystore auto-assinada para desenvolvimento..."
mkdir -p "$KEYSTORE_DIR"

if [ -f "$KEYSTORE_FILE" ]; then
    echo "Keystore ja existe em $KEYSTORE_FILE"
    echo "Remova o arquivo e execute novamente para regenerar."
    exit 0
fi

keytool -genkeypair \
    -alias ford-challenge-dev \
    -keyalg RSA \
    -keysize 2048 \
    -validity 365 \
    -keystore "$KEYSTORE_FILE" \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -dname "CN=localhost, OU=FordChallenge, O=FIAP, L=Sao Paulo, ST=SP, C=BR" \
    -ext "SAN=DNS:localhost,IP:127.0.0.1"

echo ""
echo ">>> Keystore gerada com sucesso:"
echo "    Arquivo: $KEYSTORE_FILE"
echo ""
echo "Para iniciar a aplicacao com HTTPS:"
echo "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=ssl"
echo ""
echo "Para testar:"
echo "  curl -k https://localhost:8443/api/v1/health"
