# AWS free-tier EC2 deploy (single t2.micro, ~$0/month for 12 months)
#
# Prerequisites:
#   1. AWS account with free tier available
#   2. AWS CLI v2 installed and configured (`aws configure`)
#   3. This repo pushed to GitHub (default: shivaak67/courseflow, branch develop)
#
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File infra/aws/deploy-ec2.ps1
#
# What it creates:
#   - EC2 t2.micro (Amazon Linux 2023) in us-east-1
#   - Security group allowing HTTP (80) from anywhere
#   - Docker Compose prod stack (Postgres + backend + nginx frontend)
#
# Teardown:
#   powershell -ExecutionPolicy Bypass -File infra/aws/teardown-ec2.ps1

param(
    [string]$Region = "us-east-1",
    [string]$InstanceType = "t2.micro",
    [string]$KeyName = "",
    [string]$RepoUrl = "https://github.com/shivaak67/courseflow.git",
    [string]$RepoBranch = "develop",
    [switch]$Replace,
    [switch]$BuildFrontend,
    [switch]$BuildBackend
)

$ErrorActionPreference = "Stop"
$ProjectName = "prioritize"
$StateFile = Join-Path $PSScriptRoot "deploy-state.json"

function Get-AwsExe {
    if (Get-Command aws -ErrorAction SilentlyContinue) { return "aws" }
    $full = "C:\Program Files\Amazon\AWSCLIV2\aws.exe"
    if (Test-Path $full) { return $full }
    return $null
}

function Require-AwsCli {
    $script:AwsExe = Get-AwsExe
    if (-not $script:AwsExe) {
        throw "AWS CLI not found. Install with: winget install Amazon.AWSCLI"
    }
    & $script:AwsExe sts get-caller-identity --region $Region | Out-Null
}

function Get-DefaultVpcId {
    $vpcs = & $script:AwsExe ec2 describe-vpcs --region $Region --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text
    if (-not $vpcs -or $vpcs -eq "None") {
        throw "No default VPC found in $Region. Create one or deploy manually."
    }
    return $vpcs
}

function Ensure-SecurityGroup {
    param([string]$VpcId)
    $sgName = "$ProjectName-http"
    $existing = & $script:AwsExe ec2 describe-security-groups --region $Region --filters "Name=group-name,Values=$sgName" "Name=vpc-id,Values=$VpcId" --query "SecurityGroups[0].GroupId" --output text
    if ($existing -and $existing -ne "None") {
        return $existing
    }
    $sgId = & $script:AwsExe ec2 create-security-group --region $Region --group-name $sgName --description "Prioritize HTTP" --vpc-id $VpcId --query "GroupId" --output text
    & $script:AwsExe ec2 authorize-security-group-ingress --region $Region --group-id $sgId --protocol tcp --port 80 --cidr 0.0.0.0/0 | Out-Null
    return $sgId
}

function Get-LatestAmazonLinuxAmi {
    & $script:AwsExe ec2 describe-images --region $Region `
        --owners amazon `
        --filters "Name=name,Values=al2023-ami-2023*-x86_64" "Name=state,Values=available" `
        --query "sort_by(Images, &CreationDate)[-1].ImageId" `
        --output text
}

function Read-EnvFile {
    param([string]$Path)
    $result = @{}
    if (-not (Test-Path $Path)) {
        return $result
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -lt 1) { return }
        $key = $line.Substring(0, $eq).Trim()
        $val = $line.Substring($eq + 1).Trim()
        $result[$key] = $val
    }
    return $result
}

function Stop-ExistingInstances {
    $existing = & $script:AwsExe ec2 describe-instances --region $Region `
        --filters "Name=tag:Project,Values=$ProjectName" "Name=instance-state-name,Values=running,pending,stopping" `
        --query "Reservations[].Instances[].InstanceId" --output text
    if ($existing -and $existing -ne "None") {
        $ids = $existing -split "\s+" | Where-Object { $_ }
        if ($ids.Count -gt 0) {
            Write-Host "Terminating existing instance(s): $($ids -join ', ')"
            & $script:AwsExe ec2 terminate-instances --region $Region --instance-ids $ids | Out-Null
            & $script:AwsExe ec2 wait instance-terminated --region $Region --instance-ids $ids
            if (Test-Path $StateFile) { Remove-Item $StateFile }
        }
    }
}

Require-AwsCli

if ($Replace) {
    Stop-ExistingInstances
}

if (Test-Path $StateFile) {
    $state = Get-Content $StateFile | ConvertFrom-Json
    if ($state.instanceId) {
        $status = & $script:AwsExe ec2 describe-instances --region $Region --instance-ids $state.instanceId --query "Reservations[0].Instances[0].State.Name" --output text
        if ($status -eq "running" -or $status -eq "pending") {
            Write-Host "Instance already running: $($state.instanceId)"
            Write-Host "App URL: http://$($state.publicIp)"
            Write-Host "Use -Replace to redeploy with updated user-data."
            exit 0
        }
    }
}

$vpcId = Get-DefaultVpcId
$sgId = Ensure-SecurityGroup -VpcId $vpcId
$amiId = Get-LatestAmazonLinuxAmi
$userDataPath = Join-Path $PSScriptRoot "user-data.sh"
$userData = (Get-Content $userDataPath -Raw) -replace "`r`n", "`n"
$userData = $userData.Replace('${PRIORITIZE_REPO_URL:-https://github.com/shivaak67/courseflow.git}', $RepoUrl)
$userData = $userData.Replace('${PRIORITIZE_REPO_BRANCH:-develop}', $RepoBranch)

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$localEnv = Read-EnvFile (Join-Path $repoRoot ".env")
$googleEnabled = if ($localEnv.ContainsKey('GOOGLE_OAUTH_ENABLED')) { $localEnv['GOOGLE_OAUTH_ENABLED'] } else { 'false' }
$googleClientId = if ($googleEnabled -eq 'true' -and $localEnv.ContainsKey('GOOGLE_CLIENT_ID') -and $localEnv['GOOGLE_CLIENT_ID']) {
    $localEnv['GOOGLE_CLIENT_ID']
} else {
    'disabled'
}
$googleClientSecret = if ($googleEnabled -eq 'true' -and $localEnv.ContainsKey('GOOGLE_CLIENT_SECRET') -and $localEnv['GOOGLE_CLIENT_SECRET']) {
    $localEnv['GOOGLE_CLIENT_SECRET']
} else {
    'disabled'
}
$userData = $userData.Replace('${PRIORITIZE_GOOGLE_OAUTH_ENABLED:-false}', $googleEnabled)
$userData = $userData.Replace('${PRIORITIZE_GOOGLE_CLIENT_ID:-disabled}', $googleClientId)
$userData = $userData.Replace('${PRIORITIZE_GOOGLE_CLIENT_SECRET:-disabled}', $googleClientSecret)

$appPublicUrl = if ($localEnv.ContainsKey('APP_PUBLIC_URL') -and $localEnv['APP_PUBLIC_URL']) {
    $localEnv['APP_PUBLIC_URL']
} else {
    'https://theprioritize.com'
}
$userData = $userData.Replace('${PRIORITIZE_APP_PUBLIC_URL:-}', $appPublicUrl)

$githubToken = if ($localEnv.ContainsKey('GITHUB_TOKEN') -and $localEnv['GITHUB_TOKEN']) {
    $localEnv['GITHUB_TOKEN']
} else { '' }
$userData = $userData.Replace('${PRIORITIZE_GITHUB_TOKEN:-}', $githubToken)

$aiEnabled = if ($localEnv.ContainsKey('AI_ENABLED') -and $localEnv['AI_ENABLED']) { $localEnv['AI_ENABLED'] } else { 'false' }
$aiApiKey = if ($aiEnabled -eq 'true' -and $localEnv.ContainsKey('AI_API_KEY') -and $localEnv['AI_API_KEY']) {
    $localEnv['AI_API_KEY']
} else {
    ''
}
$aiModel = if ($localEnv.ContainsKey('AI_MODEL') -and $localEnv['AI_MODEL']) { $localEnv['AI_MODEL'] } else { 'gpt-4o-mini' }
$aiBaseUrl = if ($localEnv.ContainsKey('AI_BASE_URL') -and $localEnv['AI_BASE_URL']) { $localEnv['AI_BASE_URL'] } else { 'https://api.openai.com/v1' }
$aiWarmupEnabled = if ($localEnv.ContainsKey('AI_WARMUP_ENABLED') -and $localEnv['AI_WARMUP_ENABLED']) { $localEnv['AI_WARMUP_ENABLED'] } else { 'false' }
$userData = $userData.Replace('${PRIORITIZE_AI_ENABLED:-false}', $aiEnabled)
$userData = $userData.Replace('${PRIORITIZE_AI_API_KEY:-}', $aiApiKey)
$userData = $userData.Replace('${PRIORITIZE_AI_MODEL:-gpt-4o-mini}', $aiModel)
$userData = $userData.Replace('${PRIORITIZE_AI_BASE_URL:-https://api.openai.com/v1}', $aiBaseUrl)
$userData = $userData.Replace('${PRIORITIZE_AI_WARMUP_ENABLED:-false}', $aiWarmupEnabled)

$shouldBuildFrontend = if ($BuildFrontend) { 'true' } elseif ($localEnv.ContainsKey('PRIORITIZE_BUILD_FRONTEND') -and $localEnv['PRIORITIZE_BUILD_FRONTEND'] -eq 'true') { 'true' } else { 'false' }
$shouldBuildBackend = if ($BuildBackend) { 'true' } elseif ($localEnv.ContainsKey('PRIORITIZE_BUILD_BACKEND') -and $localEnv['PRIORITIZE_BUILD_BACKEND'] -eq 'true') { 'true' } else { 'false' }
$userData = $userData.Replace('${PRIORITIZE_BUILD_FRONTEND:-false}', $shouldBuildFrontend)
$userData = $userData.Replace('${PRIORITIZE_BUILD_BACKEND:-false}', $shouldBuildBackend)

Write-Host "App public URL: $appPublicUrl"

if ($googleEnabled -eq 'true') {
    Write-Host "Google OAuth: enabled (credentials from local .env)"
} else {
    Write-Host "Google OAuth: disabled (set GOOGLE_OAUTH_ENABLED=true in .env to enable on EC2)"
}

if ($aiEnabled -eq 'true' -and $aiApiKey) {
    Write-Host "AI assistant: enabled (API key from local .env)"
} else {
    Write-Host "AI assistant: disabled (set AI_ENABLED=true and AI_API_KEY in .env to enable on EC2)"
}

$userDataB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($userData))

$launchArgs = @(
    "ec2", "run-instances",
    "--region", $Region,
    "--image-id", $amiId,
    "--instance-type", $InstanceType,
    "--security-group-ids", $sgId,
    "--user-data", $userDataB64,
    "--metadata-options", "HttpEndpoint=enabled,HttpTokens=optional",
    "--tag-specifications", "ResourceType=instance,Tags=[{Key=Name,Value=$ProjectName},{Key=Project,Value=$ProjectName}]",
    "--query", "Instances[0].InstanceId",
    "--output", "text"
)
if ($KeyName) {
    $launchArgs += @("--key-name", $KeyName)
}

Write-Host "Launching $InstanceType in $Region (free-tier eligible)..."
$instanceId = & $script:AwsExe @launchArgs
Write-Host "Instance $instanceId launched. Waiting for public IP..."

& $script:AwsExe ec2 wait instance-running --region $Region --instance-ids $instanceId
Start-Sleep -Seconds 5

$elasticIp = if ($localEnv.ContainsKey('PRIORITIZE_ELASTIC_IP') -and $localEnv['PRIORITIZE_ELASTIC_IP']) {
    $localEnv['PRIORITIZE_ELASTIC_IP']
} else { $null }
if ($elasticIp) {
    $allocId = & $script:AwsExe ec2 describe-addresses --region $Region --public-ips $elasticIp --query "Addresses[0].AllocationId" --output text 2>$null
    if ($allocId -and $allocId -ne "None") {
        Write-Host "Associating Elastic IP $elasticIp with instance $instanceId..."
        & $script:AwsExe ec2 associate-address --region $Region --instance-id $instanceId --allocation-id $allocId | Out-Null
        Start-Sleep -Seconds 3
    } else {
        Write-Host "Warning: could not find allocation for Elastic IP $elasticIp - associate manually in EC2 console."
    }
}

$publicIp = & $script:AwsExe ec2 describe-instances --region $Region --instance-ids $instanceId --query "Reservations[0].Instances[0].PublicIpAddress" --output text

@{
    instanceId = $instanceId
    publicIp = $publicIp
    region = $Region
    securityGroupId = $sgId
    launchedAt = (Get-Date).ToString("o")
} | ConvertTo-Json | Set-Content $StateFile

Write-Host ""
Write-Host "EC2 instance is up. Docker build may take 5-10 minutes on first boot."
Write-Host "App URL: http://$publicIp"
Write-Host "State saved to $StateFile"
Write-Host ""
Write-Host "Free-tier notes:"
Write-Host "  - t2.micro: 750 hours/month free for 12 months (one instance = always on)"
Write-Host "  - No RDS/ALB/Elastic IP used to avoid charges"
Write-Host "  - Stop the instance when not demoing to save free-tier hours"
