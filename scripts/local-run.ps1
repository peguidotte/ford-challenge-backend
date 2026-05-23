param(
    [switch]$SkipTests,
    [switch]$NoRun
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Invoke-Maven {
    param(
        [string[]]$MavenArgs,
        [string]$FailureMessage
    )

    & mvn @MavenArgs
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

function Resolve-JavaVersionForBuild {
    Write-Step "Tentando Java target 25"
    & mvn "-Djava.version=25" "-DskipTests" "test-compile" | Out-Host
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Java target selecionado: 25" -ForegroundColor Green
        return "25"
    }

    Write-Host "Java 25 indisponivel neste ambiente. Tentando Java 21..." -ForegroundColor Yellow

    & mvn "-Djava.version=21" "-DskipTests" "test-compile" | Out-Host
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Java target selecionado: 21" -ForegroundColor Green
        return "21"
    }

    throw "Nao foi possivel compilar com Java 25 nem Java 21. Verifique a instalacao do JDK."
}

function Wait-ForDocker {
    param(
        [int]$TimeoutSeconds = 60,
        [int]$RetryIntervalSeconds = 3
    )

    Write-Step "Verificando Docker (aguardando ate $TimeoutSeconds segundos)"

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $previousErrorAction = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & docker info > $null 2>&1
        $dockerExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorAction

        if ($dockerExitCode -eq 0) {
            Write-Host "Docker conectado com sucesso" -ForegroundColor Green
            return
        }

        Write-Host "Docker ainda indisponivel. Esperando inicializacao..." -ForegroundColor Yellow
        Start-Sleep -Seconds $RetryIntervalSeconds
    }

    throw "Nao foi possivel conectar ao Docker em $TimeoutSeconds segundos. Encerrando execucao."
}

Write-Step "Validando arquivo .env"
if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Arquivo .env criado a partir de .env.example" -ForegroundColor Green
} else {
    Write-Host "Arquivo .env ja existe" -ForegroundColor Green
}

Wait-ForDocker -TimeoutSeconds 60 -RetryIntervalSeconds 3

Write-Step "Subindo PostgreSQL com Docker Compose"
& docker compose --env-file .env up -d
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao subir o PostgreSQL via Docker Compose"
}

$javaVersion = Resolve-JavaVersionForBuild

if (-not $SkipTests) {
    Write-Step "Executando testes"
    Invoke-Maven -MavenArgs @("-Djava.version=$javaVersion", "test") -FailureMessage "Falha ao executar testes"
} else {
    Write-Step "Pulando testes por parametro -SkipTests"
}

if ($NoRun) {
    Write-Step "Finalizado sem iniciar a aplicacao por parametro -NoRun"
    exit 0
}

Write-Step "Iniciando a API"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Invoke-Maven -MavenArgs @("-Djava.version=$javaVersion", "spring-boot:run") -FailureMessage "Falha ao iniciar a aplicacao"
