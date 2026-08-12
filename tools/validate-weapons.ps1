param(
    [string]$ProjectRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Split-Path -Parent $PSScriptRoot
}

$resolvedProjectRoot = Resolve-Path -LiteralPath $ProjectRoot -ErrorAction Stop
$ProjectRoot = $resolvedProjectRoot.Path
$resourcesRoot = Join-Path $ProjectRoot "src\main\resources"
$assetsRoot = Join-Path $resourcesRoot "assets"
$gunRoot = Join-Path $resourcesRoot "data\hbm\guns"
$ammoRoot = Join-Path $resourcesRoot "data\hbm\ammo"

$script:ValidationErrors = [System.Collections.Generic.List[string]]::new()
$script:ValidationWarnings = [System.Collections.Generic.List[string]]::new()
$script:SoundCatalogs = @{}
$script:AnimationDocuments = @{}
$script:UniqueErrorKeys = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)

function Add-ValidationError {
    param([string]$Message)
    [void]$script:ValidationErrors.Add($Message)
}

function Add-ValidationWarning {
    param([string]$Message)
    [void]$script:ValidationWarnings.Add($Message)
}

function Add-UniqueValidationError {
    param([string]$Key, [string]$Message)
    if ($script:UniqueErrorKeys.Add($Key)) {
        Add-ValidationError $Message
    }
}

function Test-JsonObject {
    param($Value)
    return $null -ne $Value -and $Value -is [System.Management.Automation.PSCustomObject]
}

function Test-JsonArray {
    param($Value)
    return $null -ne $Value -and $Value -is [System.Array]
}

function Test-JsonNumber {
    param($Value)
    return $null -ne $Value -and
        $Value -is [System.ValueType] -and
        $Value -isnot [bool] -and
        $Value -isnot [char]
}

function Test-HasProperty {
    param($Object, [string]$Name)
    if (-not (Test-JsonObject $Object)) {
        return $false
    }
    return $null -ne $Object.PSObject.Properties[$Name]
}

function Get-JsonProperty {
    param($Object, [string]$Name)
    if (-not (Test-HasProperty $Object $Name)) {
        return $null
    }
    $value = $Object.PSObject.Properties[$Name].Value
    if ($value -is [System.Array]) {
        # PowerShell unwraps single-element arrays emitted by a function unless the array itself is wrapped.
        return ,$value
    }
    return $value
}

function Assert-Fields {
    param(
        $Object,
        [string[]]$Required,
        [string[]]$Allowed,
        [string]$Scope
    )

    if (-not (Test-JsonObject $Object)) {
        Add-ValidationError "$Scope must be a JSON object."
        return
    }

    foreach ($name in $Required) {
        if (-not (Test-HasProperty $Object $name)) {
            Add-ValidationError "$Scope is missing required field '$name'."
        }
    }

    foreach ($property in $Object.PSObject.Properties) {
        if ($property.Name -notin $Allowed) {
            Add-ValidationError "$Scope contains unknown field '$($property.Name)'."
        }
    }
}

function Get-StringField {
    param($Object, [string]$Name, [string]$Scope)
    $value = Get-JsonProperty $Object $Name
    if ($value -isnot [string] -or [string]::IsNullOrWhiteSpace($value)) {
        Add-ValidationError "$Scope.$Name must be a non-empty string."
        return $null
    }
    return $value
}

function Get-IntegerField {
    param($Object, [string]$Name, [string]$Scope)
    $value = Get-JsonProperty $Object $Name
    if (-not (Test-JsonNumber $value)) {
        Add-ValidationError "$Scope.$Name must be an integer."
        return $null
    }
    $number = [double]$value
    if ([double]::IsNaN($number) -or [double]::IsInfinity($number) -or $number -ne [math]::Truncate($number)) {
        Add-ValidationError "$Scope.$Name must be an integer."
        return $null
    }
    return [long]$number
}

function Get-NumberField {
    param($Object, [string]$Name, [string]$Scope)
    $value = Get-JsonProperty $Object $Name
    if (-not (Test-JsonNumber $value)) {
        Add-ValidationError "$Scope.$Name must be a number."
        return $null
    }
    $number = [double]$value
    if ([double]::IsNaN($number) -or [double]::IsInfinity($number)) {
        Add-ValidationError "$Scope.$Name must be finite."
        return $null
    }
    return $number
}

function Get-BooleanField {
    param($Object, [string]$Name, [string]$Scope)
    $value = Get-JsonProperty $Object $Name
    if ($value -isnot [bool]) {
        Add-ValidationError "$Scope.$Name must be a boolean."
        return $null
    }
    return $value
}

function Get-ObjectField {
    param($Object, [string]$Name, [string]$Scope)
    $value = Get-JsonProperty $Object $Name
    if (-not (Test-JsonObject $value)) {
        Add-ValidationError "$Scope.$Name must be an object."
        return $null
    }
    return $value
}

function Get-StringArrayField {
    param($Object, [string]$Name, [string]$Scope)
    $value = Get-JsonProperty $Object $Name
    if (-not (Test-JsonArray $value)) {
        Add-ValidationError "$Scope.$Name must be an array."
        return @()
    }

    $result = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $value.Count; $index++) {
        $entry = $value[$index]
        if ($entry -isnot [string] -or [string]::IsNullOrWhiteSpace($entry)) {
            Add-ValidationError "$Scope.$Name[$index] must be a non-empty string."
        } else {
            [void]$result.Add($entry)
        }
    }
    return $result.ToArray()
}

function Test-InRange {
    param($Value, [double]$Minimum, [double]$Maximum, [string]$Field)
    if ($null -ne $Value -and ($Value -lt $Minimum -or $Value -gt $Maximum)) {
        Add-ValidationError "$Field must be between $Minimum and $Maximum; found $Value."
    }
}

function Get-ResourceLocation {
    param([string]$Value, [string]$Field)
    if ($null -eq $Value -or $Value -cnotmatch '^([a-z0-9_.-]+):([a-z0-9/._-]+)$') {
        Add-ValidationError "$Field contains invalid resource location '$Value'."
        return $null
    }
    return [PSCustomObject]@{
        Namespace = $Matches[1]
        Path = $Matches[2]
        Value = $Value
    }
}

function Read-JsonDocument {
    param([string]$Path, [string]$Scope)
    try {
        $document = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop | ConvertFrom-Json -ErrorAction Stop
    } catch {
        Add-ValidationError "$Scope is not valid JSON: $($_.Exception.Message)"
        return $null
    }
    if (-not (Test-JsonObject $document)) {
        Add-ValidationError "$Scope must have a JSON object at its root."
        return $null
    }
    return $document
}

function Get-SafeAssetPath {
    param([string]$Namespace, [string]$AssetPath, [string]$Scope)
    $namespaceRoot = [System.IO.Path]::GetFullPath((Join-Path $assetsRoot $Namespace))
    $candidate = [System.IO.Path]::GetFullPath((Join-Path $namespaceRoot $AssetPath.Replace('/', [System.IO.Path]::DirectorySeparatorChar)))
    $prefix = $namespaceRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $candidate.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        Add-ValidationError "$Scope escapes the namespace asset directory."
        return $null
    }
    return $candidate
}

function Get-SoundCatalog {
    param([string]$Namespace, [string]$Scope)
    if ($script:SoundCatalogs.ContainsKey($Namespace)) {
        return $script:SoundCatalogs[$Namespace]
    }

    $catalogPath = Join-Path (Join-Path $assetsRoot $Namespace) "sounds.json"
    if (-not (Test-Path -LiteralPath $catalogPath -PathType Leaf)) {
        Add-ValidationError "$Scope references namespace '$Namespace', but '$catalogPath' is missing."
        return $null
    }
    $catalog = Read-JsonDocument $catalogPath "sound catalog $Namespace"
    if ($null -ne $catalog) {
        $script:SoundCatalogs[$Namespace] = $catalog
    }
    return $catalog
}

function Resolve-SoundEvent {
    param(
        [string]$EventId,
        [string]$Scope,
        [System.Collections.Generic.HashSet[string]]$Visited
    )

    $resource = Get-ResourceLocation $EventId $Scope
    if ($null -eq $resource) {
        return
    }
    if ($resource.Namespace -ne 'hbm') {
        Add-ValidationError "$Scope must use an HBM sound event; found '$EventId'."
        return
    }
    if (-not $Visited.Add($EventId)) {
        Add-ValidationError "$Scope contains a cyclic sound-event reference at '$EventId'."
        return
    }

    try {
        $catalog = Get-SoundCatalog $resource.Namespace $Scope
        if ($null -eq $catalog) {
            return
        }
        $eventProperty = $catalog.PSObject.Properties[$resource.Path]
        if ($null -eq $eventProperty) {
            Add-ValidationError "$Scope references missing sound event '$EventId'."
            return
        }
        $event = $eventProperty.Value
        if (-not (Test-JsonObject $event) -or -not (Test-HasProperty $event 'sounds')) {
            Add-ValidationError "$Scope references malformed sound event '$EventId'."
            return
        }

        # Read the property directly so PowerShell does not nest an already preserved JSON array.
        $entriesValue = $event.PSObject.Properties['sounds'].Value
        $entries = @($entriesValue)
        if ($entries.Count -eq 0) {
            Add-ValidationError "$Scope references empty sound event '$EventId'."
            return
        }

        foreach ($entry in $entries) {
            $name = $null
            $type = 'file'
            if ($entry -is [string]) {
                $name = $entry
            } elseif (Test-JsonObject $entry) {
                $name = Get-JsonProperty $entry 'name'
                if (Test-HasProperty $entry 'type') {
                    $type = Get-JsonProperty $entry 'type'
                }
            }

            if ($name -isnot [string] -or [string]::IsNullOrWhiteSpace($name)) {
                Add-ValidationError "Sound event '$EventId' contains an entry without a valid name."
                continue
            }
            if ($name -notmatch ':') {
                $name = "$($resource.Namespace):$name"
            }
            if ($type -eq 'event') {
                Resolve-SoundEvent $name "$Scope -> $EventId" $Visited
                continue
            }

            $fileResource = Get-ResourceLocation $name "sound file in $EventId"
            if ($null -eq $fileResource) {
                continue
            }
            $relativePath = "sounds/$($fileResource.Path)"
            if (-not $relativePath.EndsWith('.ogg', [System.StringComparison]::OrdinalIgnoreCase)) {
                $relativePath += '.ogg'
            }
            $filePath = Get-SafeAssetPath $fileResource.Namespace $relativePath "sound file '$name'"
            if ($null -eq $filePath -or -not (Test-Path -LiteralPath $filePath -PathType Leaf)) {
                Add-ValidationError "$Scope resolves '$EventId' to missing OGG '$name'."
            } elseif ((Get-Item -LiteralPath $filePath).Length -eq 0) {
                Add-ValidationError "$Scope resolves '$EventId' to empty OGG '$name'."
            }
        }
    } finally {
        [void]$Visited.Remove($EventId)
    }
}

function Resolve-AnimationReference {
    param([string]$Reference, [string]$Scope)

    if ($Reference -notmatch '^([^#]+)#([^#]+)$') {
        Add-ValidationError "$Scope must use '<namespace>:<asset-path>#<clip>'; found '$Reference'."
        return
    }
    $assetId = $Matches[1]
    $clip = $Matches[2]
    if ([string]::IsNullOrWhiteSpace($clip)) {
        Add-ValidationError "$Scope has an empty animation clip."
        return
    }

    $resource = Get-ResourceLocation $assetId $Scope
    if ($null -eq $resource) {
        return
    }
    if ($resource.Namespace -ne 'hbm') {
        Add-ValidationError "$Scope must use an HBM animation asset; found '$assetId'."
        return
    }
    if (-not $resource.Path.EndsWith('.json', [System.StringComparison]::OrdinalIgnoreCase)) {
        Add-ValidationError "$Scope animation asset must end in .json; found '$assetId'."
        return
    }

    $animationPath = Get-SafeAssetPath $resource.Namespace $resource.Path $Scope
    if ($null -eq $animationPath -or -not (Test-Path -LiteralPath $animationPath -PathType Leaf)) {
        Add-UniqueValidationError "missing-animation:$assetId" "Animation references resolve to missing file '$assetId'."
        return
    }

    if ($script:AnimationDocuments.ContainsKey($animationPath)) {
        $document = $script:AnimationDocuments[$animationPath]
    } else {
        $document = Read-JsonDocument $animationPath "animation file $assetId"
        if ($null -ne $document) {
            $script:AnimationDocuments[$animationPath] = $document
        }
    }
    if ($null -eq $document) {
        return
    }

    $isActiveGeckoAsset = $resource.Path.StartsWith('animations/', [System.StringComparison]::OrdinalIgnoreCase)
    if ($isActiveGeckoAsset) {
        $formatVersion = Get-JsonProperty $document 'format_version'
        if ($formatVersion -isnot [string] -or $formatVersion -ne '1.8.0') {
            Add-UniqueValidationError "gecko-format:$assetId" "Active animation '$assetId' must declare GeckoLib format_version '1.8.0'."
        }
    }

    $clipTable = $null
    if (Test-HasProperty $document 'animations') {
        $clipTable = Get-JsonProperty $document 'animations'
    } elseif (-not $isActiveGeckoAsset -and (Test-HasProperty $document 'anim')) {
        $clipTable = Get-JsonProperty $document 'anim'
    }
    if (-not (Test-JsonObject $clipTable)) {
        if ($isActiveGeckoAsset) {
            Add-UniqueValidationError "gecko-animations-root:$assetId" "Active animation '$assetId' must contain an animations object."
        } else {
            Add-ValidationError "$Scope references '$assetId', which has neither an animations nor anim object."
        }
        return
    }
    if (-not (Test-HasProperty $clipTable $clip)) {
        Add-ValidationError "$Scope references missing clip '$clip' in '$assetId'."
        return
    }

    $clipDocument = Get-JsonProperty $clipTable $clip
    if (-not (Test-JsonObject $clipDocument)) {
        Add-ValidationError "$Scope references '$clip' in '$assetId', but that clip is not an object."
        return
    }

    if ($isActiveGeckoAsset -and $clip -in @('fire', 'dry_fire', 'reload_start', 'reload_loop', 'reload_end')) {
        $bones = Get-JsonProperty $clipDocument 'bones'
        $hasMovingPartMotion = $false
        if (Test-JsonObject $bones) {
            foreach ($boneProperty in $bones.PSObject.Properties) {
                # Gun/MainBody are presentation roots; this gate requires an actual mechanism part to move.
                if ($boneProperty.Name -in @('root', 'Gun', 'MainBody')) {
                    continue
                }
                if (-not (Test-JsonObject $boneProperty.Value)) {
                    continue
                }
                foreach ($channelName in @('position', 'rotation', 'scale')) {
                    $channel = Get-JsonProperty $boneProperty.Value $channelName
                    if (-not (Test-JsonObject $channel)) {
                        continue
                    }
                    $keyframes = @($channel.PSObject.Properties)
                    if ($keyframes.Count -lt 2) {
                        continue
                    }
                    $serializedValues = @($keyframes | ForEach-Object {
                        $_.Value | ConvertTo-Json -Compress -Depth 16
                    } | Select-Object -Unique)
                    if ($serializedValues.Count -gt 1) {
                        $hasMovingPartMotion = $true
                        break
                    }
                }
                if ($hasMovingPartMotion) {
                    break
                }
            }
        }
        if (-not $hasMovingPartMotion) {
            Add-UniqueValidationError "gecko-motion:$assetId#$clip" "Active animation '$assetId#$clip' must contain nonempty keyframed motion on a moving weapon part."
        }
    }
}

function Validate-GunDefinition {
    param($Json, [string]$Id, [string]$Scope)

    $allowed = @(
        'schema', 'ammo_family', 'supported_ammo', 'fire_modes', 'default_fire_mode',
        'rpm', 'burst_size', 'ads', 'movement_weight', 'spread', 'recoil', 'magazine',
        'reload', 'damage', 'headshot_multiplier', 'velocity', 'range', 'behavior',
        'sounds', 'animations'
    )
    $required = $allowed | Where-Object { $_ -ne 'burst_size' }
    Assert-Fields $Json $required $allowed $Scope

    $schema = Get-IntegerField $Json 'schema' $Scope
    if ($null -ne $schema -and $schema -ne 1) {
        Add-ValidationError "$Scope.schema must be 1; found $schema."
    }

    $ammoFamily = Get-StringField $Json 'ammo_family' $Scope
    if ($null -ne $ammoFamily) {
        [void](Get-ResourceLocation $ammoFamily "$Scope.ammo_family")
    }

    $supportedAmmo = @(Get-StringArrayField $Json 'supported_ammo' $Scope)
    if ($supportedAmmo.Count -eq 0) {
        Add-ValidationError "$Scope.supported_ammo must not be empty."
    }
    foreach ($ammoId in $supportedAmmo) {
        [void](Get-ResourceLocation $ammoId "$Scope.supported_ammo")
    }
    if (@($supportedAmmo | Select-Object -Unique).Count -ne $supportedAmmo.Count) {
        Add-ValidationError "$Scope.supported_ammo contains duplicates."
    }

    $fireModes = @(Get-StringArrayField $Json 'fire_modes' $Scope)
    if ($fireModes.Count -eq 0) {
        Add-ValidationError "$Scope.fire_modes must not be empty."
    }
    foreach ($mode in $fireModes) {
        if ($mode.ToLowerInvariant() -notin @('semi', 'burst', 'auto')) {
            Add-ValidationError "$Scope.fire_modes contains unsupported mode '$mode'."
        }
    }
    $defaultMode = Get-StringField $Json 'default_fire_mode' $Scope
    if ($null -ne $defaultMode -and $defaultMode.ToLowerInvariant() -notin @($fireModes | ForEach-Object { $_.ToLowerInvariant() })) {
        Add-ValidationError "$Scope.default_fire_mode must appear in fire_modes."
    }

    $rpm = Get-IntegerField $Json 'rpm' $Scope
    Test-InRange $rpm 30 1800 "$Scope.rpm"
    $burstSize = 3
    if (Test-HasProperty $Json 'burst_size') {
        $burstSize = Get-IntegerField $Json 'burst_size' $Scope
    }
    Test-InRange $burstSize 2 10 "$Scope.burst_size"

    $ads = Get-ObjectField $Json 'ads' $Scope
    if ($null -ne $ads) {
        $requiredAdsFields = @('fov_multiplier', 'movement_multiplier', 'sensitivity_multiplier')
        $allowedAdsFields = $requiredAdsFields + @('zero_pitch_degrees', 'zero_distance')
        Assert-Fields $ads $requiredAdsFields $allowedAdsFields "$Scope.ads"
        Test-InRange (Get-NumberField $ads 'fov_multiplier' "$Scope.ads") 0.05 1 "$Scope.ads.fov_multiplier"
        Test-InRange (Get-NumberField $ads 'movement_multiplier' "$Scope.ads") 0.05 1 "$Scope.ads.movement_multiplier"
        Test-InRange (Get-NumberField $ads 'sensitivity_multiplier' "$Scope.ads") 0.05 1 "$Scope.ads.sensitivity_multiplier"
        if (Test-HasProperty $ads 'zero_pitch_degrees') {
            Test-InRange (Get-NumberField $ads 'zero_pitch_degrees' "$Scope.ads") -10 10 "$Scope.ads.zero_pitch_degrees"
        }
        if (Test-HasProperty $ads 'zero_distance') {
            Test-InRange (Get-NumberField $ads 'zero_distance' "$Scope.ads") 0 1024 "$Scope.ads.zero_distance"
        }
    }

    Test-InRange (Get-NumberField $Json 'movement_weight' $Scope) 0 1 "$Scope.movement_weight"

    $spread = Get-ObjectField $Json 'spread' $Scope
    if ($null -ne $spread) {
        Assert-Fields $spread @('hip_degrees', 'ads_degrees', 'movement_degrees') @('hip_degrees', 'ads_degrees', 'movement_degrees') "$Scope.spread"
        $hipSpread = Get-NumberField $spread 'hip_degrees' "$Scope.spread"
        $adsSpread = Get-NumberField $spread 'ads_degrees' "$Scope.spread"
        [void](Get-NumberField $spread 'movement_degrees' "$Scope.spread")
        if ($null -ne $hipSpread -and $hipSpread -lt 0) { Add-ValidationError "$Scope.spread.hip_degrees cannot be negative." }
        if ($null -ne $adsSpread -and $adsSpread -lt 0) { Add-ValidationError "$Scope.spread.ads_degrees cannot be negative." }
    }

    $recoil = Get-ObjectField $Json 'recoil' $Scope
    if ($null -ne $recoil) {
        Assert-Fields $recoil @('pitch', 'yaw', 'recovery_per_tick') @('pitch', 'yaw', 'recovery_per_tick') "$Scope.recoil"
        $pitch = Get-NumberField $recoil 'pitch' "$Scope.recoil"
        $yaw = Get-NumberField $recoil 'yaw' "$Scope.recoil"
        [void](Get-NumberField $recoil 'recovery_per_tick' "$Scope.recoil")
        if ($null -ne $pitch -and $pitch -lt 0) { Add-ValidationError "$Scope.recoil.pitch cannot be negative." }
        if ($null -ne $yaw -and $yaw -lt 0) { Add-ValidationError "$Scope.recoil.yaw cannot be negative." }
    }

    $magazine = Get-ObjectField $Json 'magazine' $Scope
    if ($null -ne $magazine) {
        Assert-Fields $magazine @('capacity', 'uses_chamber') @('capacity', 'uses_chamber') "$Scope.magazine"
        Test-InRange (Get-IntegerField $magazine 'capacity' "$Scope.magazine") 1 500 "$Scope.magazine.capacity"
        [void](Get-BooleanField $magazine 'uses_chamber' "$Scope.magazine")
    }

    $reload = Get-ObjectField $Json 'reload' $Scope
    if ($null -ne $reload) {
        $reloadFields = @('style', 'start_ticks', 'transfer_ticks', 'loop_ticks', 'end_ticks')
        $allowedReloadFields = $reloadFields + @('empty_end_ticks')
        Assert-Fields $reload $reloadFields $allowedReloadFields "$Scope.reload"
        $style = Get-StringField $reload 'style' "$Scope.reload"
        if ($null -ne $style -and $style.ToLowerInvariant() -notin @('magazine', 'per_round')) {
            Add-ValidationError "$Scope.reload.style contains unsupported value '$style'."
        }
        $startTicks = Get-IntegerField $reload 'start_ticks' "$Scope.reload"
        $transferTicks = Get-IntegerField $reload 'transfer_ticks' "$Scope.reload"
        $loopTicks = Get-IntegerField $reload 'loop_ticks' "$Scope.reload"
        $endTicks = Get-IntegerField $reload 'end_ticks' "$Scope.reload"
        $emptyEndTicks = if (Test-HasProperty $reload 'empty_end_ticks') {
            Get-IntegerField $reload 'empty_end_ticks' "$Scope.reload"
        } else {
            $endTicks
        }
        foreach ($timing in @(
            @{ Name = 'start_ticks'; Value = $startTicks },
            @{ Name = 'transfer_ticks'; Value = $transferTicks },
            @{ Name = 'loop_ticks'; Value = $loopTicks },
            @{ Name = 'end_ticks'; Value = $endTicks },
            @{ Name = 'empty_end_ticks'; Value = $emptyEndTicks }
        )) {
            if ($null -ne $timing.Value -and $timing.Value -lt 0) {
                Add-ValidationError "$Scope.reload.$($timing.Name) cannot be negative."
            }
        }
        if ($null -ne $style -and $style.ToLowerInvariant() -eq 'per_round' -and $null -ne $loopTicks -and $loopTicks -le 0) {
            Add-ValidationError "$Scope per_round reload requires positive loop_ticks."
        }
    }

    $damage = Get-NumberField $Json 'damage' $Scope
    if ($null -ne $damage -and $damage -le 0) { Add-ValidationError "$Scope.damage must be positive." }
    $headshot = Get-NumberField $Json 'headshot_multiplier' $Scope
    if ($null -ne $headshot -and $headshot -lt 1) { Add-ValidationError "$Scope.headshot_multiplier must be at least 1." }
    Test-InRange (Get-NumberField $Json 'velocity' $Scope) 0.05 20 "$Scope.velocity"
    Test-InRange (Get-NumberField $Json 'range' $Scope) 1 1024 "$Scope.range"

    $behavior = Get-StringField $Json 'behavior' $Scope
    if ($null -ne $behavior) {
        $behaviorResource = Get-ResourceLocation $behavior "$Scope.behavior"
        if ($null -ne $behaviorResource -and ($behaviorResource.Namespace -ne 'hbm' -or $behaviorResource.Path -notin @('standard', 'grenade_launcher'))) {
            Add-ValidationError "$Scope.behavior uses unsupported behavior '$behavior'."
        }
    }

    $sounds = Get-ObjectField $Json 'sounds' $Scope
    if ($null -ne $sounds) {
        foreach ($requiredSound in @('fire', 'dry_fire', 'reload')) {
            if (-not (Test-HasProperty $sounds $requiredSound)) {
                Add-ValidationError "$Scope.sounds is missing required event '$requiredSound'."
            }
        }
        foreach ($property in $sounds.PSObject.Properties) {
            if ($property.Value -isnot [string] -or [string]::IsNullOrWhiteSpace($property.Value)) {
                Add-ValidationError "$Scope.sounds.$($property.Name) must be a non-empty resource location."
                continue
            }
            $visited = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
            Resolve-SoundEvent $property.Value "$Scope.sounds.$($property.Name)" $visited
        }
    }

    $animations = Get-ObjectField $Json 'animations' $Scope
    $requiredAnimations = @('equip', 'idle', 'ads', 'fire', 'dry_fire', 'reload_start', 'reload_loop', 'reload_end', 'inspect', 'sprint', 'lower')
    if ($null -ne $animations) {
        foreach ($requiredAnimation in $requiredAnimations) {
            if (-not (Test-HasProperty $animations $requiredAnimation)) {
                Add-ValidationError "$Scope.animations is missing required state '$requiredAnimation'."
            }
        }
        foreach ($property in $animations.PSObject.Properties) {
            if ($property.Value -isnot [string] -or [string]::IsNullOrWhiteSpace($property.Value)) {
                Add-ValidationError "$Scope.animations.$($property.Name) must be a non-empty string."
                continue
            }
            Resolve-AnimationReference $property.Value "$Scope.animations.$($property.Name)"
        }
    }

    return [PSCustomObject]@{
        Id = $Id
        AmmoFamily = $ammoFamily
        SupportedAmmo = $supportedAmmo
    }
}

function Validate-AmmoDefinition {
    param($Json, [string]$Id, [string]$Scope)

    $allowed = @(
        'schema', 'family', 'projectile_mode', 'damage_multiplier', 'armor_penetration',
        'pellet_count', 'spread_multiplier', 'gravity', 'drag', 'explosion', 'impact_effect', 'tracer_color'
    )
    $required = $allowed | Where-Object { $_ -ne 'explosion' }
    Assert-Fields $Json $required $allowed $Scope

    $schema = Get-IntegerField $Json 'schema' $Scope
    if ($null -ne $schema -and $schema -ne 1) {
        Add-ValidationError "$Scope.schema must be 1; found $schema."
    }

    $family = Get-StringField $Json 'family' $Scope
    if ($null -ne $family) {
        [void](Get-ResourceLocation $family "$Scope.family")
    }
    $projectileMode = Get-StringField $Json 'projectile_mode' $Scope
    if ($null -ne $projectileMode -and $projectileMode.ToLowerInvariant() -notin @('trajectory', 'entity')) {
        Add-ValidationError "$Scope.projectile_mode contains unsupported value '$projectileMode'."
    }

    $damageMultiplier = Get-NumberField $Json 'damage_multiplier' $Scope
    if ($null -ne $damageMultiplier -and $damageMultiplier -le 0) { Add-ValidationError "$Scope.damage_multiplier must be positive." }
    Test-InRange (Get-NumberField $Json 'armor_penetration' $Scope) 0 1 "$Scope.armor_penetration"
    Test-InRange (Get-IntegerField $Json 'pellet_count' $Scope) 1 64 "$Scope.pellet_count"
    Test-InRange (Get-NumberField $Json 'spread_multiplier' $Scope) 0 4 "$Scope.spread_multiplier"
    Test-InRange (Get-NumberField $Json 'gravity' $Scope) 0 1 "$Scope.gravity"
    Test-InRange (Get-NumberField $Json 'drag' $Scope) 0 0.5 "$Scope.drag"

    $hasExplosion = $false
    if (Test-HasProperty $Json 'explosion') {
        $explosion = Get-JsonProperty $Json 'explosion'
        if ($null -ne $explosion) {
            $hasExplosion = $true
            if (-not (Test-JsonObject $explosion)) {
                Add-ValidationError "$Scope.explosion must be an object or null."
            } else {
                $explosionFields = @('power', 'block_damage', 'shaped_charge')
                Assert-Fields $explosion $explosionFields $explosionFields "$Scope.explosion"
                $power = Get-NumberField $explosion 'power' "$Scope.explosion"
                if ($null -ne $power -and $power -le 0) { Add-ValidationError "$Scope.explosion.power must be positive." }
                [void](Get-BooleanField $explosion 'block_damage' "$Scope.explosion")
                [void](Get-BooleanField $explosion 'shaped_charge' "$Scope.explosion")
            }
        }
    }
    if ($null -ne $projectileMode -and $projectileMode.ToLowerInvariant() -eq 'entity' -and -not $hasExplosion) {
        Add-ValidationError "$Scope entity projectile requires an explosion profile."
    }

    $impactEffect = Get-StringField $Json 'impact_effect' $Scope
    if ($null -ne $impactEffect) {
        [void](Get-ResourceLocation $impactEffect "$Scope.impact_effect")
    }
    $tracerColor = Get-StringField $Json 'tracer_color' $Scope
    if ($null -ne $tracerColor -and $tracerColor -cnotmatch '^#?[0-9A-Fa-f]{6}$') {
        Add-ValidationError "$Scope.tracer_color must be a six-digit RGB hexadecimal value."
    }
    $targetPistolTracerColors = @{
        'hbm:p22_fmj' = 'FFD27A'
        'hbm:p22_ap' = 'FFB85C'
    }
    if ($targetPistolTracerColors.ContainsKey($Id) -and
        $null -ne $tracerColor -and
        $tracerColor.TrimStart('#').ToUpperInvariant() -ne $targetPistolTracerColors[$Id]) {
        Add-ValidationError "$Scope must use the approved visible Target Pistol tracer color $($targetPistolTracerColors[$Id])."
    }

    return [PSCustomObject]@{
        Id = $Id
        Family = $family
    }
}

function Get-DefinitionId {
    param([System.IO.FileInfo]$File, [string]$DefinitionRoot, [string]$Namespace)
    $relative = $File.FullName.Substring($DefinitionRoot.Length).TrimStart('\', '/')
    $relative = $relative.Replace('\', '/')
    $path = $relative.Substring(0, $relative.Length - $File.Extension.Length)
    return "$Namespace`:$path"
}

function Validate-PilotLegacyAssets {
    $pilots = @(
        @{
            Name = 'Target Pistol'
            Model = 'models/weapons/star_f.obj'
            Texture = 'textures/models/weapons/star_f.png'
            Parts = @('Bullet', 'Gun', 'Hammer', 'Mag', 'Slide')
            LegacyAnimation = $null
            Clips = @()
        },
        @{
            Name = 'StG 77'
            Model = 'models/weapons/stg77.obj'
            Texture = 'textures/models/weapons/stg77.png'
            Parts = @('Barrel', 'Breech', 'Bullets', 'Gun', 'Handle', 'Lever', 'Magazine', 'Safety')
            LegacyAnimation = 'models/weapons/animations/stg77.json'
            Clips = @('Fire', 'FireDry', 'Inspect', 'Reload')
        },
        @{
            Name = 'SPAS-12'
            Model = 'models/weapons/spas-12.obj'
            Texture = 'textures/models/weapons/spas-12.png'
            Parts = @('MainBody', 'PumpGrip', 'Shell', 'ShellFore')
            LegacyAnimation = 'models/weapons/animations/spas12.json'
            Clips = @('Fire', 'FireAlt', 'FireDry', 'Inspect', 'Jammed', 'Reload', 'ReloadEmptyStart', 'ReloadEnd', 'ReloadStart')
        },
        @{
            Name = 'Congo Lake'
            Model = 'models/weapons/congolake.obj'
            Texture = 'textures/models/weapons/congolake.png'
            Parts = @('GuardInner', 'GuardOuter', 'Gun', 'Loop', 'Pump', 'Shell', 'ShellFore', 'Sight')
            LegacyAnimation = 'models/weapons/animations/congolake.json'
            Clips = @('Equip', 'Fire', 'FireEmpty', 'Inspect', 'Jammed', 'Reload', 'ReloadEmpty', 'ReloadEnd', 'ReloadStart')
        }
    )

    foreach ($pilot in $pilots) {
        $modelPath = Get-SafeAssetPath 'hbm' $pilot.Model "$($pilot.Name) legacy model"
        if ($null -eq $modelPath -or -not (Test-Path -LiteralPath $modelPath -PathType Leaf)) {
            Add-ValidationError "$($pilot.Name) is missing audited legacy model 'hbm:$($pilot.Model)'."
        } else {
            $parts = @(Get-Content -LiteralPath $modelPath | Where-Object { $_ -match '^(o|g)\s+(.+)$' } | ForEach-Object { $Matches[2].Trim() } | Sort-Object -Unique)
            foreach ($expectedPart in $pilot.Parts) {
                if ($expectedPart -cnotin $parts) {
                    Add-ValidationError "$($pilot.Name) legacy model is missing OBJ part '$expectedPart'."
                }
            }
        }

        $texturePath = Get-SafeAssetPath 'hbm' $pilot.Texture "$($pilot.Name) legacy texture"
        if ($null -eq $texturePath -or -not (Test-Path -LiteralPath $texturePath -PathType Leaf)) {
            Add-ValidationError "$($pilot.Name) is missing audited legacy texture 'hbm:$($pilot.Texture)'."
        } else {
            $bytes = [System.IO.File]::ReadAllBytes($texturePath)
            $pngSignature = [byte[]](137, 80, 78, 71, 13, 10, 26, 10)
            if ($bytes.Length -lt 8 -or -not (@(0..7 | Where-Object { $bytes[$_] -ne $pngSignature[$_] }).Count -eq 0)) {
                Add-ValidationError "$($pilot.Name) legacy texture is not a valid PNG by signature."
            }
        }

        if ($null -ne $pilot.LegacyAnimation) {
            $legacyAnimationPath = Get-SafeAssetPath 'hbm' $pilot.LegacyAnimation "$($pilot.Name) legacy animation"
            if ($null -eq $legacyAnimationPath -or -not (Test-Path -LiteralPath $legacyAnimationPath -PathType Leaf)) {
                Add-ValidationError "$($pilot.Name) is missing audited legacy animation 'hbm:$($pilot.LegacyAnimation)'."
            } else {
                $legacyDocument = Read-JsonDocument $legacyAnimationPath "$($pilot.Name) legacy animation"
                if ($null -ne $legacyDocument) {
                    $legacyClips = Get-JsonProperty $legacyDocument 'anim'
                    if (-not (Test-JsonObject $legacyClips)) {
                        Add-ValidationError "$($pilot.Name) legacy animation has no anim object."
                    } else {
                        foreach ($clip in $pilot.Clips) {
                            if (-not (Test-HasProperty $legacyClips $clip)) {
                                Add-ValidationError "$($pilot.Name) legacy animation is missing clip '$clip'."
                            }
                        }
                    }
                }
            }
        }
    }
}

function Validate-ItemModel {
    param(
        [string]$ItemId,
        [string]$ExpectedParent,
        [bool]$RequireDisplayTransforms = $false
    )

    $resource = Get-ResourceLocation $ItemId "item model id"
    if ($null -eq $resource) {
        return
    }
    $modelPath = Get-SafeAssetPath $resource.Namespace "models/item/$($resource.Path).json" "item model $ItemId"
    if ($null -eq $modelPath -or -not (Test-Path -LiteralPath $modelPath -PathType Leaf)) {
        Add-ValidationError "Item '$ItemId' is missing model 'models/item/$($resource.Path).json'."
        return
    }
    $model = Read-JsonDocument $modelPath "item model $ItemId"
    if ($null -eq $model) {
        return
    }
    $parent = Get-StringField $model 'parent' "item model $ItemId"
    if ($null -ne $parent -and $parent -ne $ExpectedParent) {
        Add-ValidationError "Item model '$ItemId' must use parent '$ExpectedParent'; found '$parent'."
    }

    if ($RequireDisplayTransforms) {
        $display = Get-JsonProperty $model 'display'
        if (-not (Test-JsonObject $display)) {
            Add-ValidationError "Rendered gun '$ItemId' must define item display transforms."
        } else {
            foreach ($context in @('firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand', 'gui', 'ground', 'fixed')) {
                if (-not (Test-HasProperty $display $context)) {
                    Add-ValidationError "Rendered gun '$ItemId' is missing '$context' display transforms."
                }
            }
        }
        return
    }

    $textures = Get-JsonProperty $model 'textures'
    $layer = if (Test-JsonObject $textures) { Get-JsonProperty $textures 'layer0' } else { $null }
    if ($layer -isnot [string] -or [string]::IsNullOrWhiteSpace($layer)) {
        Add-ValidationError "Generated item model '$ItemId' must define textures.layer0."
        return
    }
    if ($layer.StartsWith('#')) {
        Add-ValidationError "Generated item model '$ItemId' has unresolved texture variable '$layer'."
        return
    }
    if ($layer -notmatch ':') {
        $layer = "$($resource.Namespace):$layer"
    }
    $texture = Get-ResourceLocation $layer "item model $ItemId texture"
    if ($null -eq $texture) {
        return
    }
    $texturePath = Get-SafeAssetPath $texture.Namespace "textures/$($texture.Path).png" "item model $ItemId texture"
    if ($null -eq $texturePath -or -not (Test-Path -LiteralPath $texturePath -PathType Leaf)) {
        Add-ValidationError "Item model '$ItemId' references missing texture '$layer'."
    }
}

function Validate-PilotRuntimeAssets {
    $pilots = @(
        @{ Name = 'Target Pistol'; Id = 'gun_star_f'; Model = 'star_f'; Animation = 'star_f'; VirtualBones = @('camera', 'root', 'Lefthand', 'Righthand', 'flare') },
        @{ Name = 'StG 77'; Id = 'gun_stg77'; Model = 'stg77'; Animation = 'stg77'; VirtualBones = @('camera', 'root', 'Lefthand', 'Righthand', 'flare') },
        @{ Name = 'SPAS-12'; Id = 'gun_spas12'; Model = 'spas-12'; Animation = 'spas12'; VirtualBones = @('camera', 'root', 'Lefthand', 'Righthand', 'flare') },
        @{ Name = 'Congo Lake'; Id = 'gun_congolake'; Model = 'congolake'; Animation = 'congolake'; VirtualBones = @('camera', 'root', 'Lefthand', 'Righthand', 'flare') }
    )

    foreach ($pilot in $pilots) {
        Validate-ItemModel "hbm:$($pilot.Id)" 'builtin/entity' $true
        $modelPath = Get-SafeAssetPath 'hbm' "models/weapons/$($pilot.Model).obj" "$($pilot.Name) runtime OBJ"
        $animationPath = Get-SafeAssetPath 'hbm' "animations/weapon/$($pilot.Animation).animation.json" "$($pilot.Name) runtime animation"
        if ($null -eq $modelPath -or -not (Test-Path -LiteralPath $modelPath -PathType Leaf) -or
            $null -eq $animationPath -or -not (Test-Path -LiteralPath $animationPath -PathType Leaf)) {
            continue
        }

        $parts = @(Get-Content -LiteralPath $modelPath | Where-Object { $_ -match '^(o|g)\s+(.+)$' } |
            ForEach-Object { $Matches[2].Trim() } | Sort-Object -Unique)
        $animation = Read-JsonDocument $animationPath "$($pilot.Name) runtime animation"
        if ($null -eq $animation) {
            continue
        }
        $clips = Get-JsonProperty $animation 'animations'
        if (-not (Test-JsonObject $clips)) {
            continue
        }
        $animatedBones = @()
        foreach ($clip in $clips.PSObject.Properties) {
            $bones = Get-JsonProperty $clip.Value 'bones'
            if (Test-JsonObject $bones) {
                $animatedBones += $bones.PSObject.Properties.Name
            }
        }
        foreach ($bone in @($animatedBones | Sort-Object -Unique)) {
            if ($bone -notin @('root') -and $bone -cnotin $parts -and $bone -cnotin $pilot.VirtualBones) {
                Add-ValidationError "$($pilot.Name) animation targets bone '$bone', but its OBJ exposes no matching object/group."
            }
        }
    }

    $bridgeSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\ObjBakedGeoModelLoader.java'
    $modelSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\HbmGunGeoModel.java'
    $rigSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\SuperbGunRig.java'
    $presentationSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\SuperbGunPresentationState.java'
    $rendererSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\HbmGunGeoRenderer.java'
    $armRendererSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\HbmPlayerArmRenderer.java'
    $calibrationSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\TargetPistolCalibrationState.java'
    $calibrationMarkerSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\TargetPistolCalibrationMarkerRenderer.java'
    $flareRendererSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\render\HbmMuzzleFlashRenderer.java'
    $controllerSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\ClientWeaponController.java'
    $clientSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\HbmNuclearTechClient.java'
    $armPoseSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\client\weapon\HbmGunArmPose.java'
    $weaponServiceSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\weapon\HbmWeaponService.java'
    $targetItemModel = Join-Path $ProjectRoot 'src\main\resources\assets\hbm\models\item\gun_star_f.json'
    $gunItemSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\item\HbmGunItem.java'
    $ballisticsSource = Join-Path $ProjectRoot 'src\main\java\com\hbm\weapon\ballistics\BallisticsService.java'
    if (-not (Test-Path -LiteralPath $bridgeSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $bridgeSource -SimpleMatch 'OBJ-to-Gecko bridge' -Quiet)) {
        Add-ValidationError 'The faithful OBJ-to-Gecko runtime geometry bridge is missing.'
    }
    if (-not (Test-Path -LiteralPath $modelSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $modelSource -SimpleMatch 'ObjBakedGeoModelLoader.load' -Quiet)) {
        Add-ValidationError 'The gun GeoModel is not bound to the OBJ-to-Gecko geometry bridge.'
    }
    if (-not (Test-Path -LiteralPath $rigSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch '"Righthand"' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch '"Lefthand"' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch '"flare"' -Quiet)) {
        Add-ValidationError 'The pilot weapon rigs are missing synthesized hand or muzzle-flare bones.'
    }
    $superbCommit = '9b5284f42ef79532e6fb7f03ab07425c693b0b43'
    if (-not (Test-Path -LiteralPath $presentationSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch $superbCommit -Quiet) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'boneRotX' -Quiet) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'applyFirstPerson' -Quiet)) {
        Add-ValidationError 'The GPL-attributed Superb Warfare procedural presentation adaptation is missing.'
    }
    if (-not (Test-Path -LiteralPath $rendererSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $rendererSource -SimpleMatch 'HbmMuzzleFlashRenderer.render' -Quiet) -or
        -not (Select-String -LiteralPath $rendererSource -SimpleMatch 'HbmPlayerArmRenderer.render' -Quiet) -or
        -not (Select-String -LiteralPath $rendererSource -SimpleMatch 'virtualBone.pivot()' -Quiet)) {
        Add-ValidationError 'The gun renderer is not bound to its model-local arms and muzzle effects.'
    }
    if ((Test-Path -LiteralPath $rendererSource -PathType Leaf) -and
        (Select-String -LiteralPath $rendererSource -SimpleMatch 'poseStack.translate(-0.5D, -0.51D, -0.5D)' -Quiet)) {
        Add-ValidationError 'The gun renderer contains the rejected first-person centering translation that hides the viewmodel below the hotbar.'
    }
    if ((Test-Path -LiteralPath $presentationSource -PathType Leaf) -and
        (Select-String -LiteralPath $presentationSource -SimpleMatch 'sprintCurve' -Quiet)) {
        Add-ValidationError 'The gun presentation contains the rejected non-monotonic sprint curve that drops the viewmodel below the hotbar.'
    }
    if (-not (Test-Path -LiteralPath $gunItemSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $gunItemSource -SimpleMatch 'shouldCauseReequipAnimation' -Quiet) -or
        -not (Select-String -LiteralPath $gunItemSource -SimpleMatch 'return slotChanged || oldStack.getItem() != newStack.getItem();' -Quiet)) {
        Add-ValidationError 'Gun component updates can regress to vanilla re-equip lowering after firing or ADS reconciliation.'
    }
    if (-not (Test-Path -LiteralPath $presentationSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'activeStackIdentity' -Quiet) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'previousWalkPhase' -Quiet) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'getGameTimeDeltaPartialTick(true)' -Quiet) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'interpolate(previousSwayX, swayX, partialTick)' -Quiet)) {
        Add-ValidationError 'The gun presentation no longer guarantees stack-keyed state and frame-interpolated movement/sway.'
    }
    if (-not (Test-Path -LiteralPath $rigSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'pose(-0.484375, 0.2340625, 0.18, 0.0F, 0.0F, 0.0F, 1.0F)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'pose(-0.484375, 0.21875, -0.09375, 0.0F, 0.0F, 0.0F, 1.0F)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new ModelPose(new Vec3(0.40, -0.70, -2.20)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(-1.0, 180.0, 0.0), 1.01F)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new ModelPose(new Vec3(-1.20, 0.55, -2.65)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(1.0, 180.0, 0.0), 0.99F)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(-0.65, -1.55, 0.50)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(0.75, -1.60, 0.45)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(-1.75, -0.50, 0.50)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(-0.50, -0.55, 0.45)' -Quiet)) {
        Add-ValidationError 'Target Pistol independent hip-fire or physical iron-sight ADS endpoint has regressed.'
    }
    if (-not (Test-Path -LiteralPath $bridgeSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $bridgeSource -SimpleMatch '"model_space"' -Quiet) -or
        -not (Select-String -LiteralPath $bridgeSource -SimpleMatch 'rootName + "_mesh"' -Quiet) -or
        -not (Select-String -LiteralPath $bridgeSource -SimpleMatch 'BoneRole.MUZZLE_FLASH' -Quiet)) {
        Add-ValidationError 'Target Pistol model-axis normalization can regress to rotating player arms with the HBM OBJ.'
    }
    if (-not (Test-Path -LiteralPath $rigSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(-81.0, -163.0, 167.0)' -Quiet) -or
        -not (Select-String -LiteralPath $rigSource -SimpleMatch 'new Vec3(-88.0, 161.0, 176.0)' -Quiet)) {
        Add-ValidationError 'Target Pistol player arms have regressed from the approved HBM grip pose.'
    }
    if (-not (Test-Path -LiteralPath $armRendererSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $armRendererSource -SimpleMatch 'player.getSkin().texture()' -Quiet)) {
        Add-ValidationError 'The first-person weapon renderer is not bound to the local player skin.'
    }
    if (-not (Test-Path -LiteralPath $armRendererSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $armRendererSource -SimpleMatch 'bone.getPivotY() + 7.0F' -Quiet) -or
        -not (Select-String -LiteralPath $armRendererSource -SimpleMatch 'part.yRot = (float) Math.PI' -Quiet) -or
        -not (Select-String -LiteralPath $armRendererSource -SimpleMatch 'part.zRot = (float) Math.PI' -Quiet)) {
        Add-ValidationError 'Target Pistol arms have regressed from Superb Warfare setupModelFromBone2 parity.'
    }
    if (-not (Test-Path -LiteralPath $calibrationSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'applyToModelSpace' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'hbm-target-pistol-calibration.json' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'GLFW.GLFW_KEY_F8' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'GLFW.GLFW_KEY_F10' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'applyToHandBone' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'ADS_POSE' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'adsPose(SuperbGunRig rig)' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'schema < 4' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'modelPoseForContext' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'TARGET_NON_FIRST_PERSON_POSE' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'adsPresentationBlend()' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'queueSave()' -Quiet)) {
        Add-ValidationError 'The Target Pistol endpoint-separated model/arm calibration and autosave path is missing.'
    }
    if (-not (Test-Path -LiteralPath $rendererSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $rendererSource -SimpleMatch 'applyToModelSpace(bone, rig, renderPerspective)' -Quiet)) {
        Add-ValidationError 'Target Pistol first-person calibration can leak into GUI or third-person display contexts.'
    }
    if (-not (Test-Path -LiteralPath $targetItemModel -PathType Leaf) -or
        -not (Select-String -LiteralPath $targetItemModel -SimpleMatch '"scale": [1.35, 1.35, 1.35]' -Quiet) -or
        -not (Select-String -LiteralPath $targetItemModel -SimpleMatch '"scale": [0.7, 0.7, 0.7]' -Quiet) -or
        -not (Select-String -LiteralPath $targetItemModel -SimpleMatch '"translation": [-1.25, -1, 0]' -Quiet)) {
        Add-ValidationError 'Target Pistol GUI scale or third-person item scale has regressed.'
    }
    if (-not (Test-Path -LiteralPath $armPoseSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $armPoseSource -SimpleMatch 'HumanoidModel.ArmPose.BOW_AND_ARROW' -Quiet) -or
        -not (Select-String -LiteralPath $armPoseSource -SimpleMatch 'HumanoidModel.ArmPose.CROSSBOW_CHARGE' -Quiet) -or
        -not (Test-Path -LiteralPath $clientSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $clientSource -SimpleMatch 'HbmGunArmPose.select' -Quiet)) {
        Add-ValidationError 'The Superb Warfare-style third-person gun arm pose is missing.'
    }
    if (-not (Test-Path -LiteralPath $presentationSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch '0.10F, 0.14F' -Quiet) -or
        (Select-String -LiteralPath $presentationSource -SimpleMatch '115.0F * drawAmount' -Quiet)) {
        Add-ValidationError 'Weapon movement cadence or clean equip transition has regressed.'
    }
    if (-not (Test-Path -LiteralPath $controllerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'boolean valid = isHoldingGun(minecraft);' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'boolean inputAllowed = valid && minecraft.screen == null;' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'if (!valid) {' -Quiet)) {
        Add-ValidationError 'Opening chat or inventory can invalidate and rotate the held weapon presentation again.'
    }
    if (-not (Test-Path -LiteralPath $weaponServiceSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $weaponServiceSource -SimpleMatch 'if (session.adsHeld() && player.isSprinting())' -Quiet) -or
        -not (Select-String -LiteralPath $weaponServiceSource -SimpleMatch 'player.setSprinting(false);' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'if (ads && wasSprinting)' -Quiet)) {
        Add-ValidationError 'ADS no longer authoritatively overrides sprint on both client and server.'
    }
    if (-not (Test-Path -LiteralPath $controllerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'boolean movingForward = minecraft.player != null' -Quiet) -or
        (Select-String -LiteralPath $controllerSource -SimpleMatch 'minecraft.player.input.hasForwardImpulse(),' -Quiet)) {
        Add-ValidationError 'Weapon movement intent can dereference the client player before a world is loaded.'
    }
    if (-not (Test-Path -LiteralPath $calibrationMarkerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $calibrationMarkerSource -SimpleMatch 'HBM_RIGHT_GRIP' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationMarkerSource -SimpleMatch 'renderFixedHandAnchor' -Quiet)) {
        Add-ValidationError 'The Target Pistol fixed-hand and HBM-grip calibration markers are missing.'
    }
    if (-not (Test-Path -LiteralPath $flareRendererSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $flareRendererSource -SimpleMatch $superbCommit -Quiet) -or
        -not (Select-String -LiteralPath $flareRendererSource -SimpleMatch 'RenderUtil.translateMatrixToBone' -Quiet) -or
        -not (Select-String -LiteralPath $flareRendererSource -SimpleMatch 'sourcePosition.x / 16.0D' -Quiet)) {
        Add-ValidationError 'The GPL-attributed model-local muzzle-flare renderer is missing.'
    }
    if (-not (Test-Path -LiteralPath $controllerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'case MUZZLE_FLASH -> {' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'if (localFirstPerson && HbmClientConfig.CASING_PARTICLES.get())' -Quiet) -or
        -not (Select-String -LiteralPath $rendererSource -SimpleMatch 'if (renderPerspective.firstPerson())' -Quiet)) {
        Add-ValidationError 'Muzzle flashes or casing effects can leak back into third-person rendering.'
    }
    if (-not (Test-Path -LiteralPath $controllerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'targetPistol ? "idle"' -Quiet)) {
        Add-ValidationError 'Target Pistol ADS and sprint pose clips can regress to double-transforming the procedural presentation root.'
    }
    if (-not (Test-Path -LiteralPath $controllerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'authoritativeMatchesHeldStack' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'SuperbGunPresentationState.tick(minecraft, valid,' -Quiet)) {
        Add-ValidationError 'Target Pistol presentation can regress to stale packet state surviving weapon/action transitions.'
    }
    if (-not (Test-Path -LiteralPath $ballisticsSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $ballisticsSource -SimpleMatch 'if (round.tracerColor == 0)' -Quiet)) {
        Add-ValidationError 'Zero-colour ammunition no longer suppresses server tracer events.'
    }
    if (-not (Test-Path -LiteralPath $controllerSource -PathType Leaf) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch $superbCommit -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'VanillaGuiLayers.CROSSHAIR' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'SuperbGunPresentationState.crosshairSpread()' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'renderAmmoPanel' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'drawHitFeedback' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'case HIT -> HIT_FEEDBACK.start' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'case HEADSHOT_KILL -> HIT_FEEDBACK.start' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'case HIT, HEADSHOT -> 0xFFFFFF' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'case KILL, HEADSHOT_KILL -> 0xFF2525' -Quiet) -or
        -not (Select-String -LiteralPath $ballisticsSource -SimpleMatch 'sendConfirmation' -Quiet) -or
        -not (Select-String -LiteralPath $ballisticsSource -SimpleMatch 'if (!damageApplied)' -Quiet) -or
        -not (Select-String -LiteralPath $ballisticsSource -SimpleMatch 'WeaponEffectType.HEADSHOT_KILL' -Quiet)) {
        Add-ValidationError 'The GPL-attributed Superb-style crosshair, hit feedback, or ammunition HUD is missing.'
    }
    if (-not (Select-String -LiteralPath $controllerSource -SimpleMatch 'int centerX = graphics.guiWidth() / 2;' -Quiet) -or
        -not (Select-String -LiteralPath $controllerSource -SimpleMatch 'minecraft.options.bobView().set(false)' -Quiet) -or
        -not (Select-String -LiteralPath $presentationSource -SimpleMatch 'float swayScale = 1.0F - ads;' -Quiet)) {
        Add-ValidationError 'Hip-fire reticle zero or complete ADS movement-bob suppression has regressed.'
    }
    if (-not (Select-String -LiteralPath $controllerSource -SimpleMatch 'suppressOffhandGun(RenderHandEvent event)' -Quiet) -or
        -not (Select-String -LiteralPath $rendererSource -SimpleMatch 'ItemDisplayContext.THIRD_PERSON_LEFT_HAND' -Quiet) -or
        -not (Select-String -LiteralPath $clientSource -SimpleMatch 'InteractionHand.OFF_HAND' -Quiet)) {
        Add-ValidationError 'Off-hand guns can render or re-enter the supported weapon pose path.'
    }
    if ((Select-String -LiteralPath $calibrationSource -SimpleMatch 'animatedRotX' -Quiet) -or
        -not (Select-String -LiteralPath $calibrationSource -SimpleMatch 'This must be absolute.' -Quiet)) {
        Add-ValidationError 'Target Pistol hand rotations can accumulate between frames and spin during ADS.'
    }
}

function Validate-WeaponCraftingAssets {
    $componentRecipes = @{
        'casing_small' = @{ Ingredient = 'hbm:plate_steel'; Stamp = 'c9' }
        'casing_rifle' = @{ Ingredient = 'hbm:plate_steel'; Stamp = 'c357' }
        'casing_shotshell' = @{ Ingredient = 'hbm:plate_steel'; Stamp = 'c44' }
        'casing_40mm' = @{ Ingredient = 'hbm:plate_steel'; Stamp = 'c50' }
        'projectile_lead_small' = @{ Ingredient = 'hbm:lead_ingot'; Stamp = 'c9' }
        'projectile_steel_small' = @{ Ingredient = 'hbm:steel_ingot'; Stamp = 'c9' }
        'projectile_lead_rifle' = @{ Ingredient = 'hbm:lead_ingot'; Stamp = 'c357' }
        'projectile_steel_rifle' = @{ Ingredient = 'hbm:steel_ingot'; Stamp = 'c357' }
        'pellets_lead' = @{ Ingredient = 'hbm:lead_ingot'; Stamp = 'flat' }
        'slug_lead' = @{ Ingredient = 'hbm:lead_ingot'; Stamp = 'c44' }
        'projectile_40mm_he' = @{ Ingredient = 'hbm:steel_ingot'; Stamp = 'c50' }
        'projectile_40mm_heat' = @{ Ingredient = 'hbm:plate_lead'; Stamp = 'c50' }
    }
    $ammoRecipes = @{
        'p22_fmj' = @('hbm:projectile_lead_small', 'hbm:casing_small')
        'p22_ap' = @('hbm:projectile_steel_small', 'hbm:casing_small')
        'r556_fmj' = @('hbm:projectile_lead_rifle', 'hbm:casing_rifle')
        'r556_ap' = @('hbm:projectile_steel_rifle', 'hbm:casing_rifle')
        'g12_buckshot' = @('hbm:pellets_lead', 'hbm:casing_shotshell')
        'g12_slug' = @('hbm:slug_lead', 'hbm:casing_shotshell')
        'g40_he' = @('hbm:projectile_40mm_he', 'hbm:casing_40mm')
        'g40_heat' = @('hbm:projectile_40mm_heat', 'hbm:casing_40mm')
    }

    foreach ($component in $componentRecipes.Keys) {
        Validate-ItemModel "hbm:$component" 'minecraft:item/generated'
        $path = Join-Path $resourcesRoot "data\hbm\recipe\burner_press\$component.json"
        $recipe = if (Test-Path -LiteralPath $path -PathType Leaf) { Read-JsonDocument $path "Burner Press recipe $component" } else { $null }
        if ($null -eq $recipe) {
            Add-ValidationError "Missing Burner Press recipe for hbm:$component."
            continue
        }
        $expected = $componentRecipes[$component]
        $ingredient = Get-JsonProperty (Get-JsonProperty $recipe 'ingredient') 'item'
        $stamp = Get-JsonProperty $recipe 'stamp'
        $result = Get-JsonProperty (Get-JsonProperty $recipe 'result') 'id'
        if ($ingredient -ne $expected.Ingredient -or $stamp -ne $expected.Stamp -or $result -ne "hbm:$component") {
            Add-ValidationError "Burner Press recipe '$component' does not match its approved ingredient, stamp, and result mapping."
        }
    }

    foreach ($ammo in $ammoRecipes.Keys) {
        Validate-ItemModel "hbm:$ammo" 'minecraft:item/generated'
        $path = Join-Path $resourcesRoot "data\hbm\recipe\ammo\$ammo.json"
        $recipe = if (Test-Path -LiteralPath $path -PathType Leaf) { Read-JsonDocument $path "ammo recipe $ammo" } else { $null }
        if ($null -eq $recipe) {
            Add-ValidationError "Missing survival recipe for hbm:$ammo."
            continue
        }
        $key = Get-JsonProperty $recipe 'key'
        $ingredients = @()
        if (Test-JsonObject $key) {
            foreach ($entry in $key.PSObject.Properties) {
                $ingredients += Get-JsonProperty $entry.Value 'item'
            }
        }
        $expected = $ammoRecipes[$ammo]
        foreach ($item in @($expected[0], $expected[1], 'minecraft:gunpowder')) {
            if ($item -notin $ingredients) {
                Add-ValidationError "Ammo recipe '$ammo' is missing required ingredient '$item'."
            }
        }
        $result = Get-JsonProperty (Get-JsonProperty $recipe 'result') 'id'
        if ($result -ne "hbm:$ammo") {
            Add-ValidationError "Ammo recipe '$ammo' produces '$result' instead of 'hbm:$ammo'."
        }
    }

    foreach ($recipePath in @(
        'data\hbm\recipe\steel_ingot.json',
        'data\hbm\recipe\burner_press.json',
        'data\hbm\recipe\stamps\stamp_flat.json',
        'data\hbm\recipe\stamps\stamp_plate.json',
        'data\hbm\recipe\stamps\stamp_9.json',
        'data\hbm\recipe\stamps\stamp_357.json',
        'data\hbm\recipe\stamps\stamp_44.json',
        'data\hbm\recipe\stamps\stamp_50.json',
        'data\hbm\recipe\burner_press\plate_steel.json',
        'data\hbm\recipe\burner_press\plate_lead.json'
    )) {
        $path = Join-Path $resourcesRoot $recipePath
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            Add-ValidationError "Pilot ammunition survival chain is missing '$recipePath'."
        } else {
            [void](Read-JsonDocument $path "survival-chain recipe $recipePath")
        }
    }
}

Validate-PilotLegacyAssets
Validate-PilotRuntimeAssets
Validate-WeaponCraftingAssets

$gunDefinitions = @{}
$ammoDefinitions = @{}

if (-not (Test-Path -LiteralPath $gunRoot -PathType Container)) {
    Add-ValidationError "Built-in gun definition directory is missing: '$gunRoot'."
} else {
    $gunFiles = @(Get-ChildItem -LiteralPath $gunRoot -Recurse -File -Filter '*.json' | Sort-Object FullName)
    if ($gunFiles.Count -eq 0) {
        Add-ValidationError "No built-in gun definitions were found under '$gunRoot'."
    }
    foreach ($file in $gunFiles) {
        $id = Get-DefinitionId $file $gunRoot 'hbm'
        $scope = "gun $id ($($file.FullName))"
        $json = Read-JsonDocument $file.FullName $scope
        if ($null -ne $json) {
            $gunDefinitions[$id] = Validate-GunDefinition $json $id $scope
        }
    }
}

if (-not (Test-Path -LiteralPath $ammoRoot -PathType Container)) {
    Add-ValidationError "Built-in ammo definition directory is missing: '$ammoRoot'."
} else {
    $ammoFiles = @(Get-ChildItem -LiteralPath $ammoRoot -Recurse -File -Filter '*.json' | Sort-Object FullName)
    if ($ammoFiles.Count -eq 0) {
        Add-ValidationError "No built-in ammo definitions were found under '$ammoRoot'."
    }
    foreach ($file in $ammoFiles) {
        $id = Get-DefinitionId $file $ammoRoot 'hbm'
        $scope = "ammo $id ($($file.FullName))"
        $json = Read-JsonDocument $file.FullName $scope
        if ($null -ne $json) {
            $ammoDefinitions[$id] = Validate-AmmoDefinition $json $id $scope
        }
    }
}

foreach ($gun in $gunDefinitions.Values) {
    if ($null -eq $gun) {
        continue
    }
    foreach ($ammoId in $gun.SupportedAmmo) {
        if (-not $ammoDefinitions.ContainsKey($ammoId)) {
            Add-ValidationError "Gun $($gun.Id) references missing ammo $ammoId."
            continue
        }
        $ammo = $ammoDefinitions[$ammoId]
        if ($null -ne $gun.AmmoFamily -and $null -ne $ammo.Family -and $gun.AmmoFamily -ne $ammo.Family) {
            Add-ValidationError "Gun $($gun.Id) expects family $($gun.AmmoFamily), but $ammoId belongs to $($ammo.Family)."
        }
    }
}

foreach ($warning in $script:ValidationWarnings) {
    Write-Output "[weapon-validator] WARNING: $warning"
}

if ($script:ValidationErrors.Count -gt 0) {
    foreach ($validationError in $script:ValidationErrors) {
        Write-Output "[weapon-validator] ERROR: $validationError"
    }
    Write-Output "[weapon-validator] FAILED with $($script:ValidationErrors.Count) error(s)."
    exit 1
}

Write-Output "[weapon-validator] PASS: validated $($gunDefinitions.Count) gun definition(s), $($ammoDefinitions.Count) ammo definition(s), HBM sounds, animation-to-OBJ bone binding, item models, the survival ammunition chain, and four pilot legacy asset sets."
exit 0
