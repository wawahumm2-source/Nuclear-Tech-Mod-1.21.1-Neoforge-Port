param(
    [ValidateRange(15, 180)]
    [int]$TimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'gradle-bootstrap.ps1')
$gradle = Get-HbmGradleCommand -ProjectRoot $root
$verificationDirectory = Join-Path $root "build\verification"
$gameDirectory = Join-Path $root "run-server-smoke"
$serverProperties = Join-Path $gameDirectory "server.properties"
$stdout = Join-Path $verificationDirectory "server-smoke.stdout.log"
$stderr = Join-Path $verificationDirectory "server-smoke.stderr.log"
$localJavaCandidates = @(
    (Join-Path $root ".tooling\jdk-21\jdk-21.0.11+10"),
    (Join-Path $root "tools\jdk21-download\jdk-21.0.11+10")
)

New-Item -ItemType Directory -Force -Path $verificationDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $gameDirectory | Out-Null
Set-Content -LiteralPath $stdout -Value ""
Set-Content -LiteralPath $stderr -Value ""

$properties = if (Test-Path -LiteralPath $serverProperties) { Get-Content -LiteralPath $serverProperties } else { @() }
$properties = $properties | Where-Object {
    $_ -notmatch '^(server-port|view-distance|simulation-distance|sync-chunk-writes)='
}
$properties += 'server-port=0'
$properties += 'view-distance=4'
$properties += 'simulation-distance=4'
$properties += 'sync-chunk-writes=false'
Set-Content -LiteralPath $serverProperties -Value $properties

$localJava = $localJavaCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if ($localJava) {
    $env:JAVA_HOME = $localJava
    $env:PATH = (Join-Path $localJava "bin") + [System.IO.Path]::PathSeparator + $env:PATH
}

$command = "`"$gradle`" --no-daemon runServerSmoke"
$process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/c', $command) -WorkingDirectory $root -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru -NoNewWindow
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$ready = $false

try {
    while ((Get-Date) -lt $deadline) {
        if (Select-String -LiteralPath $stdout -Pattern 'Done \(.+\)!' -Quiet -ErrorAction SilentlyContinue) {
            $ready = $true
            break
        }
        if ($process.HasExited) {
            break
        }
        Start-Sleep -Milliseconds 500
    }
} finally {
    if (-not $process.HasExited) {
        & taskkill.exe /PID $process.Id /T /F | Out-Null
    }
}

if (-not $ready) {
    Write-Host "Dedicated-server smoke test did not reach the ready state within $TimeoutSeconds seconds."
    Get-Content -LiteralPath $stdout -Tail 80 -ErrorAction SilentlyContinue
    Get-Content -LiteralPath $stderr -Tail 80 -ErrorAction SilentlyContinue
    exit 1
}

Write-Host "Dedicated-server smoke test passed. Fresh log: $stdout"
