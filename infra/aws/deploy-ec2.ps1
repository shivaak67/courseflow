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
    [string]$RepoBranch = "develop"
)

$ErrorActionPreference = "Stop"
$ProjectName = "prioritize"
$StateFile = Join-Path $PSScriptRoot "deploy-state.json"

function Require-AwsCli {
    if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
        throw "AWS CLI not found. Install with: winget install Amazon.AWSCLI"
    }
    aws sts get-caller-identity --region $Region | Out-Null
}

function Get-DefaultVpcId {
    $vpcs = aws ec2 describe-vpcs --region $Region --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text
    if (-not $vpcs -or $vpcs -eq "None") {
        throw "No default VPC found in $Region. Create one or deploy manually."
    }
    return $vpcs
}

function Ensure-SecurityGroup {
    param([string]$VpcId)
    $sgName = "$ProjectName-http"
    $existing = aws ec2 describe-security-groups --region $Region --filters "Name=group-name,Values=$sgName" "Name=vpc-id,Values=$VpcId" --query "SecurityGroups[0].GroupId" --output text
    if ($existing -and $existing -ne "None") {
        return $existing
    }
    $sgId = aws ec2 create-security-group --region $Region --group-name $sgName --description "Prioritize HTTP" --vpc-id $VpcId --query "GroupId" --output text
    aws ec2 authorize-security-group-ingress --region $Region --group-id $sgId --protocol tcp --port 80 --cidr 0.0.0.0/0 | Out-Null
    return $sgId
}

function Get-LatestAmazonLinuxAmi {
    aws ec2 describe-images --region $Region `
        --owners amazon `
        --filters "Name=name,Values=al2023-ami-2023*-x86_64" "Name=state,Values=available" `
        --query "sort_by(Images, &CreationDate)[-1].ImageId" `
        --output text
}

Require-AwsCli

if (Test-Path $StateFile) {
    $state = Get-Content $StateFile | ConvertFrom-Json
    if ($state.instanceId) {
        $status = aws ec2 describe-instances --region $Region --instance-ids $state.instanceId --query "Reservations[0].Instances[0].State.Name" --output text
        if ($status -eq "running" -or $status -eq "pending") {
            Write-Host "Instance already running: $($state.instanceId)"
            Write-Host "App URL: http://$($state.publicIp)"
            exit 0
        }
    }
}

$vpcId = Get-DefaultVpcId
$sgId = Ensure-SecurityGroup -VpcId $vpcId
$amiId = Get-LatestAmazonLinuxAmi
$userDataPath = Join-Path $PSScriptRoot "user-data.sh"
$userData = Get-Content $userDataPath -Raw
$userData = $userData.Replace('${PRIORITIZE_REPO_URL:-https://github.com/shivaak67/courseflow.git}', $RepoUrl)
$userData = $userData.Replace('${PRIORITIZE_REPO_BRANCH:-develop}', $RepoBranch)
$userDataB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($userData))

$launchArgs = @(
    "ec2", "run-instances",
    "--region", $Region,
    "--image-id", $amiId,
    "--instance-type", $InstanceType,
    "--security-group-ids", $sgId,
    "--user-data", $userDataB64,
    "--tag-specifications", "ResourceType=instance,Tags=[{Key=Name,Value=$ProjectName},{Key=Project,Value=$ProjectName}]",
    "--query", "Instances[0].InstanceId",
    "--output", "text"
)
if ($KeyName) {
    $launchArgs += @("--key-name", $KeyName)
}

Write-Host "Launching $InstanceType in $Region (free-tier eligible)..."
$instanceId = aws @launchArgs
Write-Host "Instance $instanceId launched. Waiting for public IP..."

aws ec2 wait instance-running --region $Region --instance-ids $instanceId
Start-Sleep -Seconds 5
$publicIp = aws ec2 describe-instances --region $Region --instance-ids $instanceId --query "Reservations[0].Instances[0].PublicIpAddress" --output text

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
