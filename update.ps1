# ============================================================
#  Storm - update without git
#
#  Downloads the current branch from GitHub and replaces the
#  sources in this folder. Your dist\ and build-out\ folders and
#  anything else you added are left alone.
#
#  Usage:  .\update.ps1           update the sources
#          .\update.ps1 build     update, then build
#
#  If you have git, none of this is needed:
#      git pull
# ============================================================
param([string]$Task = "update")

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path (Join-Path $PSScriptRoot "settings.gradle"))) {
    Write-Host "this script has to sit in the Storm folder, next to settings.gradle" -ForegroundColor Red
    exit 1
}

$repo   = "StormX1337/storm"
$branch = "claude/charming-archimedes-400dsx"
$url    = "https://github.com/$repo/archive/refs/heads/$branch.zip"

# folders that belong to you, not to the repository
$keep = @("dist", "build-out", ".gradle", "build", ".git")

Write-Host "Storm updater" -ForegroundColor Cyan
Write-Host "  repo   $repo"
Write-Host "  branch $branch"
Write-Host ""

if (Get-Command git -ErrorAction SilentlyContinue) {
    if (Test-Path (Join-Path $PSScriptRoot ".git")) {
        Write-Host "this is a git checkout, using git pull instead" -ForegroundColor Yellow
        git pull
        if ($Task -eq "build") { & "$PSScriptRoot\build.ps1" test }
        exit $LASTEXITCODE
    }
}

$temp = Join-Path $env:TEMP ("storm-update-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $temp -Force | Out-Null
$zip = Join-Path $temp "storm.zip"

try {
    Write-Host "downloading..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing

    Write-Host "extracting..."
    Expand-Archive -Path $zip -DestinationPath $temp -Force

    $extracted = Get-ChildItem $temp -Directory | Select-Object -First 1
    if (-not $extracted) { throw "the archive was empty" }

    Write-Host "replacing sources..."
    $copied = 0
    Get-ChildItem $extracted.FullName -Force | ForEach-Object {
        if ($keep -contains $_.Name) { return }
        $target = Join-Path $PSScriptRoot $_.Name
        if (Test-Path $target) { Remove-Item $target -Recurse -Force }
        Move-Item $_.FullName $target -Force
        $copied++
    }

    Write-Host ""
    Write-Host "updated, $copied entries replaced" -ForegroundColor Green
} finally {
    if (Test-Path $temp) { Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue }
}

if ($Task -eq "build") {
    Write-Host ""
    & "$PSScriptRoot\build.ps1" test
} else {
    Write-Host "run .\build.ps1 test next"
}

Write-Host ""
Write-Host "next steps" -ForegroundColor Cyan
Write-Host "  start the launcher   java -jar dist\storm-launcher.jar"
Write-Host "  build the bridge     cd storm-bridge-1.8.9\workspace ; .\build-bridge.ps1"
