Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$textureRoot = Join-Path $root "src\main\resources\assets\hbm\textures\block"
$modelRoot = Join-Path $root "src\main\resources\assets\hbm\models\block"
$blockstatePath = Join-Path $root "src\main\resources\assets\hbm\blockstates\sellafield.json"

$sourceTextures = @(
    "sellafield_slaked.png",
    "sellafield_slaked_1.png",
    "sellafield_slaked_2.png",
    "sellafield_slaked_3.png"
)
$targetColors = @(
    @(0x4C7939, 0x41463F),
    @(0x418223, 0x3E443B),
    @(0x338C0E, 0x3B5431),
    @(0x1C9E00, 0x394733),
    @(0x02B200, 0x37492F),
    @(0x00D300, 0x324C26)
)
$sourceLight = 0x858384
$sourceDark = 0x434343

function Get-Component([int]$color, [int]$shift) {
    return ($color -shr $shift) -band 0xFF
}

function Remap-Component([int]$component, [int]$shift, [int]$targetLight, [int]$targetDark) {
    $boundLight = Get-Component $sourceLight $shift
    $boundDark = Get-Component $sourceDark $shift
    $light = Get-Component $targetLight $shift
    $dark = Get-Component $targetDark $shift
    $position = ($component - $boundLight) / ($boundDark - $boundLight)
    return ([int][Math]::Truncate($light + $position * ($dark - $light))) -band 0xFF
}

$variantEntries = New-Object System.Collections.Generic.List[string]
for ($level = 0; $level -lt $targetColors.Count; $level++) {
    $targetLight = $targetColors[$level][0]
    $targetDark = $targetColors[$level][1]

    for ($variant = 0; $variant -lt $sourceTextures.Count; $variant++) {
        $sourcePath = Join-Path $textureRoot $sourceTextures[$variant]
        $bitmap = [System.Drawing.Bitmap]::new($sourcePath)
        $output = [System.Drawing.Bitmap]::new(
            $bitmap.Width,
            $bitmap.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                for ($y = 0; $y -lt $bitmap.Height; $y++) {
                    $pixel = $bitmap.GetPixel($x, $y)
                    $red = Remap-Component $pixel.R 16 $targetLight $targetDark
                    $green = Remap-Component $pixel.G 8 $targetLight $targetDark
                    $blue = Remap-Component $pixel.B 0 $targetLight $targetDark
                    $output.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $red, $green, $blue))
                }
            }

            $name = "sellafield_${level}_${variant}"
            $output.Save(
                (Join-Path $textureRoot ($name + ".png")),
                [System.Drawing.Imaging.ImageFormat]::Png
            )
            $model = @"
{
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "hbm:block/$name" }
}
"@
            [System.IO.File]::WriteAllText(
                (Join-Path $modelRoot ($name + ".json")),
                $model,
                [System.Text.UTF8Encoding]::new($false)
            )
            $variantEntries.Add("    `"level=$level,variant=$variant`": { `"model`": `"hbm:block/$name`" }")
        } finally {
            $output.Dispose()
            $bitmap.Dispose()
        }
    }
}

$blockstate = "{`r`n  `"variants`": {`r`n" + ($variantEntries -join ",`r`n") + "`r`n  }`r`n}`r`n"
[System.IO.File]::WriteAllText($blockstatePath, $blockstate, [System.Text.UTF8Encoding]::new($false))

$oreOverlays = @(
    "diamond",
    "emerald",
    "uranium_scorched",
    "schrabidium",
    "radgem"
)
foreach ($ore in $oreOverlays) {
    $base = [System.Drawing.Bitmap]::new((Join-Path $textureRoot "sellafield_slaked.png"))
    $overlay = [System.Drawing.Bitmap]::new((Join-Path $textureRoot ("ore_overlay_" + $ore + ".png")))
    $output = [System.Drawing.Bitmap]::new(
        $base.Width,
        $base.Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($output)
    try {
        $graphics.DrawImageUnscaled($base, 0, 0)
        $graphics.DrawImageUnscaled($overlay, 0, 0)
        $output.Save(
            (Join-Path $textureRoot ("ore_sellafield_" + $ore + "_item.png")),
            [System.Drawing.Imaging.ImageFormat]::Png
        )
    } finally {
        $graphics.Dispose()
        $output.Dispose()
        $overlay.Dispose()
        $base.Dispose()
    }
}

Write-Host "Generated Sellafite tier assets plus five source-style ore item composites."
