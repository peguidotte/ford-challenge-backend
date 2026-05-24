#!/usr/bin/env bash
#
# Testes de Integracao Simples via Bash
# Uso: ./scripts/integration-test.sh [URL_BASE]
# Exemplo: ./scripts/integration-test.sh http://localhost:8080

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
TEST_SEQ=0

do_req() {
    local method="$1" path="$2"
    local data="${3:-}"
    local args=(-s -o /tmp/inttest_body -w "%{http_code}" -X "$method" \
        -H "Content-Type: application/json")
    if [ -n "$data" ]; then
        args+=(-d "$data")
    fi
    curl "${args[@]}" "${BASE_URL}${path}"
}

assert_status() {
    local method="$1" path="$2" expected="$3" desc="$4"
    local data="${5:-}"
    TEST_SEQ=$((TEST_SEQ + 1))

    local status
    status=$(do_req "$method" "$path" "$data")

    if [ "$status" = "$expected" ]; then
        echo "  OK  [$TEST_SEQ] $desc  (HTTP $status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL [$TEST_SEQ] $desc  (esperado $expected, obtido $status)"
        echo "       Response: $(head -c 500 /tmp/inttest_body)"
        FAIL=$((FAIL + 1))
    fi
}

assert_json() {
    local method="$1" path="$2" desc="$3"
    local jq_filter="$4" expected_value="$5"
    local data="${6:-}"
    TEST_SEQ=$((TEST_SEQ + 1))

    local status body
    body=$(do_req "$method" "$path" "$data")
    status=$body  # do_req outputs status code only
    body=$(cat /tmp/inttest_body)

    local actual
    actual=$(echo "$body" | jq -r "$jq_filter" 2>/dev/null || echo "__JQ_ERROR__")

    if [ "$actual" = "$expected_value" ]; then
        echo "  OK  [$TEST_SEQ] $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL [$TEST_SEQ] $desc  (esperado '$expected_value', obtido '$actual')"
        echo "       HTTP $status | Body: $(echo "$body" | head -c 300)"
        FAIL=$((FAIL + 1))
    fi
}

echo "=========================================="
echo "  Ford Challenge - Testes de Integracao"
echo "  URL: $BASE_URL"
echo "=========================================="
echo ""

# ─── Vehicle Specs Query ────────────────────────────────────────────────────
echo ""
echo ">>> VehicleSpecs Query"

VALID_PAYLOAD='{
    "brand": "Ford",
    "model": "Ranger",
    "version": "Raptor",
    "vehicleType": "cars",
    "attributes": ["Motor", "Potencia", "Torque", "Preco de referencia", "Modos de conducao"]
}'

assert_status POST /api/v1/vehicle-specs/query 200 \
    "POST /api/v1/vehicle-specs/query (valido)" "$VALID_PAYLOAD"

assert_json POST /api/v1/vehicle-specs/query \
    "query retorna brand=Ford" ".brand" "Ford" "$VALID_PAYLOAD"

assert_json POST /api/v1/vehicle-specs/query \
    "query retorna model=Ranger" ".model" "Ranger" "$VALID_PAYLOAD"

assert_json POST /api/v1/vehicle-specs/query \
    "query retorna version=Raptor" ".version" "Raptor" "$VALID_PAYLOAD"

assert_json POST /api/v1/vehicle-specs/query \
    "query retorna attributes.Motor preenchido" \
    ".attributes.Motor | length > 0" \
    "true" "$VALID_PAYLOAD"

assert_json POST /api/v1/vehicle-specs/query \
    "query retorna generatedAt nao nulo" \
    ".generatedAt != null" \
    "true" "$VALID_PAYLOAD"

# ─── Vehicle Specs Query - Atributo inexistente ────────────────────────────
echo ""
echo ">>> VehicleSpecs Query - Atributo indisponivel"

ATTR_INEXISTENTE='{
    "brand": "Ford",
    "model": "Ranger",
    "version": "Raptor",
    "attributes": ["AtributoInexistenteXYZ"]
}'

assert_json POST /api/v1/vehicle-specs/query \
    "atributo inexistente retorna 'Nao disponivel'" \
    ".attributes.AtributoInexistenteXYZ" \
    "Nao disponivel" "$ATTR_INEXISTENTE"

assert_json POST /api/v1/vehicle-specs/query \
    "atributo inexistente retorna source NOT_FOUND" \
    ".sources.AtributoInexistenteXYZ" \
    "NOT_FOUND" "$ATTR_INEXISTENTE"

# ─── Vehicle Specs Query - Validacao (400) ─────────────────────────────────
echo ""
echo ">>> Vehicle Specs Query - Erros de validacao"

assert_status POST /api/v1/vehicle-specs/query 400 \
    "POST com brand vazio retorna 400" \
    '{"brand":"","model":"Ranger","version":"Raptor","attributes":["Motor"]}'

assert_status POST /api/v1/vehicle-specs/query 400 \
    "POST com attributes vazio retorna 400" \
    '{"brand":"Ford","model":"Ranger","version":"Raptor","attributes":[]}'

# ─── FIPE Catalog ──────────────────────────────────────────────────────────
echo ""
echo ">>> FIPE Catalog"

assert_status GET "/api/v1/catalog/fipe/brands?vehicleType=cars" 200 \
    "GET /api/v1/catalog/fipe/brands"

assert_json GET "/api/v1/catalog/fipe/brands?vehicleType=cars" \
    "brands retorna array" \
    "type == \"array\"" "true"

assert_json GET "/api/v1/catalog/fipe/brands?vehicleType=cars" \
    "brands[0] contem code" \
    ".[0].code != null" "true"

assert_json GET "/api/v1/catalog/fipe/brands?vehicleType=cars" \
    "brands[0] contem name" \
    ".[0].name != null" "true"

assert_status GET "/api/v1/catalog/fipe/brands/22/models?vehicleType=cars" 200 \
    "GET /api/v1/catalog/fipe/brands/22/models"

assert_json GET "/api/v1/catalog/fipe/brands/22/models?vehicleType=cars" \
    "models retorna array" \
    "type == \"array\"" "true"

# ─── Resumo ────────────────────────────────────────────────────────────────
echo ""
echo "=========================================="
echo "  Resultado: $PASS passaram, $FAIL falharam"
echo "=========================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
