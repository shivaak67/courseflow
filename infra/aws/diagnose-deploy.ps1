# Diagnose Prioritize EC2 deploy — writes NDJSON to debug-5ec6c4.log (debug session 5ec6c4)
param(
    [string]$Region = "us-east-1",
    [string]$PublicIp = "",
    [int]$MaxAttempts = 10
)

$ErrorActionPreference = "Continue"
$StateFile = Join-Path $PSScriptRoot "deploy-state.json"
$LogFile = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) "debug-5ec6c4.log"
$SessionId = "5ec6c4"
$RunId = "diagnose-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

function Write-DebugLog {
    param(
        [string]$HypothesisId,
        [string]$Location,
        [string]$Message,
        [hashtable]$Data = @{}
    )
    $entry = @{
        sessionId   = $SessionId
        runId       = $RunId
        hypothesisId = $HypothesisId
        location    = $Location
        message     = $Message
        data        = $Data
        timestamp   = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    } | ConvertTo-Json -Compress -Depth 6
    Add-Content -Path $LogFile -Value $entry -Encoding utf8
}

function Get-AwsExe {
    if (Get-Command aws -ErrorAction SilentlyContinue) { return "aws" }
    $full = "C:\Program Files\Amazon\AWSCLIV2\aws.exe"
    if (Test-Path $full) { return $full }
    return $null
}

if (-not $PublicIp -and (Test-Path $StateFile)) {
    $state = Get-Content $StateFile | ConvertFrom-Json
    $PublicIp = $state.publicIp
    if ($state.region) { $Region = $state.region }
}

if (-not $PublicIp) {
    Write-DebugLog -HypothesisId "SETUP" -Location "diagnose-deploy.ps1" -Message "No public IP" -Data @{ stateFile = $StateFile }
    throw "No PublicIp. Pass -PublicIp or ensure deploy-state.json exists."
}

$baseUrl = "http://$PublicIp"
Write-DebugLog -HypothesisId "SETUP" -Location "diagnose-deploy.ps1" -Message "Starting diagnosis" -Data @{
    publicIp = $PublicIp
    region   = $Region
    baseUrl  = $baseUrl
    maxAttempts = $MaxAttempts
}

# Hypothesis A/B/E: frontend vs API status codes
for ($i = 1; $i -le $MaxAttempts; $i++) {
    $frontendCode = "err"
    $apiCode = "err"
    $apiBodySnippet = ""
    $frontendMs = 0
    $apiMs = 0

    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $fe = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing -TimeoutSec 15
        $sw.Stop()
        $frontendCode = [int]$fe.StatusCode
        $frontendMs = $sw.ElapsedMilliseconds
    } catch {
        $frontendCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "err" }
        $frontendMs = -1
    }

    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $api = Invoke-WebRequest -Uri "$baseUrl/actuator/health" -UseBasicParsing -TimeoutSec 15
        $sw.Stop()
        $apiCode = [int]$api.StatusCode
        $apiBodySnippet = if ($api.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($api.Content)
        } else {
            [string]$api.Content
        }
        $apiBodySnippet = $apiBodySnippet.Substring(0, [Math]::Min(200, $apiBodySnippet.Length))
        $apiMs = $sw.ElapsedMilliseconds
    } catch {
        $apiCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "err" }
        $apiMs = -1
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $apiBodySnippet = $reader.ReadToEnd().Substring(0, [Math]::Min(200, $reader.ReadToEnd().Length))
            } catch { $apiBodySnippet = $_.Exception.Message.Substring(0, [Math]::Min(120, $_.Exception.Message.Length)) }
        }
    }

    Write-Host "[$i/$MaxAttempts] frontend=$frontendCode (${frontendMs}ms) api=$apiCode (${apiMs}ms)"

  #region agent log
    Write-DebugLog -HypothesisId "A" -Location "diagnose-deploy.ps1:poll" -Message "Poll result" -Data @{
        attempt = $i
        frontendStatus = $frontendCode
        frontendMs = $frontendMs
        apiStatus = $apiCode
        apiMs = $apiMs
        apiBodySnippet = $apiBodySnippet
    }
  #endregion

    $isJsonUp = ($apiCode -eq 200 -and $apiBodySnippet -match '"status"\s*:\s*"UP"')
    $isSpringJson = ($apiBodySnippet -match '"timestamp"\s*:' -and $apiBodySnippet -match '"status"\s*:\s*\d+')
    $isHtmlFalsePositive = ($apiBodySnippet -match '<!doctype html>' -or $apiBodySnippet -match '<html')

    if ($isJsonUp -or $isSpringJson) {
        Write-DebugLog -HypothesisId "A" -Location "diagnose-deploy.ps1" -Message "API healthy (JSON UP or Spring JSON)" -Data @{ attempt = $i; bodySnippet = $apiBodySnippet }
        Write-Host "API is UP."
        break
    }
    if ($apiCode -eq 200 -and $isHtmlFalsePositive) {
        Write-Host "API returned 200 but Angular HTML (nginx /actuator/ proxy missing)."
        Write-DebugLog -HypothesisId "E" -Location "diagnose-deploy.ps1" -Message "HTML false positive on /actuator/health" -Data @{ attempt = $i; bodySnippet = $apiBodySnippet }
    }

    if ($i -lt $MaxAttempts) { Start-Sleep -Seconds 15 }
}

# Hypothesis B/C/D: EC2 console output (user-data / docker errors)
$aws = Get-AwsExe
if ($aws -and (Test-Path $StateFile)) {
    $instanceId = (Get-Content $StateFile | ConvertFrom-Json).instanceId
    if ($instanceId) {
        $consolePath = Join-Path $PSScriptRoot "diagnose-console.txt"
        & $aws ec2 get-console-output --region $Region --instance-id $instanceId --latest --output text 2>$null | Out-File -FilePath $consolePath -Encoding utf8
        $consoleText = ""
        if (Test-Path $consolePath) {
            $consoleText = Get-Content $consolePath -Raw -ErrorAction SilentlyContinue
        }
        $hasUnhealthy = $consoleText -match "prioritize-backend is unhealthy"
        $hasOom = $consoleText -match "OutOfMemory|Killed process|oom"
        $hasFlyway = $consoleText -match "Flyway|migration"
        $hasDbError = $consoleText -match "Connection refused|password authentication failed|FATAL"
        $tail = if ($consoleText.Length -gt 2500) { $consoleText.Substring($consoleText.Length - 2500) } else { $consoleText }

      #region agent log
        Write-DebugLog -HypothesisId "B" -Location "diagnose-deploy.ps1:console" -Message "Console scan" -Data @{
            instanceId = $instanceId
            hasUnhealthyBackend = $hasUnhealthy
            hasOom = $hasOom
            hasFlyway = $hasFlyway
            hasDbError = $hasDbError
            consoleTail = $tail
        }
      #endregion
        Write-Host "Console: unhealthy=$hasUnhealthy oom=$hasOom flyway=$hasFlyway dbError=$hasDbError"
    }
}

Write-Host "Logs written to $LogFile"
