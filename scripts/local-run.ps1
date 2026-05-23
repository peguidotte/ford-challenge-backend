param(
    [switch]$SkipTests,
    [switch]$NoRun
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $ProjectRoot

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

function Configure-JavaHomeForBuild {
    Write-Step "Configurando JAVA_HOME para build local"

    $candidates = @()

    if ($env:JAVA25_HOME -and (Test-Path (Join-Path $env:JAVA25_HOME "bin\java.exe"))) {
        $candidates += $env:JAVA25_HOME
    }

    $candidates += @(Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "jdk-25*" } |
        Sort-Object Name -Descending |
        Select-Object -ExpandProperty FullName)

    $candidates += @(Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "jdk-25*" } |
        Sort-Object Name -Descending |
        Select-Object -ExpandProperty FullName)

    $latestJava = "C:\Program Files\Java\latest"
    if (Test-Path (Join-Path $latestJava "bin\java.exe")) {
        $candidates += $latestJava
    }

    $selected = $candidates |
        Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) } |
        Select-Object -First 1

    if (-not $selected) {
        Write-Host "Nao foi encontrado JDK 25 automaticamente. Mantendo JAVA_HOME atual." -ForegroundColor Yellow
        return
    }

    $env:JAVA_HOME = $selected
    $env:Path = "$selected\bin;$env:Path"
    Write-Host "JAVA_HOME configurado para: $selected" -ForegroundColor Green
    & java -version | Out-Host
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

Configure-JavaHomeForBuild

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
