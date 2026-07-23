# ===============================================================
# Debezium AI — One-Click Setup & Configuration Wizard (Windows)
# ===============================================================
# This script automates the installation of Debezium AI on Windows.
# ===============================================================

param(
    [string]$Mode = "dev",
    [string]$Storage = "memory",
    [string]$Auth = "local",
    [string]$Port = "8080"
)

$VERSION = "4.0.1"
$PROJECT_DIR = Split-Path -Parent $PSScriptRoot

Write-Host "================================================" -ForegroundColor Blue
Write-Host "  Debezium AI v$VERSION — Setup Wizard (Windows)" -ForegroundColor Blue
Write-Host "================================================" -ForegroundColor Blue
Write-Host ""

# Check Java
try {
    $javaVersion = java -version 2>&1 | Out-String
    Write-Host "  Java: Found" -ForegroundColor Green
} catch {
    Write-Host "  Java: Not found. Please install Java 21+ from https://adoptium.net" -ForegroundColor Red
    exit 1
}

# Check Maven
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "  Maven: Found" -ForegroundColor Green
} else {
    Write-Host "  Maven: Not found (will use wrapper)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Installation Mode: $Mode" -ForegroundColor Yellow
Write-Host ""

# Generate API Key
$apiKey = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | % { [char]$_ })
$adminPass = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 16 | % { [char]$_ })

# Generate config
$configPath = Join-Path $PROJECT_DIR "application.properties"
@"
# ==========================================
# Debezium AI v$VERSION — Configuration
# ==========================================
quarkus.http.port=$Port
debezium.storage.type=$Storage
debezium.auth.type=$Auth
debezium.api.key=$apiKey
debezium.deployment.target=$Mode
debezium.ai.embeddings.provider=minilm
"@ | Set-Content -Path $configPath

Write-Host "  Configuration generated: $configPath" -ForegroundColor Green
Write-Host "  API Key: $apiKey" -ForegroundColor Green
Write-Host ""

# Build
Write-Host "Building Debezium AI v$VERSION..." -ForegroundColor Yellow
Set-Location -Path $PROJECT_DIR

if (Test-Path ".\mvnw.cmd") {
    & ".\mvnw.cmd" clean install -Dquick -DskipITs
} else {
    mvn clean install -Dquick -DskipITs
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  Debezium AI v$VERSION Setup Complete!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Start the application:" -ForegroundColor Yellow
Write-Host "    cd $PROJECT_DIR"
Write-Host "    .\mvnw.cmd quarkus:dev"
Write-Host ""
Write-Host "  Access: http://localhost:$Port/v4" -ForegroundColor Green
Write-Host "  API Key: $apiKey" -ForegroundColor Green
Write-Host "  Admin Password: $adminPass" -ForegroundColor Green
Write-Host ""
