Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$resources = Join-Path $root "src\main\resources"
$assetRoot = Join-Path $resources "assets"
$issues = New-Object System.Collections.Generic.List[string]
Add-Type -AssemblyName System.Web.Extensions
$jsonSerializer = New-Object System.Web.Script.Serialization.JavaScriptSerializer

function Add-Issue([string]$message) {
    $issues.Add($message)
}

function Test-ResourceFile([string]$namespace, [string]$folder, [string]$path, [string]$extension) {
    if ($namespace -ne "hbm") {
        return $true
    }
    $candidate = Join-Path $assetRoot (Join-Path $namespace (Join-Path $folder ($path + $extension)))
    return Test-Path -LiteralPath $candidate
}

function Get-TextureRefs($node) {
    $refs = New-Object System.Collections.Generic.List[string]
    if ($null -eq $node) {
        return $refs
    }

    if ($node -is [System.Management.Automation.PSCustomObject]) {
        foreach ($property in $node.PSObject.Properties) {
            if ($property.Name -eq "textures" -and $property.Value -is [System.Management.Automation.PSCustomObject]) {
                foreach ($texture in $property.Value.PSObject.Properties) {
                    if ($texture.Value -is [string] -and -not $texture.Value.StartsWith("#")) {
                        $refs.Add($texture.Value)
                    }
                }
            } else {
                foreach ($nested in Get-TextureRefs $property.Value) {
                    $refs.Add($nested)
                }
            }
        }
    } elseif ($node -is [System.Array]) {
        foreach ($entry in $node) {
            foreach ($nested in Get-TextureRefs $entry) {
                $refs.Add($nested)
            }
        }
    }

    return $refs
}

Get-ChildItem -LiteralPath $resources -Recurse -Filter "*.json" | ForEach-Object {
    $file = $_
    try {
        $jsonSerializer.DeserializeObject((Get-Content -LiteralPath $file.FullName -Raw)) | Out-Null
    } catch {
        Add-Issue "Invalid JSON: $($file.FullName) :: $($_.Exception.Message)"
    }
}

$modelRoots = @(
    (Join-Path $assetRoot "hbm\models\block"),
    (Join-Path $assetRoot "hbm\models\item")
)

foreach ($modelRoot in $modelRoots) {
    if (-not (Test-Path -LiteralPath $modelRoot)) {
        continue
    }
    Get-ChildItem -LiteralPath $modelRoot -Recurse -Filter "*.json" | ForEach-Object {
        $model = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        foreach ($textureRef in Get-TextureRefs $model) {
            if ($textureRef -notmatch "^([^:]+):(.+)$") {
                Add-Issue "Model texture missing namespace: $($_.FullName) -> $textureRef"
                continue
            }
            $namespace = $Matches[1]
            $texturePath = $Matches[2]
            if (-not (Test-ResourceFile $namespace "textures" $texturePath ".png")) {
                Add-Issue "Missing model texture: $($_.FullName) -> $textureRef"
            }
        }
    }
}

$soundsJson = Join-Path $assetRoot "hbm\sounds.json"
if (Test-Path -LiteralPath $soundsJson) {
    $sounds = Get-Content -LiteralPath $soundsJson -Raw | ConvertFrom-Json
    foreach ($soundEvent in $sounds.PSObject.Properties) {
        foreach ($sound in $soundEvent.Value.sounds) {
            $soundName = if ($sound -is [string]) { $sound } else { $sound.name }
            if ($soundName -notmatch "^([^:]+):(.+)$") {
                Add-Issue "Sound missing namespace: $($soundEvent.Name) -> $soundName"
                continue
            }
            $namespace = $Matches[1]
            $soundPath = $Matches[2]
            if (-not (Test-ResourceFile $namespace "sounds" $soundPath ".ogg")) {
                Add-Issue "Missing sound file: $($soundEvent.Name) -> $soundName"
            }
        }
    }
}

if ($issues.Count -gt 0) {
    Write-Host "Parity validation failed with $($issues.Count) issue(s):"
    foreach ($issue in $issues) {
        Write-Host " - $issue"
    }
    exit 1
}

Write-Host "Parity validation passed: JSON, HBM model textures, and HBM sound references resolved."
