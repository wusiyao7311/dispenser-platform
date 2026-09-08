<#
.SYNOPSIS
    Restarts the dispenser-platform Windows service on a touch-PC dispenser
    host and confirms it comes back healthy. Intended for 2nd/3rd-level
    support runbooks and Task Scheduler-driven recurring maintenance windows.

.PARAMETER ServiceName
    Name of the Windows service wrapping the application (e.g. via NSSM or
    WinSW). Defaults to "DispenserPlatform".

.PARAMETER HealthUrl
    Local health endpoint to poll after restart.

.EXAMPLE
    .\Restart-DispenserService.ps1
    .\Restart-DispenserService.ps1 -ServiceName "DispenserPlatform" -HealthUrl "http://localhost:8080/actuator/health"
#>

param(
    [string]$ServiceName = "DispenserPlatform",
    [string]$HealthUrl = "http://localhost:8080/actuator/health",
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

function Write-Log {
    param([string]$Message)
    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Write-Output "$timestamp [restart-service] $Message"
}

$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Log "ERROR: service '$ServiceName' not found on this host"
    exit 1
}

Write-Log "Restarting service '$ServiceName' (current status: $($service.Status))"
Restart-Service -Name $ServiceName -Force

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$healthy = $false

while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200 -and $response.Content -match '"status"\s*:\s*"UP"') {
            $healthy = $true
            break
        }
    } catch {
        Write-Log "waiting for service to respond ($($_.Exception.Message))"
    }
}

if ($healthy) {
    Write-Log "OK: service '$ServiceName' is UP and responding at $HealthUrl"
    exit 0
} else {
    Write-Log "ERROR: service '$ServiceName' did not become healthy within $TimeoutSeconds seconds"
    exit 1
}
