[CmdletBinding()]
param(
    [ValidateSet("All", "Android", "EspIdf")]
    [string]$Scope = "All"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $repoRoot "android-app"
$androidSdk = "C:\Users\MPHNUC\AppData\Local\Android\Sdk"
$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$idfToolsPath = "C:\Espressif"
$idfPath = "C:\Espressif\frameworks\esp-idf-v5.5.3"
$idfPython = "C:\Espressif\python_env\idf5.5_py3.11_env\Scripts\python.exe"

function Assert-ExistingPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Component
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Component is missing at the recorded path: $Path. Update tools/codex-env.ps1 and docs/CODEX_ENV.md only after confirming the replacement."
    }
}

function Add-ProcessPath {
    param([Parameter(Mandatory = $true)][string]$Path)

    $entries = @($env:Path -split ";" | Where-Object { $_ })
    if ($entries -notcontains $Path) {
        $env:Path = "$Path;$env:Path"
    }
}

function Enable-AndroidEnvironment {
    Assert-ExistingPath -Path $androidRoot -Component "Android project"
    Assert-ExistingPath -Path $androidSdk -Component "Android SDK"
    Assert-ExistingPath -Path (Join-Path $androidSdk "platform-tools\adb.exe") -Component "Android SDK adb"
    Assert-ExistingPath -Path (Join-Path $javaHome "bin\java.exe") -Component "Android Studio JBR"

    $env:JAVA_HOME = $javaHome
    $env:ANDROID_HOME = $androidSdk
    $env:ANDROID_SDK_ROOT = $androidSdk
    Add-ProcessPath -Path (Join-Path $javaHome "bin")
    Add-ProcessPath -Path (Join-Path $androidSdk "platform-tools")

    Write-Host "Android environment ready"
    Write-Host "  project: $androidRoot"
    Write-Host "  JAVA_HOME: $javaHome"
    Write-Host "  Android SDK: $androidSdk"
    Write-Host "  Gradle entry: android-app\gradlew.bat"
}

function Enable-EspIdfEnvironment {
    $activatePy = Join-Path $idfPath "tools\activate.py"
    $idfPy = Join-Path $idfPath "tools\idf.py"

    Assert-ExistingPath -Path $idfPath -Component "ESP-IDF"
    Assert-ExistingPath -Path $idfPython -Component "ESP-IDF Python virtual environment"
    Assert-ExistingPath -Path $activatePy -Component "ESP-IDF activation script"
    Assert-ExistingPath -Path $idfPy -Component "idf.py"

    $env:IDF_TOOLS_PATH = $idfToolsPath
    $env:IDF_PATH = $idfPath

    # In this host's PowerShell 7 non-interactive sessions, export.ps1 captures
    # activate.py's progress output as well as the generated script path and then
    # fails to dot-source the resulting array. Use the same official activation
    # backend, select its generated script explicitly, and dot-source that script.
    $activationOutput = @(& $idfPython $activatePy --export 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $activationOutput | ForEach-Object { Write-Error $_.ToString() }
        throw "ESP-IDF activate.py failed with exit code $LASTEXITCODE."
    }

    $activationScript = $activationOutput |
        ForEach-Object { $_.ToString().Trim() } |
        Where-Object { $_ -match "\.ps1$" -and (Test-Path -LiteralPath $_) } |
        Select-Object -Last 1

    if (-not $activationScript) {
        throw "ESP-IDF activation did not emit a usable PowerShell environment script."
    }

    . $activationScript

    # A host Python package also exposes an unrelated idf.py (v1.0.3). Bind the
    # project command explicitly so command lookup cannot select that package.
    $env:AI_PASSPORT_IDF_PYTHON = $idfPython
    $env:PYTHON_BIN = $idfPython.Replace("\", "/")
    function global:idf.py {
        & $env:AI_PASSPORT_IDF_PYTHON "$env:IDF_PATH\tools\idf.py" @args
    }

    $idfVersion = (idf.py --version).Trim()
    if ($LASTEXITCODE -ne 0 -or $idfVersion -ne "ESP-IDF v5.5.3") {
        throw "Expected ESP-IDF v5.5.3, got '$idfVersion'."
    }

    Write-Host "ESP-IDF environment ready"
    Write-Host "  IDF_PATH: $idfPath"
    Write-Host "  IDF_TOOLS_PATH: $idfToolsPath"
    Write-Host "  Python: $idfPython"
    Write-Host "  version: $idfVersion"
    Write-Host "  target: esp32c3 (RISC-V)"
}

if ($MyInvocation.InvocationName -ne ".") {
    Write-Warning "Dot-source this script so ESP-IDF functions and environment remain available: . .\tools\codex-env.ps1 -Scope $Scope"
}

switch ($Scope) {
    "All" {
        Enable-AndroidEnvironment
        Enable-EspIdfEnvironment
    }
    "Android" { Enable-AndroidEnvironment }
    "EspIdf" { Enable-EspIdfEnvironment }
}

Write-Host "Repository: $repoRoot"
