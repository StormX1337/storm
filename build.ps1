# ============================================================
#  Storm Client - build without Gradle (Windows / PowerShell)
#
#  Needs nothing but a JDK 17+. No downloads, no wrapper, no
#  network. Produces dist\storm-agent.jar and
#  dist\storm-launcher.jar.
#
#  Usage:  .\build.ps1          build everything
#          .\build.ps1 test     build, then run the smoke test
#
#  If PowerShell refuses to run the script:
#      powershell -ExecutionPolicy Bypass -File .\build.ps1
# ============================================================
param([string]$Task = "build")

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$root = $PSScriptRoot
$out  = Join-Path $root "build-out"
$dist = Join-Path $root "dist"

function Find-Tool($name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$name.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $found = Get-Command $name -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    return $null
}

$javac = Find-Tool "javac"
$java  = Find-Tool "java"
$jar   = Find-Tool "jar"

if (-not $javac) {
    Write-Host "javac not found. Install a JDK 17+ and set JAVA_HOME." -ForegroundColor Red
    Write-Host "  https://adoptium.net/temurin/releases/?version=21"
    exit 1
}

$versionText = (& $javac -version 2>&1) -join " "
$version = [int]([regex]::Match($versionText, "javac (\d+)").Groups[1].Value)
Write-Host "using JDK $version  ($javac)"

$buildLauncher = $true
if ($version -lt 17) {
    Write-Host ""
    Write-Host "  The launcher needs JDK 17 or newer. Found $version." -ForegroundColor Yellow
    Write-Host "  Core and agent will still be built."
    Write-Host ""
    $buildLauncher = $false
}

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

# ---- storm-core ---------------------------------------------------
Write-Host "[1/4] storm-core"
Write-SourceList "$root\storm-core\src\main\java" "$out\core.txt"
& $javac --release 8 -nowarn -d "$out\core" "@$out\core.txt"
if ($LASTEXITCODE -ne 0) { exit 1 }

# ---- storm-agent --------------------------------------------------
Write-Host "[2/4] storm-agent"
Write-SourceList "$root\storm-agent\src\main\java" "$out\agent.txt"
& $javac --release 8 -nowarn -cp "$out\core" -d "$out\agent" "@$out\agent.txt"
if ($LASTEXITCODE -ne 0) { exit 1 }

@"
Premain-Class: xyz.stormclient.agent.StormAgent
Agent-Class: xyz.stormclient.agent.StormAgent
Can-Retransform-Classes: true
Can-Redefine-Classes: true
"@ | Set-Content -Path "$out\agent-manifest.txt" -Encoding ASCII

Copy-Item "$out\core\*" -Destination "$out\agent" -Recurse -Force
& $jar --create --file "$dist\storm-agent.jar" --manifest "$out\agent-manifest.txt" -C "$out\agent" .
if ($LASTEXITCODE -ne 0) { exit 1 }

# ---- storm-launcher -----------------------------------------------
if ($buildLauncher) {
    Write-Host "[3/4] storm-launcher"
    Write-SourceList "$root\storm-launcher\src\main\java" "$out\launcher.txt"
    & $javac --release 17 -nowarn -cp "$out\core" -d "$out\launcher" "@$out\launcher.txt"
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Copy-Item "$out\core\*" -Destination "$out\launcher" -Recurse -Force
    & $jar --create --file "$dist\storm-launcher.jar" `
           --main-class xyz.stormclient.launcher.StormLauncher -C "$out\launcher" .
    if ($LASTEXITCODE -ne 0) { exit 1 }
} else {
    Write-Host "[3/4] storm-launcher  SKIPPED (needs JDK 17+)"
}

# ---- smoke test ---------------------------------------------------
if ($Task -eq "test") {
    Write-Host "[4/4] smoke test"
    Write-SourceList "$root\storm-core\src\test\java" "$out\test.txt"
    & $javac --release 8 -nowarn -cp "$out\core" -d "$out\test" "@$out\test.txt"
    if ($LASTEXITCODE -ne 0) { exit 1 }
    & $java -cp "$out\core;$out\test" xyz.stormclient.test.StormSmokeTest
} else {
    Write-Host "[4/4] smoke test      skipped (run .\build.ps1 test to include it)"
}

Write-Host ""
Write-Host "done. jars are in dist\" -ForegroundColor Green
Get-ChildItem $dist | ForEach-Object { Write-Host "  $($_.Name)" }
Write-Host ""
Write-Host "start the launcher with:"
Write-Host "  java -jar dist\storm-launcher.jar"
