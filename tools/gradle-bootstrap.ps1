Set-StrictMode -Version Latest

function Get-HbmGradleCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    $wrapper = Join-Path $ProjectRoot 'gradlew.bat'
    $properties = Join-Path $ProjectRoot 'gradle\wrapper\gradle-wrapper.properties'
    $version = $null

    if (Test-Path -LiteralPath $properties -PathType Leaf) {
        $distributionLine = Get-Content -LiteralPath $properties |
            Where-Object { $_ -match '^distributionUrl=' } |
            Select-Object -First 1
        if ($distributionLine -match 'gradle-([0-9][0-9A-Za-z.-]*)-bin\.zip') {
            $version = $Matches[1]
        }
    }

    $candidates = [System.Collections.Generic.List[string]]::new()
    if ($version) {
        $candidates.Add((Join-Path $ProjectRoot ".tooling\gradle-$version\bin\gradle.bat"))
        if (![string]::IsNullOrWhiteSpace($env:USERPROFILE)) {
            $distributionRoot = Join-Path $env:USERPROFILE ".gradle\wrapper\dists\gradle-$version-bin"
            if (Test-Path -LiteralPath $distributionRoot -PathType Container) {
                Get-ChildItem -LiteralPath $distributionRoot -Directory -ErrorAction SilentlyContinue |
                    ForEach-Object {
                        $candidates.Add((Join-Path $_.FullName "gradle-$version\bin\gradle.bat"))
                    }
            }
        }
    }

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    if (Test-Path -LiteralPath $wrapper -PathType Leaf) {
        return (Resolve-Path -LiteralPath $wrapper).Path
    }

    throw "Neither an installed Gradle $version runtime nor the project wrapper was found."
}
