param(
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"
$StateFile = Join-Path $PSScriptRoot "deploy-state.json"

if (-not (Test-Path $StateFile)) {
    Write-Host "No deploy state found at $StateFile"
    exit 0
}

$state = Get-Content $StateFile | ConvertFrom-Json
if ($state.instanceId) {
    Write-Host "Terminating instance $($state.instanceId)..."
    aws ec2 terminate-instances --region $Region --instance-ids $state.instanceId | Out-Null
    aws ec2 wait instance-terminated --region $Region --instance-ids $state.instanceId
}

Remove-Item $StateFile
Write-Host "Teardown complete."
