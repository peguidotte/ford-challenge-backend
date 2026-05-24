#!/usr/bin/env bash
# Run Ford Challenge Backend locally
# Usage: ./scripts/run.sh [--skip-tests] [--no-run]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
step()  { echo -e "\n${CYAN}==>${NC} $1"; }
ok()    { echo -e "  ${GREEN}OK${NC} $1"; }
warn()  { echo -e "  ${YELLOW}AVISO${NC} $1"; }
fail()  { echo -e "  ${RED}ERRO${NC} $1"; exit 1; }

SKIP_TESTS=false; NO_RUN=false
while [[ $# -gt 0 ]]; do
    case "$1" in --skip-tests) SKIP_TESTS=true ;; --no-run) NO_RUN=true ;; *) fail "Parametro desconhecido: $1" ;; esac; shift
done

# ─── Pre-requisitos ────────────────────────────────────────────────────────

step "Verificando pre-requisitos"

command -v docker >/dev/null 2>&1 || fail "Docker nao instalado."

USE_DOCKER_MVN=true
JAVA_TARGET="25"

# Tenta usar Maven do host se disponivel com Java compativel
if command -v mvn >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/[^0-9.]//g' | cut -d. -f1)
    if [ "$JAVA_VER" -ge 25 ] 2>/dev/null; then
        USE_DOCKER_MVN=false; MVN_BIN="mvn"; ok "Maven no host (Java $JAVA_VER)"
    elif [ "$JAVA_VER" -ge 21 ] 2>/dev/null; then
        USE_DOCKER_MVN=false; MVN_BIN="mvn"; JAVA_TARGET=21; ok "Maven no host (Java $JAVA_VER, target 21)"
    fi
fi

# Tenta Maven Wrapper como alternativa
if [ "$USE_DOCKER_MVN" = true ] && [ -x "./mvnw" ]; then
    USE_DOCKER_MVN=false; MVN_BIN="./mvnw"; ok "Maven Wrapper encontrado"
fi

# fallback: Maven via Docker
if [ "$USE_DOCKER_MVN" = true ]; then
    # Tenta imagem com JDK 25; se nao existe, baixa
    if ! docker image inspect maven:3-eclipse-temurin-25-alpine >/dev/null 2>&1; then
        docker pull maven:3-eclipse-temurin-25-alpine
    fi
    MVN_DOCKER_IMG="maven:3-eclipse-temurin-25-alpine"
    ok "Maven via Docker (${MVN_DOCKER_IMG})"
fi

mvn_cmd() {
    if [ "$USE_DOCKER_MVN" = true ]; then
        docker run --rm \
            -v "$PROJECT_DIR:/project" \
            -w /project \
            -v "$HOME/.m2:/root/.m2" \
            -e MAVEN_CONFIG=/root/.m2 \
            --network host \
            "$MVN_DOCKER_IMG" \
            mvn "-Djava.version=$JAVA_TARGET" "$@"
    else
        "$MVN_BIN" "-Djava.version=$JAVA_TARGET" "$@"
    fi
}

# ─── .env ───────────────────────────────────────────────────────────────────

step "Configurando .env"
if [ ! -f ".env" ]; then
    cp .env.example .env
    ok ".env criado a partir de .env.example"
else
    ok ".env ja existe"
fi

# ─── Docker Compose (PostgreSQL) ───────────────────────────────────────────

step "Subindo PostgreSQL com Docker Compose"
docker compose --env-file .env up -d
ok "PostgreSQL iniciado"

step "Aguardando PostgreSQL ficar saudavel"
for i in $(seq 1 30); do
    if docker compose exec postgres pg_isready -U ford -d ford_challenge >/dev/null 2>&1; then
        ok "PostgreSQL pronto"
        break
    fi
    if [ "$i" -eq 30 ]; then
        fail "PostgreSQL nao ficou pronto em 30s. Logs: docker compose logs postgres"
    fi
    sleep 1
done

# ─── Compilar / Testar ─────────────────────────────────────────────────────

if [ "$SKIP_TESTS" = false ]; then
    step "Executando testes"
    mvn_cmd test
    ok "Testes passaram"
else
    step "Compilando (testes pulados)"
    mvn_cmd compile -DskipTests
    ok "Compilacao concluida"
fi

# ─── Rodar ─────────────────────────────────────────────────────────────────

if [ "$NO_RUN" = true ]; then
    echo ""
    ok "Build concluido. Para rodar a API: ./scripts/run.sh"
    exit 0
fi

step "Iniciando API Ford Challenge"
echo -e "  Swagger: ${GREEN}http://localhost:8080/swagger-ui.html${NC}"
echo -e "  Health:  ${GREEN}http://localhost:8080/actuator/health${NC}"
echo ""

mvn_cmd spring-boot:run
