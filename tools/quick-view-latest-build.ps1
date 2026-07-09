param(
    [switch]$InfoOnly,
    [switch]$RebuildFirst
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHome = Join-Path $projectRoot ".tooling\jdk-21\jdk-21.0.11+10"
$javaExe = Join-Path $javaHome "bin\java.exe"
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$buildLibs = Join-Path $projectRoot "build\libs"

function Format-FileSize {
    param([long]$Bytes)

    if ($Bytes -ge 1GB) {
        return "{0:N2} GB" -f ($Bytes / 1GB)
    }
    if ($Bytes -ge 1MB) {
        return "{0:N2} MB" -f ($Bytes / 1MB)
    }
    if ($Bytes -ge 1KB) {
        return "{0:N2} KB" -f ($Bytes / 1KB)
    }
    return "$Bytes bytes"
}

function Get-LatestBuildJar {
    if (!(Test-Path $buildLibs)) {
        return $null
    }

    return Get-ChildItem $buildLibs -Filter "*.jar" |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

if (!(Test-Path $javaExe)) {
    Write-Host "Java 21 was not found at:" -ForegroundColor Red
    Write-Host "  $javaExe"
    exit 1
}

if (!(Test-Path $gradleWrapper)) {
    Write-Host "Gradle wrapper was not found at:" -ForegroundColor Red
    Write-Host "  $gradleWrapper"
    exit 1
}

$latestJar = Get-LatestBuildJar

Write-Host ""
Write-Host "HBM Nuclear Tech - Quick View Latest Build" -ForegroundColor Cyan
Write-Host "Project: $projectRoot"
Write-Host "Java:    $javaHome"

if ($latestJar) {
    Write-Host "Latest:  $($latestJar.Name) ($(Format-FileSize $latestJar.Length), $($latestJar.LastWriteTime))"
} else {
    Write-Host "Latest:  No jar found yet. A build will be needed." -ForegroundColor Yellow
}

if ($InfoOnly) {
    exit 0
}

Set-Location $projectRoot
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

if ($RebuildFirst -or !$latestJar) {
    Write-Host ""
    Write-Host "Building latest jar..." -ForegroundColor Cyan
    & $gradleWrapper build
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Write-Host ""
Write-Host "Launching Minecraft client with the current workspace build..." -ForegroundColor Cyan
Write-Host "Close Minecraft when you are finished reviewing the build."
Write-Host ""

& $gradleWrapper runClient
exit $LASTEXITCODE
