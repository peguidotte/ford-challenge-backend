<h1 align="center">
  🚗 Ford Challenge — Backend
</h1>

<p align="center">
  <strong>Inteligência Competitiva Automotiva · FIAP × Ford · 2026</strong>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white"/>
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white"/>
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white"/>
  <img alt="Docker" src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white"/>
  <img alt="Flyway" src="https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white"/>
  <img alt="Swagger" src="https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?logo=swagger&logoColor=black"/>
  <img alt="License" src="https://img.shields.io/badge/license-MIT-blue"/>
</p>

---

## 👥 Integrantes

| Nome | RM |
|---|---|
| Leonardo Correa de Mello | RM 555573 |
| Felipe Soares Xavier | RM 556931 |
| Pedro Visconti Guidotte | RM 556630 |
| Herbert de Sousa Vilela | RM 555701 |
| Gabriel Figueira Flora | RM 556476 |

---

## 📖 Sobre o Projeto

O mapeamento de especificações técnicas de veículos concorrentes é hoje um processo manual e demorado — consome cerca de **1 hora por versão avaliada**, dependendo de buscas pulverizadas em sites, revistas e visitas a concessionárias.

A solução centraliza e padroniza esses dados: a partir de uma entrada simples (**Marca, Modelo, Versão e Atributos desejados**), a API consolida uma lista de especificações clara, organizada e diretamente comparável — pronta para alimentar dashboards, relatórios e agentes de IA.

---

## 🛠️ Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 (com fallback para Java 21) |
| Framework | Spring Boot 4.x |
| Persistência | Spring Data JPA / Hibernate |
| Banco de dados | PostgreSQL 16 |
| Migrations | Flyway |
| Infraestrutura local | Docker & Docker Compose |
| Documentação da API | OpenAPI 3 / Swagger UI |

---

## 📦 Dependências

Antes de rodar o projeto, garanta que você tem instalado:

- **Docker** (para subir o banco PostgreSQL localmente)
- **Java 25** ou **Java 21** — [Eclipse Adoptium Temurin](https://adoptium.net/) recomendado
- **Maven** (ou utilize o wrapper `./mvnw` incluso no projeto)

> O Spring Boot é uma dependência gerenciada pelo Maven — não precisa de instalação separada.

---

## 🔑 Configurando as APIs externas

O projeto consome duas APIs externas. Ambas funcionam sem cadastro, mas **recomendamos criar as credenciais** para evitar limitações de rate limit em uso intenso.

---

### FIPE API *(opcional, mas recomendado)*

A API FIPE pode ser consumida sem token, porém criar uma conta aumenta os limites de uso.

1. Crie sua conta em **[fipe.online/register](https://fipe.online/register)**
2. Acesse seu dashboard e copie a chave em **[fipe.online/dashboard/keys](https://fipe.online/dashboard/keys)**
3. Cole o valor no `.env`:
   ```env
   FIPE_TOKEN=sua_chave_aqui
   ```

> Sem `FIPE_TOKEN`, a aplicação ainda consulta a tabela FIPE normalmente — apenas com limites menores.

---

### CarAPI *(recomendado)*

A CarAPI é a **fonte primária** de especificações técnicas. Quando ela não retorna dados suficientes para um veículo, o sistema faz fallback automático para a API FIPE e, por último, para uma base de dados local com informações previamente coletadas. Para melhores resultados, configure as credenciais da CarAPI.

1. Crie sua conta em **[carapi.app/register](https://carapi.app/register)**
2. Acesse seu perfil e gere o token e o secret em **[carapi.app/profile/users/api](https://carapi.app/profile/users/api)**
3. Cole os valores no `.env`:
   ```env
   CAR_API_API_TOKEN=seu_token_aqui
   CAR_API_API_SECRET=seu_secret_aqui
   ```

> A cadeia de fallback é: **CarAPI → FIPE → base local**. Sem as credenciais, as consultas dependem dos níveis seguintes, com cobertura e atualização de dados menores.

---

## 🚀 Como subir o projeto

### 1. Configure as variáveis de ambiente

```bash
cp .env.example .env
# Preencha os tokens das APIs externas conforme a seção acima (opcional)
```

### 2. Suba o banco de dados

```bash
docker compose --env-file .env up -d
```

### 3. Rode a aplicação

```bash
mvn spring-boot:run
```

> O Flyway executa as migrations automaticamente na inicialização.

---

## ⚡ Scripts de execução local

Os scripts automatizam todo o ciclo: checam pré-requisitos, sobem o banco e iniciam a API.

### Windows (PowerShell)

```powershell
# Execução completa (com testes)
powershell -ExecutionPolicy Bypass -File .\scripts\local-run.ps1

# Pular testes
powershell -ExecutionPolicy Bypass -File .\scripts\local-run.ps1 -SkipTests

# Apenas preparar ambiente (não sobe a API)
powershell -ExecutionPolicy Bypass -File .\scripts\local-run.ps1 -NoRun
```

### Linux / macOS (Bash)

```bash
# Execução completa (com testes)
./scripts/run.sh

# Pular testes
./scripts/run.sh --skip-tests

# Apenas preparar ambiente (não sobe a API)
./scripts/run.sh --no-run
```

> Os scripts detectam automaticamente a versão do Java disponível e fazem **fallback de Java 25 → Java 21** se necessário.

---

## 🧪 Testando a API

### Swagger UI

Acesse diretamente pelo navegador após subir a aplicação:

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

### Collection Postman / REST Client

Uma collection pronta está disponível em [postman/ford-challenge-backend.postman_collection.json](postman/ford-challenge-backend.postman_collection.json).

Ela pode ser importada no **Postman**, **Insomnia**, **Bruno** ou qualquer cliente REST compatível com o formato Collection v2.1. Já vem com `baseUrl` configurada e exemplos de payload para uso imediato em ambiente local.

---

## 📡 Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/health` | Health check da aplicação |
| `POST` | `/api/v1/vehicle-specs/query` | Consulta especificações técnicas de veículos |
| `GET` | `/api/v1/catalog/fipe/brands` | Lista marcas disponíveis na tabela FIPE |
| `GET` | `/api/v1/catalog/fipe/models` | Lista modelos por marca |
| `GET` | `/api/v1/catalog/fipe/years` | Lista anos disponíveis por modelo |
| `GET` | `/api/v1/catalog/fipe/price` | Consulta preço FIPE |
