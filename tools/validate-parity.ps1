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

    if ($node -is [System.Collections.IDictionary]) {
        foreach ($key in $node.Keys) {
            $value = $node[$key]
            if (([string]$key -eq "model" -or [string]$key -eq "parent") -and $value -is [string]) {
                $refs.Add($value)
            } else {
                foreach ($nested in Get-ModelRefs $value) {
                    $refs.Add($nested)
                }
            }
        }
    } elseif ($node -is [System.Management.Automation.PSCustomObject]) {
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

function Get-ModelRefs($node) {
    $refs = New-Object System.Collections.Generic.List[string]
    if ($null -eq $node) {
        return $refs
    }

    if ($node -is [System.Management.Automation.PSCustomObject]) {
        foreach ($property in $node.PSObject.Properties) {
            if (($property.Name -eq "model" -or $property.Name -eq "parent") -and $property.Value -is [string]) {
                $refs.Add($property.Value)
            } else {
                foreach ($nested in Get-ModelRefs $property.Value) {
                    $refs.Add($nested)
                }
            }
        }
    } elseif ($node -is [System.Array]) {
        foreach ($entry in $node) {
            foreach ($nested in Get-ModelRefs $entry) {
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
        foreach ($modelRef in Get-ModelRefs $model) {
            if ($modelRef.EndsWith(".obj")) {
                continue
            }
            if ($modelRef -notmatch "^([^:]+):(.+)$") {
                continue
            }
            if (-not (Test-ResourceFile $Matches[1] "models" $Matches[2] ".json")) {
                Add-Issue "Missing HBM parent/override model: $($_.FullName) -> $modelRef"
            }
        }
    }
}

$blockstateRoot = Join-Path $assetRoot "hbm\blockstates"
if (Test-Path -LiteralPath $blockstateRoot) {
    Get-ChildItem -LiteralPath $blockstateRoot -Recurse -Filter "*.json" | ForEach-Object {
        $blockstate = $jsonSerializer.DeserializeObject((Get-Content -LiteralPath $_.FullName -Raw))
        foreach ($modelRef in Get-ModelRefs $blockstate) {
            if ($modelRef -notmatch "^([^:]+):(.+)$") {
                Add-Issue "Blockstate model missing namespace: $($_.FullName) -> $modelRef"
                continue
            }
            if (-not (Test-ResourceFile $Matches[1] "models" $Matches[2] ".json")) {
                Add-Issue "Missing blockstate model: $($_.FullName) -> $modelRef"
            }
        }
    }
}

$particleRoot = Join-Path $assetRoot "hbm\particles"
if (Test-Path -LiteralPath $particleRoot) {
    Get-ChildItem -LiteralPath $particleRoot -Recurse -Filter "*.json" | ForEach-Object {
        $particle = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        foreach ($particleRef in @($particle.textures)) {
            if ($particleRef -notmatch "^([^:]+):(.+)$") {
                Add-Issue "Particle texture missing namespace: $($_.FullName) -> $particleRef"
                continue
            }
            $namespace = $Matches[1]
            $particlePath = $Matches[2]
            if (-not (Test-ResourceFile $namespace "textures\particle" $particlePath ".png")) {
                Add-Issue "Missing particle texture: $($_.FullName) -> $particleRef"
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

foreach ($asset in @(
    @{ Path = "hbm\textures\particle\particle_base.png"; Sha256 = "CAB60F5502B307235135EE54EBDE9C081B00D61A009CCEEB221DD9213DDA3872" },
    @{ Path = "hbm\textures\particle\flare.png"; Sha256 = "353FEB776F5E1E7CA2F036E95FB4C26442EBBD9366CAF6356756692C48CE1F50" }
)) {
    $path = Join-Path $assetRoot $asset.Path
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Issue "Missing NTM Extended Torex asset: $path"
        continue
    }
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    if ($hash -ne $asset.Sha256) {
        Add-Issue "Wrong NTM Extended Torex asset hash: $path -> $hash"
    }
}

if ($issues.Count -gt 0) {
    Write-Host "Parity validation failed with $($issues.Count) issue(s):"
    foreach ($issue in $issues) {
        Write-Host " - $issue"
    }
    exit 1
}

Write-Host "Parity validation passed: JSON, HBM model/particle textures, NTM Extended Torex assets, and HBM sound references resolved."
