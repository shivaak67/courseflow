param(
    [string]$Region = "us-east-1",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$StateFile = Join-Path $PSScriptRoot "deploy-state.json"

function Get-AwsExe {
    if (Get-Command aws -ErrorAction SilentlyContinue) { return "aws" }
    $full = "C:\Program Files\Amazon\AWSCLIV2\aws.exe"
    if (Test-Path $full) { return $full }
    return $null
}

$AwsExe = Get-AwsExe
if (-not $AwsExe) {
    throw "AWS CLI not found. Install with: winget install Amazon.AWSCLI"
}

$ids = @()
$tagged = & $AwsExe ec2 describe-instances --region $Region `
    --filters "Name=tag:Project,Values=prioritize" "Name=instance-state-name,Values=running,pending,stopping,stopped" `
    --query "Reservations[].Instances[].InstanceId" --output text
if ($tagged -and $tagged -ne "None") {
    $ids = $tagged -split "\s+" | Where-Object { $_ }
    Write-Host "Terminating tagged instance(s): $($ids -join ', ')"
    & $AwsExe ec2 terminate-instances --region $Region --instance-ids $ids | Out-Null
    & $AwsExe ec2 wait instance-terminated --region $Region --instance-ids $ids
}

if (Test-Path $StateFile) {
    $state = Get-Content $StateFile | ConvertFrom-Json
    if ($state.instanceId -and ($ids -notcontains $state.instanceId)) {
        Write-Host "Terminating instance from state file $($state.instanceId)..."
        & $AwsExe ec2 terminate-instances --region $Region --instance-ids $state.instanceId | Out-Null
        & $AwsExe ec2 wait instance-terminated --region $Region --instance-ids $state.instanceId
    }
}

if (Test-Path $StateFile) {
    Remove-Item $StateFile
}
Write-Host "Teardown complete."
