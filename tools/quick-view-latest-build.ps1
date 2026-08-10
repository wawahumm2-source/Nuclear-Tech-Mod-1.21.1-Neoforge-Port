param(
    [switch]$InfoOnly,
    [switch]$RebuildFirst
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$buildLibs = Join-Path $projectRoot "build\libs"
$comparisonGameDirectory = Join-Path $projectRoot "run-client-comparison"
$comparisonModsDirectory = Join-Path $comparisonGameDirectory "mods"
$managedSuperbJar = Join-Path $comparisonModsDirectory "superbwarfare-0.8.9-final-mc1.21.1-9b5284f4.jar"
$expectedSuperbHash = "1a648d19104ef8c485c539edaf4795553c6ee8d5a624fb59aca5d86f4b0297c9"

function Test-Java21 {
    param([string]$JavaPath)

    if ([string]::IsNullOrWhiteSpace($JavaPath) -or !(Test-Path -LiteralPath $JavaPath -PathType Leaf)) {
        return $false
    }

    $previousErrorPreference = $ErrorActionPreference
    try {
        # java -version writes its normal version banner to stderr.
        $ErrorActionPreference = "Continue"
        $versionText = (& $JavaPath -version 2>&1 | Out-String)
        return $versionText -match '\bversion\s+"21(?:\.|"|\s)'
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $previousErrorPreference
    }
}

function Find-Java21 {
    $candidates = [System.Collections.Generic.List[string]]::new()

    $localJdkRoot = Join-Path $projectRoot ".tooling\jdk-21"
    $candidates.Add((Join-Path $localJdkRoot "bin\java.exe"))
    if (Test-Path -LiteralPath $localJdkRoot -PathType Container) {
        Get-ChildItem -LiteralPath $localJdkRoot -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { $candidates.Add((Join-Path $_.FullName "bin\java.exe")) }
    }

    if (![string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidates.Add((Join-Path $env:JAVA_HOME "bin\java.exe"))
    }

    $pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($pathJava) {
        $candidates.Add($pathJava.Source)
    }

    foreach ($vendorRoot in @(
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:ProgramFiles "Java")
    )) {
        if (!(Test-Path -LiteralPath $vendorRoot -PathType Container)) {
            continue
        }
        Get-ChildItem -LiteralPath $vendorRoot -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { $candidates.Add((Join-Path $_.FullName "bin\java.exe")) }
    }

    $seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($candidate in $candidates) {
        if ($seen.Add($candidate) -and (Test-Java21 $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    return $null
}

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

function Find-SuperbWarfareJar {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if (![string]::IsNullOrWhiteSpace($env:SUPERB_WARFARE_JAR)) {
        $candidates.Add($env:SUPERB_WARFARE_JAR)
    }
    if (![string]::IsNullOrWhiteSpace($env:USERPROFILE)) {
        $candidates.Add((Join-Path $env:USERPROFILE "Downloads\superbwarfare-0.8.9-final-mc1.21.1-9b5284f4.jar"))
    }

    foreach ($candidate in $candidates) {
        if (!(Test-Path -LiteralPath $candidate -PathType Leaf)) {
            continue
        }
        $resolved = (Resolve-Path -LiteralPath $candidate).Path
        $hash = (Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($hash -ne $expectedSuperbHash) {
            throw "Superb Warfare comparison jar has unexpected SHA-256: $resolved ($hash)"
        }
        return $resolved
    }
    return $null
}

function Stage-SuperbWarfareJar {
    param([string]$SourceJar)

    New-Item -ItemType Directory -Force -Path $comparisonModsDirectory | Out-Null
    $conflicts = @(Get-ChildItem -LiteralPath $comparisonModsDirectory -Filter "*.jar" -File |
        Where-Object {
            $_.FullName -ne $managedSuperbJar -and $_.Name -match '(?i)^superbwarfare'
        })
    if ($conflicts.Count -gt 0) {
        throw "Another Superb Warfare jar is already in the isolated comparison mods folder: $($conflicts[0].FullName)"
    }

    $stagedHash = if (Test-Path -LiteralPath $managedSuperbJar -PathType Leaf) {
        (Get-FileHash -LiteralPath $managedSuperbJar -Algorithm SHA256).Hash.ToLowerInvariant()
    } else {
        $null
    }
    if ($stagedHash -ne $expectedSuperbHash) {
        Copy-Item -LiteralPath $SourceJar -Destination $managedSuperbJar -Force
    }
}

$javaExe = Find-Java21
if (!$javaExe) {
    Write-Host "Java 21 was not found." -ForegroundColor Red
    Write-Host "Install a Java 21 JDK, set JAVA_HOME, add java.exe to PATH, or place it under:"
    Write-Host "  $projectRoot\.tooling\jdk-21"
    exit 1
}
$javaHome = Split-Path -Parent (Split-Path -Parent $javaExe)

if (!(Test-Path $gradleWrapper)) {
    Write-Host "Gradle wrapper was not found at:" -ForegroundColor Red
    Write-Host "  $gradleWrapper"
    exit 1
}

$latestJar = Get-LatestBuildJar
$superbJar = Find-SuperbWarfareJar
if (!$superbJar) {
    Write-Host "Superb Warfare comparison jar was not found." -ForegroundColor Red
    Write-Host "Expected the exact 9b5284f4 build at:"
    Write-Host "  $env:USERPROFILE\Downloads\superbwarfare-0.8.9-final-mc1.21.1-9b5284f4.jar"
    Write-Host "Alternatively set SUPERB_WARFARE_JAR to that file."
    exit 1
}

Write-Host ""
Write-Host "HBM Nuclear Tech - Quick View Latest Build" -ForegroundColor Cyan
Write-Host "Project: $projectRoot"
Write-Host "Java:    $javaHome"
Write-Host "Compare: Superb Warfare 0.8.9, commit 9b5284f4"
Write-Host "Source:  $superbJar"

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

Stage-SuperbWarfareJar $superbJar

Write-Host ""
Write-Host "Launching isolated HBM + Superb Warfare comparison client..." -ForegroundColor Cyan
Write-Host "Comparison directory: $comparisonGameDirectory"
Write-Host "Close Minecraft when you are finished reviewing the build."
Write-Host ""

& $gradleWrapper runClientComparison
exit $LASTEXITCODE
