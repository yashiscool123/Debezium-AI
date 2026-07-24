# ===============================================================
# Debezium AI — Unified CLI (PowerShell)
# ===============================================================
# Usage: .\debezium-ai.ps1 <command> [options]
#
# Commands:
#   build [v3|v4|all]   Build project modules (default: all)
#   run [v3|v4]         Run in Quarkus dev mode (default: v4)
#   setup               Run interactive setup wizard
#   docker [profile]    Run Docker Compose (dev|full|streaming|monitoring)
#   help                Show this help message
# ===============================================================

param(
    [Parameter(Position = 0)]
    [string]$Command = "help",

    [Parameter(Position = 1, ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$Version = "4.1.0"
$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Mvnw = Join-Path $RootDir "mvnw.cmd"

function Write-Info  { Write-Host "[debezium-ai] $args" -ForegroundColor Blue }
function Write-Ok    { Write-Host "[v] $args" -ForegroundColor Green }
function Write-Warn  { Write-Host "[!] $args" -ForegroundColor Yellow }
function Write-Err   { Write-Host "[x] $args" -ForegroundColor Red }

function Show-Usage {
    @"
Debezium AI v$Version - Unified CLI

Usage: .\debezium-ai.ps1 <command> [options]

Commands:
  build [v3|v4|all]   Build project modules (default: all)
  run [v3|v4]         Run in Quarkus dev mode (default: v4)
  setup               Run interactive setup wizard
  docker [profile]    Run Docker Compose (dev|full|streaming|monitoring)
  help                Show this help message

Examples:
  .\debezium-ai.ps1 build          Build everything
  .\debezium-ai.ps1 build v4       Build only v4
  .\debezium-ai.ps1 run v4         Start v4 in dev mode
  .\debezium-ai.ps1 docker dev     Start lightweight dev stack
  .\debezium-ai.ps1 setup          Launch configuration wizard
"@
}

function Invoke-Build {
    $target = if ($Args.Count -gt 0) { $Args[0] } else { "all" }
    if (-not (Test-Path $Mvnw)) { Write-Err "Maven wrapper not found at $Mvnw"; exit 1 }

    switch ($target) {
        "v3" {
            Write-Info "Building v3 Pipeline Generator..."
            Push-Location (Join-Path $RootDir "debezium-pipeline-generator")
            & $Mvnw clean install -Dquick
            Pop-Location
            Write-Ok "v3 build complete"
        }
        "v4" {
            Write-Info "Building v4 Pipeline Generator..."
            Push-Location (Join-Path $RootDir "debezium-pipeline-v4")
            & $Mvnw clean install -Dquick
            Pop-Location
            Write-Ok "v4 build complete"
        }
        "all" {
            Write-Info "Building entire project (Debezium Core + v3 + v4)..."
            & $Mvnw clean install -Dquick -DskipITs
            Write-Ok "Full build complete"
        }
        default {
            Write-Err "Unknown target: $target. Use v3, v4, or all."
            exit 1
        }
    }
}

function Invoke-Run {
    $target = if ($Args.Count -gt 0) { $Args[0] } else { "v4" }
    if (-not (Test-Path $Mvnw)) { Write-Err "Maven wrapper not found at $Mvnw"; exit 1 }

    switch ($target) {
        "v3" {
            Write-Info "Starting v3 in dev mode on http://localhost:8080/api/v1..."
            Push-Location (Join-Path $RootDir "debezium-pipeline-generator")
            & $Mvnw quarkus:dev
            Pop-Location
        }
        "v4" {
            Write-Info "Starting v4 in dev mode on http://localhost:8080/v4..."
            Push-Location (Join-Path $RootDir "debezium-pipeline-v4")
            & $Mvnw quarkus:dev
            Pop-Location
        }
        default {
            Write-Err "Unknown target: $target. Use v3 or v4."
            exit 1
        }
    }
}

function Invoke-Setup {
    $installer = Join-Path $RootDir "debezium-pipeline-v4\installer\setup.ps1"
    if (-not (Test-Path $installer)) { Write-Err "Installer not found at $installer"; exit 1 }
    Write-Info "Launching interactive setup wizard..."
    & $installer
}

function Invoke-Docker {
    $profile = if ($Args.Count -gt 0) { $Args[0] } else { "dev" }
    $compose = Join-Path $RootDir "debezium-pipeline-v4\installer\docker-compose.yml"
    if (-not (Test-Path $compose)) { Write-Err "Docker Compose file not found at $compose"; exit 1 }

    switch ($profile) {
        "dev" { & docker compose -f $compose --profile dev up -d; Write-Ok "Dev stack started" }
        "full" { & docker compose -f $compose --profile full up -d; Write-Ok "Full stack started" }
        "streaming" { & docker compose -f $compose --profile streaming up -d; Write-Ok "Streaming stack started" }
        "monitoring" { & docker compose -f $compose --profile monitoring up -d; Write-Ok "Monitoring stack started" }
        "down" { & docker compose -f $compose down; Write-Ok "Stack stopped" }
        "logs" { & docker compose -f $compose logs -f }
        default { Write-Err "Unknown profile: $profile. Use dev, full, streaming, monitoring, down, or logs."; exit 1 }
    }
}

# --- Main ---
switch ($Command) {
    "build"  { Invoke-Build }
    "run"    { Invoke-Run }
    "setup"  { Invoke-Setup }
    "docker" { Invoke-Docker }
    "help"   { Show-Usage }
    default  { Write-Err "Unknown command: $Command"; Show-Usage; exit 1 }
}