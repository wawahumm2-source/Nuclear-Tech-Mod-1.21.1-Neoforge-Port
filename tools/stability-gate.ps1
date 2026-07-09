param(
    [switch]$SkipGradle,
    [switch]$SkipLogScan
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $root "gradlew.bat"
$validateParity = Join-Path $PSScriptRoot "validate-parity.ps1"
$localJavaCandidates = @(
    (Join-Path $root ".tooling\jdk-21\jdk-21.0.11+10"),
    (Join-Path $root "tools\jdk21-download\jdk-21.0.11+10")
)
$logPaths = @(
    (Join-Path $root "run\logs\latest.log"),
    (Join-Path $root "run\logs\debug.log")
)
$warningPatterns = @(
    "missing texture",
    "Missing texture",
    "missing sound",
    "Missing sound",
    "Unable to load model",
    "Failed to load",
    "Parsing error loading recipe",
    "Exception loading recipe",
    "Attempted to load class net/minecraft/client",
    "NoSuchMethodError",
    "ClassNotFoundException"
)

function Invoke-Step([string]$Name, [scriptblock]$Step) {
    Write-Host "== $Name =="
    & $Step
    Write-Host ""
}

Invoke-Step "Parity resource validation" {
    & $validateParity
}

if (-not $SkipGradle) {
    $localJava = $localJavaCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if ($localJava) {
        $env:JAVA_HOME = $localJava
        $env:PATH = (Join-Path $localJava "bin") + [System.IO.Path]::PathSeparator + $env:PATH
    }

    Invoke-Step "Gradle build" {
        & $gradle build
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE."
        }
    }

    Invoke-Step "Data generation" {
        & $gradle runData
        if ($LASTEXITCODE -ne 0) {
            throw "Data generation failed with exit code $LASTEXITCODE."
        }
    }
}

if (-not $SkipLogScan) {
    Invoke-Step "Runtime log scan" {
        $found = New-Object System.Collections.Generic.List[string]
        foreach ($path in $logPaths) {
            if (-not (Test-Path -LiteralPath $path)) {
                continue
            }
            $lines = Get-Content -LiteralPath $path
            foreach ($line in $lines) {
                if ($line.Contains("Missing sound for event: minecraft:")) {
                    continue
                }
                foreach ($pattern in $warningPatterns) {
                    if ($line.Contains($pattern)) {
                        $found.Add("$path :: $line")
                    }
                }
            }
        }

        if ($found.Count -gt 0) {
            Write-Host "Stability log scan found $($found.Count) warning pattern(s):"
            foreach ($entry in $found) {
                Write-Host " - $entry"
            }
            exit 1
        }

        Write-Host "Stability log scan passed for available logs."
    }
}

Write-Host "Framework stability gate passed."
