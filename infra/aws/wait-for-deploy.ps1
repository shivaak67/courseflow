# Wait until Prioritize EC2 deploy is reachable. Logs NDJSON to debug-5ec6c4.log
param(
    [string]$Region = "us-east-1",
    [string]$PublicIp = "",
    [int]$MaxFrontendAttempts = 80,
    [int]$MaxApiAttempts = 60
)

$ErrorActionPreference = "Continue"
$StateFile = Join-Path $PSScriptRoot "deploy-state.json"
$LogFile = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) "debug-5ec6c4.log"
$SessionId = "5ec6c4"
$RunId = "wait-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

function Write-DebugLog {
    param(
        [string]$HypothesisId,
        [string]$Location,
        [string]$Message,
        [hashtable]$Data = @{}
    )
    $entry = @{
        sessionId    = $SessionId
        runId        = $RunId
        hypothesisId = $HypothesisId
        location     = $Location
        message      = $Message
        data         = $Data
        timestamp    = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    } | ConvertTo-Json -Compress -Depth 6
    Add-Content -Path $LogFile -Value $entry -Encoding utf8
}

if (-not $PublicIp -and (Test-Path $StateFile)) {
    $state = Get-Content $StateFile | ConvertFrom-Json
    $PublicIp = $state.publicIp
    if ($state.region) { $Region = $state.region }
}

if (-not $PublicIp) {
    throw "No PublicIp. Pass -PublicIp or ensure deploy-state.json exists."
}

$baseUrl = "http://$PublicIp"
$healthUrl = "$baseUrl/actuator/health"

Write-DebugLog -HypothesisId "SETUP" -Location "wait-for-deploy.ps1" -Message "Waiting for deploy" -Data @{
    publicIp = $PublicIp
    healthUrl = $healthUrl
}

for ($i = 1; $i -le $MaxFrontendAttempts; $i++) {
    try {
        $fe = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing -TimeoutSec 15
        if ($fe.StatusCode -eq 200) {
            Write-Host "Frontend UP at attempt $i"
            Write-DebugLog -HypothesisId "A" -Location "wait-for-deploy.ps1" -Message "Frontend up" -Data @{ attempt = $i }
            break
        }
    } catch {
        # continue
    }
    Write-Host "Frontend waiting $i/$MaxFrontendAttempts..."
    if ($i -eq $MaxFrontendAttempts) {
        Write-DebugLog -HypothesisId "A" -Location "wait-for-deploy.ps1" -Message "Frontend timeout" -Data @{ attempts = $i }
        throw "Frontend did not become ready."
    }
    Start-Sleep -Seconds 20
}

for ($j = 1; $j -le $MaxApiAttempts; $j++) {
    $code = "err"
    $bodySnippet = ""
    $apiReady = $false
    try {
        $api = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 15
        $code = [int]$api.StatusCode
        $content = if ($api.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($api.Content)
        } else {
            [string]$api.Content
        }
        $bodySnippet = $content.Substring(0, [Math]::Min(200, $content.Length))
        if ($code -eq 200 -and $bodySnippet -match '"status"\s*:\s*"UP"') {
            $apiReady = $true
        } elseif ($bodySnippet -match '"timestamp"\s*:' -and $bodySnippet -match '"status"\s*:\s*\d+') {
            $apiReady = $true
        }
    } catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "err" }
        if ($_.ErrorDetails.Message) {
            $bodySnippet = $_.ErrorDetails.Message.Substring(0, [Math]::Min(200, $_.ErrorDetails.Message.Length))
            if ($bodySnippet -match '"timestamp"\s*:' -and $bodySnippet -match '"status"\s*:\s*\d+') {
                $apiReady = $true
            }
        }
    }

    if (-not $apiReady -and ($bodySnippet -match '<!doctype html>' -or $bodySnippet -match '<html' -or $code -eq 200)) {
        try {
            $probe = Invoke-WebRequest -Uri "$baseUrl/api/auth/config" -UseBasicParsing -TimeoutSec 15
            $probeBody = [string]$probe.Content
            if ($probeBody -match '"timestamp"\s*:' -or $probeBody -match 'googleOAuthEnabled') {
                $apiReady = $true
                $bodySnippet = $probeBody.Substring(0, [Math]::Min(200, $probeBody.Length))
            }
        } catch {
            if ($_.ErrorDetails.Message -match '"timestamp"\s*:') {
                $apiReady = $true
                $bodySnippet = $_.ErrorDetails.Message.Substring(0, [Math]::Min(200, $_.ErrorDetails.Message.Length))
            }
        }
    }

    if ($apiReady) {
        Write-Host "API healthy at attempt $j"
        Write-DebugLog -HypothesisId "F" -Location "wait-for-deploy.ps1" -Message "API healthy" -Data @{
            attempt = $j
            healthUrl = $healthUrl
            bodySnippet = $bodySnippet
        }
        Write-Host "Deploy ready: $baseUrl"
        exit 0
    }

    Write-Host "API waiting $j/$MaxApiAttempts code=$code"
    Write-DebugLog -HypothesisId "F" -Location "wait-for-deploy.ps1:poll" -Message "API poll" -Data @{
        attempt = $j
        code = $code
        healthUrl = $healthUrl
        bodySnippet = $bodySnippet
        isHtmlFalsePositive = ($bodySnippet -match '<!doctype html>' -or $bodySnippet -match '<html')
    }

    if ($j -eq $MaxApiAttempts) {
        Write-DebugLog -HypothesisId "B" -Location "wait-for-deploy.ps1" -Message "API timeout" -Data @{ attempts = $j }
        throw "API did not become healthy at $healthUrl"
    }
    Start-Sleep -Seconds 15
}
