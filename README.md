# Ford Challenge Backend

Backend Spring Boot para consolidacao de especificacoes tecnicas de veiculos concorrentes.

## Stack
- Java 25
- Spring Boot 4.x
- PostgreSQL
- Flyway
- OpenAPI/Swagger

## Subindo banco local
1. Copie `.env.example` para `.env`.
2. Execute:
   - `docker compose --env-file .env up -d`

## Rodando aplicacao
1. Configure variaveis de ambiente do banco e APIs externas.
2. Execute:
   - `mvn spring-boot:run`

## Script unico de execucao local
Use o script abaixo para preparar ambiente, subir banco, testar e iniciar a API.

1. Execucao completa:
   - `powershell -ExecutionPolicy Bypass -File .\scripts\local-run.ps1`
2. Sem testes:
   - `powershell -ExecutionPolicy Bypass -File .\scripts\local-run.ps1 -SkipTests`
3. Somente preparar ambiente (nao sobe API):
   - `powershell -ExecutionPolicy Bypass -File .\scripts\local-run.ps1 -NoRun`

O script tenta compilar primeiro com `java.version=25`. Se nao conseguir, faz fallback automatico para `java.version=21`.

## Swagger
- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Endpoints iniciais
- `GET /api/v1/health`
- `POST /api/v1/vehicle-specs/query`
- `GET /api/v1/catalog/fipe/brands`
- `GET /api/v1/catalog/fipe/models`
- `GET /api/v1/catalog/fipe/years`
- `GET /api/v1/catalog/fipe/price`
