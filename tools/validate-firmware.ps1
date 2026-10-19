[CmdletBinding()]
param(
    [switch]$KeepBuildDirectory
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$environmentScript = Join-Path $PSScriptRoot "codex-env.ps1"
$buildRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("bugchecking-firmware-" + [guid]::NewGuid().ToString("N"))
$sdkconfigPath = Join-Path $buildRoot "sdkconfig"
$mergedImage = Join-Path $buildRoot "BugChecking-full.bin"
$deliveryDirectory = Join-Path $repoRoot "build"
$archiveDirectory = Join-Path $deliveryDirectory "firmware"
$deliveryImage = Join-Path $deliveryDirectory "BugChecking-full.bin"
$previousSdkconfigDefaults = $env:SDKCONFIG_DEFAULTS
$previousIdfTarget = $env:IDF_TARGET

function Invoke-CheckedPython {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & $env:AI_PASSPORT_IDF_PYTHON @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $($Arguments -join ' ')"
    }
}

try {
    . $environmentScript -Scope EspIdf

    New-Item -ItemType Directory -Path $buildRoot | Out-Null
    $env:SDKCONFIG_DEFAULTS = Join-Path $repoRoot "sdkconfig.defaults"
    $env:IDF_TARGET = "esp32c3"
    $idfPy = Join-Path $env:IDF_PATH "tools\idf.py"

    Invoke-CheckedPython -Arguments @($idfPy, "-B", $buildRoot, "-D", "SDKCONFIG=$sdkconfigPath", "build")
    Invoke-CheckedPython -Arguments @($idfPy, "-B", $buildRoot, "merge-bin", "-o", $mergedImage)
    Invoke-CheckedPython -Arguments @((Join-Path $repoRoot "tools\verify_firmware.py"), $buildRoot)
    Invoke-CheckedPython -Arguments @((Join-Path $repoRoot "tools\archive_firmware.py"), "create", $buildRoot, "--archive-root", $archiveDirectory)

    New-Item -ItemType Directory -Force -Path $deliveryDirectory | Out-Null
    Copy-Item -LiteralPath $mergedImage -Destination $deliveryImage -Force

    Write-Host "Firmware build: PASS"
    Write-Host "  merged image: $deliveryImage"
    Write-Host "  debug archive: $archiveDirectory"
}
finally {
    $env:SDKCONFIG_DEFAULTS = $previousSdkconfigDefaults
    $env:IDF_TARGET = $previousIdfTarget

    if ($KeepBuildDirectory) {
        Write-Host "  temporary build: $buildRoot"
    }
    elseif (Test-Path -LiteralPath $buildRoot) {
        $resolvedBuildRoot = (Resolve-Path -LiteralPath $buildRoot).Path
        $resolvedTempRoot = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path.TrimEnd('\')
        $expectedPrefix = "$resolvedTempRoot\bugchecking-firmware-"
        if (-not $resolvedBuildRoot.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove unexpected build directory: $resolvedBuildRoot"
        }
        Remove-Item -LiteralPath $resolvedBuildRoot -Recurse -Force
    }
}
