param (
    [string]$TargetIp = "127.0.0.1",
    [int]$Port = 8420,
    [string]$AuthToken = "master-companion-default-token",
    [string]$Action = "ping"
)

$baseUrl = "http://${TargetIp}:${Port}"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host " Master Companion — Command Bridge HTTP Client Test" -ForegroundColor Cyan
Write-Host " Target URL: $baseUrl" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

switch ($Action.ToLower()) {
    "ping" {
        Write-Host "`n[TEST] Sending GET /ping..." -ForegroundColor Yellow
        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/ping" -Method GET -TimeoutSec 3
            Write-Host "Response: $($resp | ConvertTo-Json -Compress)" -ForegroundColor Green
        } catch {
            Write-Host "Failed: $_" -ForegroundColor Red
        }
    }
    "status" {
        Write-Host "`n[TEST] Sending GET /status..." -ForegroundColor Yellow
        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/status" -Method GET -TimeoutSec 3
            Write-Host "Response: $($resp | ConvertTo-Json)" -ForegroundColor Green
        } catch {
            Write-Host "Failed: $_" -ForegroundColor Red
        }
    }
    "commands" {
        Write-Host "`n[TEST] Sending GET /commands..." -ForegroundColor Yellow
        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/commands" -Method GET -TimeoutSec 3
            Write-Host "Response: $($resp | ConvertTo-Json)" -ForegroundColor Green
        } catch {
            Write-Host "Failed: $_" -ForegroundColor Red
        }
    }
    default {
        Write-Host "`n[TEST] Sending POST /command (action: $Action)..." -ForegroundColor Yellow
        $headers = @{
            "Content-Type" = "application/json"
            "X-Auth-Token" = $AuthToken
        }
        $body = @{
            action = $Action
            params = @{}
        } | ConvertTo-Json

        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/command" -Method POST -Headers $headers -Body $body -TimeoutSec 3
            Write-Host "Response: $($resp | ConvertTo-Json)" -ForegroundColor Green
        } catch {
            Write-Host "Failed: $_" -ForegroundColor Red
        }
    }
}
