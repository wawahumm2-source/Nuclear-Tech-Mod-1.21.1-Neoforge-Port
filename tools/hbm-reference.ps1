param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ReferenceArgs
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$indexScript = Join-Path $scriptRoot "reference_index.py"
$bundledPython = "C:\Users\wawah\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"

if (Test-Path -LiteralPath $bundledPython) {
    $python = $bundledPython
} else {
    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if ($null -eq $pythonCommand) {
        throw "Python was not found. Install Python or run from Codex with the bundled runtime available."
    }
    $python = $pythonCommand.Source
}

& $python $indexScript @ReferenceArgs
