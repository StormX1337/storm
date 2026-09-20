# ============================================================
#  Storm Client - build without Gradle (Windows / PowerShell)
#
#  Needs a JDK 17+. No downloads, no wrapper, no network.
#  Produces dist\storm-agent.jar and dist\storm-launcher.jar.
#
#  Usage:  .\build.ps1          build everything
#          .\build.ps1 test     build, then run the smoke test
#
#  If PowerShell refuses to run the script:
#      powershell -ExecutionPolicy Bypass -File .\build.ps1
# ============================================================
param([string]$Task = "build")

# javac writes to stderr even when it succeeds, so native commands must not
# be treated as terminating errors. Every call checks $LASTEXITCODE instead.
$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot

$root = $PSScriptRoot
$out  = Join-Path $root "build-out"
$dist = Join-Path $root "dist"

# ------------------------------------------------------------------
#  find a JDK
# ------------------------------------------------------------------
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
        "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\Amazon Corretto",
        "$env:ProgramFiles\Zulu",
        "$env:ProgramFiles\BellSoft",
        "$env:ProgramFiles\Semeru",
        "${env:ProgramFiles(x86)}\Java",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
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

$candidates = Find-Jdks
if ($candidates.Count -eq 0) {
    Write-Host "No JDK found." -ForegroundColor Red
    Write-Host ""
    Write-Host "Install one, the quickest way is:"
    Write-Host "  winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Cyan
    Write-Host "Then open a new terminal and run this script again."
    exit 1
}

$best = $null
$bestVersion = 0
foreach ($candidate in $candidates) {
    $version = Get-JavacVersion $candidate
    Write-Host ("  found JDK {0,-3} {1}" -f $version, $candidate) -ForegroundColor DarkGray
    if ($version -gt $bestVersion) {
        $bestVersion = $version
        $best = $candidate
    }
}

$javac = $best
$javaHome = Split-Path (Split-Path $javac -Parent) -Parent
$java = Join-Path $javaHome "bin\java.exe"
$jar  = Join-Path $javaHome "bin\jar.exe"
Write-Host ""
Write-Host "using JDK $bestVersion  ($javac)" -ForegroundColor Green

if ($bestVersion -lt 17) {
    Write-Host ""
    Write-Host "  This build needs JDK 17 or newer, the newest one here is $bestVersion." -ForegroundColor Red
    Write-Host ""
    Write-Host "  Install a current JDK:"
    Write-Host "    winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Open a NEW terminal afterwards, or point this session at it:"
    Write-Host '    $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  The path above is an example, use whatever winget installed."
    exit 1
}

if (-not (Test-Path $jar)) {
    Write-Host "jar.exe missing next to javac. That is a JRE, not a JDK." -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------
#  build
# ------------------------------------------------------------------
if (Test-Path $out)  { Remove-Item $out -Recurse -Force }
if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }
foreach ($dir in @("core", "agent", "launcher", "test")) {
    New-Item -ItemType Directory -Path (Join-Path $out $dir) -Force | Out-Null
}
New-Item -ItemType Directory -Path $dist -Force | Out-Null

function Write-SourceList($sourceDir, $listFile) {
    Get-ChildItem -Path $sourceDir -Recurse -Filter *.java |
        ForEach-Object { $_.FullName } |
        Set-Content -Path $listFile -Encoding UTF8
}

function Invoke-Step($name, $scriptBlock) {
    Write-Host $name
    & $scriptBlock
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  failed" -ForegroundColor Red
        exit 1
    }
}

Invoke-Step "[1/4] storm-core" {
    Write-SourceList "$root\storm-core\src\main\java" "$out\core.txt"
    & $javac --release 8 -nowarn -d "$out\core" "@$out\core.txt"
}

Invoke-Step "[2/4] storm-agent" {
    Write-SourceList "$root\storm-agent\src\main\java" "$out\agent.txt"
    & $javac --release 8 -nowarn -cp "$out\core" -d "$out\agent" "@$out\agent.txt"
}

@"
Premain-Class: xyz.stormclient.agent.StormAgent
Agent-Class: xyz.stormclient.agent.StormAgent
Can-Retransform-Classes: true
Can-Redefine-Classes: true
"@ | Set-Content -Path "$out\agent-manifest.txt" -Encoding ASCII

Copy-Item "$out\core\*" -Destination "$out\agent" -Recurse -Force
Invoke-Step "      packing storm-agent.jar" {
    & $jar --create --file "$dist\storm-agent.jar" --manifest "$out\agent-manifest.txt" -C "$out\agent" .
}

Invoke-Step "[3/4] storm-launcher" {
    Write-SourceList "$root\storm-launcher\src\main\java" "$out\launcher.txt"
    & $javac --release 17 -nowarn -cp "$out\core" -d "$out\launcher" "@$out\launcher.txt"
}

Copy-Item "$out\core\*" -Destination "$out\launcher" -Recurse -Force
Invoke-Step "      packing storm-launcher.jar" {
    & $jar --create --file "$dist\storm-launcher.jar" `
           --main-class xyz.stormclient.launcher.StormLauncher -C "$out\launcher" .
}

if ($Task -eq "test") {
    Invoke-Step "[4/4] smoke test" {
        Write-SourceList "$root\storm-core\src\test\java" "$out\test.txt"
        & $javac --release 8 -nowarn -cp "$out\core" -d "$out\test" "@$out\test.txt"
    }
    & $java -cp "$out\core;$out\test" xyz.stormclient.test.StormSmokeTest
    if ($LASTEXITCODE -ne 0) { exit 1 }
} else {
    Write-Host "[4/4] smoke test      skipped (run .\build.ps1 test to include it)"
}

Write-Host ""
Write-Host "done." -ForegroundColor Green
Get-ChildItem $dist | ForEach-Object { Write-Host "  dist\$($_.Name)" }
Write-Host ""
Write-Host "start the launcher with:"
Write-Host "  & `"$java`" -jar dist\storm-launcher.jar" -ForegroundColor Cyan
