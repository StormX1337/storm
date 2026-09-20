# ============================================================
#  Storm - build the 1.8.9 bridge
#
#  ForgeGradle 2.3 is the last toolchain that deobfuscates
#  1.8.9 and it only runs on Java 8, so this script finds a
#  JDK 8 on the machine, builds with it, and copies the result
#  next to the agent.
#
#  Usage:  .\build-bridge.ps1
#          .\build-bridge.ps1 clean     rebuild from scratch
# ============================================================
param([string]$Task = "build")

$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot

function Get-JavacVersion($javacPath) {
    $text = (& $javacPath -version 2>&1 | Out-String)
    $match = [regex]::Match($text, 'javac\s+(\d+)(?:\.(\d+))?')
    if (-not $match.Success) { return 0 }

    $major = [int]$match.Groups[1].Value
    if ($major -eq 1 -and $match.Groups[2].Success) { $major = [int]$match.Groups[2].Value }
    return $major
}

function Find-Jdks {
    $found = @()

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\javac.exe"
        if (Test-Path $candidate) { $found += $candidate }
    }
    $onPath = Get-Command javac -ErrorAction SilentlyContinue
    if ($onPath) { $found += $onPath.Source }

    $roots = @(
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Eclipse Foundation",
        "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\Amazon Corretto",
        "$env:ProgramFiles\Zulu",
        "$env:ProgramFiles\BellSoft",
        "$env:ProgramFiles\Semeru",
        "${env:ProgramFiles(x86)}\Java",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium",
        "$env:LOCALAPPDATA\Programs\Java"
    )
    foreach ($dir in $roots) {
        if (-not $dir -or -not (Test-Path $dir)) { continue }
        Get-ChildItem $dir -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            $candidate = Join-Path $_.FullName "bin\javac.exe"
            if (Test-Path $candidate) { $found += $candidate }
        }
    }
    return $found | Select-Object -Unique
}

Write-Host "looking for a JDK 8" -ForegroundColor Cyan
$candidates = Find-Jdks
$java8 = $null

foreach ($candidate in $candidates) {
    $version = Get-JavacVersion $candidate
    # not $home: that is a read-only automatic variable in PowerShell, and
    # assigning to it fails while leaving the user profile path in place
    $jdkHome = Split-Path (Split-Path $candidate -Parent) -Parent
    $mark = if ($version -eq 8 -and -not $java8) { "  <- using this" } else { "" }
    Write-Host ("  JDK {0,-3} {1}{2}" -f $version, $jdkHome, $mark) -ForegroundColor DarkGray
    if ($version -eq 8 -and -not $java8) { $java8 = $jdkHome }
}

if (-not $java8) {
    Write-Host ""
    Write-Host "No JDK 8 found, and ForgeGradle 2.3 cannot run on anything newer." -ForegroundColor Red
    Write-Host ""
    Write-Host "Install one:"
    Write-Host "  winget install EclipseAdoptium.Temurin.8.JDK" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "A JRE is not enough, it has to be a JDK (bin\javac.exe must exist)."
    exit 1
}

$javaExe = Join-Path $java8 "bin\java.exe"
if (-not (Test-Path $javaExe)) {
    Write-Host ""
    Write-Host "$java8 does not look like a JDK, bin\java.exe is missing" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "building with $java8" -ForegroundColor Green
Write-Host "the first run downloads and deobfuscates Minecraft, which takes a while"
Write-Host ""

# only for this process, the shell keeps whatever it had
$env:JAVA_HOME = $java8

if ($Task -eq "clean") {
    & .\gradlew.bat clean
}

& .\gradlew.bat setupDecompWorkspace --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "setupDecompWorkspace failed, see docs\BRIDGE-BUILD.md" -ForegroundColor Red
    exit 1
}

& .\gradlew.bat build --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "build failed, see docs\BRIDGE-BUILD.md" -ForegroundColor Red
    exit 1
}

$jar = Join-Path $PSScriptRoot "build\libs\storm-bridge-1.8.9.jar"
if (-not (Test-Path $jar)) {
    Write-Host "the build reported success but produced no jar" -ForegroundColor Red
    exit 1
}

$dist = Join-Path $PSScriptRoot "..\..\dist"
if (-not (Test-Path $dist)) { New-Item -ItemType Directory -Path $dist -Force | Out-Null }
Copy-Item $jar $dist -Force

Write-Host ""
Write-Host "done." -ForegroundColor Green
Write-Host "  $jar"
Write-Host "  copied to dist\storm-bridge-1.8.9.jar"
Write-Host ""
Write-Host "the Play page's bridge row should be green now."
