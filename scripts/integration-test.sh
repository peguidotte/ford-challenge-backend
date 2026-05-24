#!/usr/bin/env bash
#
# Testes de Integracao via Bash
# Uso: ./scripts/integration-test.sh [URL_BASE]
# Exemplo: ./scripts/integration-test.sh http://localhost:8080
# Requer: curl, jq

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
TEST_SEQ=0

ADMIN_TOKEN=""
USER_TOKEN=""

do_req() {
    local method="$1" path="$2"
    local data="${3:-}"
    local token="${4:-}"
    local args=(-s -o /tmp/inttest_body -w "%{http_code}" -X "$method" \
        -H "Content-Type: application/json")
    if [ -n "$token" ]; then
        args+=(-H "Authorization: Bearer $token")
    fi
    if [ -n "$data" ]; then
        args+=(-d "$data")
    fi
    curl "${args[@]}" "${BASE_URL}${path}"
}

assert_status() {
    local method="$1" path="$2" expected="$3" desc="$4"
    local data="${5:-}"
    local token="${6:-}"
    TEST_SEQ=$((TEST_SEQ + 1))
    local status
    status=$(do_req "$method" "$path" "$data" "$token")
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
    local token="${7:-}"
    TEST_SEQ=$((TEST_SEQ + 1))
    do_req "$method" "$path" "$data" "$token" > /dev/null
    local body
    body=$(cat /tmp/inttest_body)
    local actual
    actual=$(echo "$body" | jq -r "$jq_filter" 2>/dev/null || echo "__JQ_ERROR__")
    if [ "$actual" = "$expected_value" ]; then
        echo "  OK  [$TEST_SEQ] $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL [$TEST_SEQ] $desc  (esperado '$expected_value', obtido '$actual')"
        echo "       HTTP $(do_req "$method" "$path" "$data" "$token") | Body: $(echo "$body" | head -c 300)"
        FAIL=$((FAIL + 1))
    fi
}

assert_no_contains() {
    local method="$1" path="$2" desc="$3" substring="$4"
    local data="${5:-}"
    local token="${6:-}"
    TEST_SEQ=$((TEST_SEQ + 1))
    do_req "$method" "$path" "$data" "$token" > /dev/null
    local body
    body=$(cat /tmp/inttest_body)
    if echo "$body" | grep -qF "$substring"; then
        echo "  FAIL [$TEST_SEQ] $desc  (resposta contem '$substring')"
        echo "       Body: $(echo "$body" | head -c 300)"
        FAIL=$((FAIL + 1))
    else
        echo "  OK  [$TEST_SEQ] $desc"
        PASS=$((PASS + 1))
    fi
}

login() {
    local email="$1" password="$2"
    do_req POST /api/v1/auth/login "{\"email\":\"$email\",\"password\":\"$password\"}" "" > /dev/null
    local body
    body=$(cat /tmp/inttest_body)
    echo "$body" | jq -r '.accessToken // empty' 2>/dev/null || echo ""
}

echo "=========================================="
echo "  Ford Challenge - Testes de Integracao"
echo "  URL: $BASE_URL"
echo "=========================================="
echo ""

# ─── Setup: Autenticacao ────────────────────────────────────────────────────
echo ""
echo ">>> Setup: Login como admin e usuario regular"

ADMIN_TOKEN=$(login "admin@fordchallenge.com" "admin123")
USER_TOKEN=$(login "user@fordchallenge.com" "password123")

if [ -z "$ADMIN_TOKEN" ]; then
    echo "  WARN: Login admin falhou. Tentando registrar admin..."
    do_req POST /api/v1/auth/register \
        '{"name":"Admin","email":"admin@fordchallenge.com","password":"admin123","role":"ROLE_ADMIN"}' "" > /dev/null 2>&1 || true
    ADMIN_TOKEN=$(login "admin@fordchallenge.com" "admin123")
fi

if [ -z "$ADMIN_TOKEN" ]; then
    echo "  AVISO: Nao foi possivel obter token de admin"
    echo "  Testes autenticados serao pulados"
fi
echo "  Admin token: ${ADMIN_TOKEN:0:20}...${ADMIN_TOKEN: -5}"

# ─── Health (publico) ───────────────────────────────────────────────────────
echo ""
echo ">>> Health"

assert_status GET /api/v1/health 200 \
    "GET /api/v1/health (publico, sem auth)"

# ─── Vehicle Specs Query (autenticado) ──────────────────────────────────────
echo ""
echo ">>> VehicleSpecs Query"

VALID_PAYLOAD='{
    "brand": "Ford",
    "model": "Ranger",
    "version": "Raptor",
    "vehicleType": "cars",
    "attributes": ["Motor", "Potencia", "Torque", "Preco de referencia", "Modos de conducao"]
}'

if [ -n "$ADMIN_TOKEN" ]; then
    assert_status POST /api/v1/vehicle-specs/query 200 \
        "POST /api/v1/vehicle-specs/query (valido)" "$VALID_PAYLOAD" "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "query retorna brand=Ford" ".brand" "Ford" "$VALID_PAYLOAD" "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "query retorna model=Ranger" ".model" "Ranger" "$VALID_PAYLOAD" "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "query retorna version=Raptor" ".version" "Raptor" "$VALID_PAYLOAD" "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "query retorna attributes.Motor preenchido" \
        ".attributes.Motor | length > 0" \
        "true" "$VALID_PAYLOAD" "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "query retorna generatedAt nao nulo" \
        ".generatedAt != null" \
        "true" "$VALID_PAYLOAD" "$ADMIN_TOKEN"
else
    echo "  SKIP: Testes de query exigem token"
fi

# ─── Vehicle Specs Query - Atributo inexistente ────────────────────────────
echo ""
echo ">>> VehicleSpecs Query - Atributo indisponivel"

ATTR_INEXISTENTE='{
    "brand": "Ford",
    "model": "Ranger",
    "version": "Raptor",
    "attributes": ["AtributoInexistenteXYZ"]
}'

if [ -n "$ADMIN_TOKEN" ]; then
    assert_json POST /api/v1/vehicle-specs/query \
        "atributo inexistente retorna 'Nao disponivel'" \
        ".attributes.AtributoInexistenteXYZ" \
        "Nao disponivel" "$ATTR_INEXISTENTE" "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "atributo inexistente retorna source NOT_FOUND" \
        ".sources.AtributoInexistenteXYZ" \
        "NOT_FOUND" "$ATTR_INEXISTENTE" "$ADMIN_TOKEN"
else
    echo "  SKIP: Testes de query exigem token"
fi

# ─── Vehicle Specs Query - Validacao (400) ─────────────────────────────────
echo ""
echo ">>> Vehicle Specs Query - Erros de validacao"

if [ -n "$ADMIN_TOKEN" ]; then
    assert_status POST /api/v1/vehicle-specs/query 400 \
        "POST com brand vazio retorna 400" \
        '{"brand":"","model":"Ranger","version":"Raptor","attributes":["Motor"]}' "$ADMIN_TOKEN"

    assert_status POST /api/v1/vehicle-specs/query 400 \
        "POST com attributes vazio retorna 400" \
        '{"brand":"Ford","model":"Ranger","version":"Raptor","attributes":[]}' "$ADMIN_TOKEN"

    assert_status POST /api/v1/vehicle-specs/query 400 \
        "POST com brand <script> retorna 400" \
        '{"brand":"<script>alert(1)</script>","model":"Ranger","version":"Raptor","attributes":["Motor"]}' "$ADMIN_TOKEN"

    assert_status POST /api/v1/vehicle-specs/query 400 \
        "POST com 25 attributes retorna 400" \
        '{"brand":"Ford","model":"Ranger","version":"Raptor","attributes":["a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y"]}' "$ADMIN_TOKEN"
else
    echo "  SKIP: Testes de validacao exigem token"
fi

# ─── Autenticacao ───────────────────────────────────────────────────────────
echo ""
echo ">>> Autenticacao (RF01)"

assert_status POST /api/v1/auth/login 200 \
    "Login com credenciais validas" \
    '{"email":"admin@fordchallenge.com","password":"admin123"}'

assert_status POST /api/v1/auth/login 401 \
    "Login com senha errada" \
    '{"email":"admin@fordchallenge.com","password":"wrongpass"}'

assert_status POST /api/v1/auth/login 401 \
    "Login com email inexistente" \
    '{"email":"naoexiste@test.com","password":"admin123"}'

assert_status GET /api/v1/health 200 \
    "Health sem token (publico)" "" ""

# ─── Erros Seguros ──────────────────────────────────────────────────────────
echo ""
echo ">>> Erros Seguros (RF05)"

if [ -n "$ADMIN_TOKEN" ]; then
    assert_no_contains POST /api/v1/vehicle-specs/query \
        "Erro 400 nao contem stackTrace" \
        "stackTrace" \
        '{"brand":"","model":"Ranger","version":"Raptor","attributes":["Motor"]}' "$ADMIN_TOKEN"

    assert_json POST /api/v1/vehicle-specs/query \
        "Erro 400 retorna mensagem generica" \
        ".message" \
        "Dados de entrada invalidos" \
        '{"brand":"","model":"Ranger","version":"Raptor","attributes":["Motor"]}' "$ADMIN_TOKEN"
else
    echo "  SKIP: Testes de erro seguro exigem token"
fi

# Login com email inexistente retorna mesma msg que senha errada
do_req POST /api/v1/auth/login \
    '{"email":"naoexiste@test.com","password":"admin123"}' "" > /dev/null
MSG_INEXISTENTE=$(cat /tmp/inttest_body | jq -r '.message // "none"')

do_req POST /api/v1/auth/login \
    '{"email":"admin@fordchallenge.com","password":"wrongpass"}' "" > /dev/null
MSG_ERRADA=$(cat /tmp/inttest_body | jq -r '.message // "none"')

if [ "$MSG_INEXISTENTE" = "$MSG_ERRADA" ] && [ -n "$MSG_INEXISTENTE" ]; then
    echo "  OK  [$((++TEST_SEQ))] Erro de login: mesma mensagem para email inexistente e senha errada"
    PASS=$((PASS + 1))
else
    echo "  FAIL [$((++TEST_SEQ))] Erro de login: mensagens diferentes (inexistente='$MSG_INEXISTENTE' vs errada='$MSG_ERRADA')"
    FAIL=$((FAIL + 1))
fi

# ─── FIPE Catalog (autenticado) ────────────────────────────────────────────
echo ""
echo ">>> FIPE Catalog"

if [ -n "$ADMIN_TOKEN" ]; then
    assert_status GET "/api/v1/catalog/fipe/brands?vehicleType=cars" 200 \
        "GET /api/v1/catalog/fipe/brands" "" "$ADMIN_TOKEN"

    assert_json GET "/api/v1/catalog/fipe/brands?vehicleType=cars" \
        "brands retorna array" \
        "type == \"array\"" "true" "" "$ADMIN_TOKEN"

    assert_status GET "/api/v1/catalog/fipe/brands/22/models?vehicleType=cars" 200 \
        "GET /api/v1/catalog/fipe/brands/22/models" "" "$ADMIN_TOKEN"
else
    echo "  SKIP: Testes FIPE exigem token"
fi

# ─── RBAC ───────────────────────────────────────────────────────────────────
echo ""
echo ">>> RBAC (RF02)"

if [ -n "$ADMIN_TOKEN" ]; then
    # Admin pode acessar query
    assert_status POST /api/v1/vehicle-specs/query 200 \
        "Admin pode consultar query" "$VALID_PAYLOAD" "$ADMIN_TOKEN"
fi

if [ -n "$USER_TOKEN" ]; then
    # User pode acessar query
    assert_status POST /api/v1/vehicle-specs/query 200 \
        "User pode consultar query" "$VALID_PAYLOAD" "$USER_TOKEN"

    # User NAO pode acessar FIPE (403)
    assert_status GET "/api/v1/catalog/fipe/brands?vehicleType=cars" 403 \
        "User nao pode acessar FIPE (403)" "" "$USER_TOKEN"
fi

# ─── Resumo ────────────────────────────────────────────────────────────────
echo ""
echo "=========================================="
echo "  Resultado: $PASS passaram, $FAIL falharam"
echo "=========================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
