<p align="right">
  <a href="CODEX_ENV.zh_CN.md">简体中文</a> · <strong>English</strong>
</p>

# Codex Development Environment

This is the machine-specific, verified environment record for the AI Passport
checkout used by Codex. It complements the portable setup guide. New tasks read
root `AGENTS.md` first and consult this file only when environment details are
needed. The snapshot below was verified on 2026-10-15.

## Repository layout

- Repository root: `C:\Users\MPHNUC\Documents\ChatGPT\AI Passport\ai-passport-fork-checkout\ai-passport-main`
- Host: Windows 11 build 26300, x64; Codex commands use PowerShell 7.6.5.
- Android root: `android-app`; the only Gradle module and application module is `app`.
- ESP-IDF project root: the repository root (`CMakeLists.txt`, `main`, `components`,
  `sdkconfig.defaults`, and `partitions.csv`).
- Protocol documentation: `docs/protocols/passport-companion-ble.md`.
- Firmware protocol/link code: `main/passport_ble_ids.h`,
  `main/passport_ble_link.c`, `main/passport_location_protocol.*`, and the
  trip/location model and store files in `main`.
- Android protocol/link code: `android-app/app/src/main/java/com/folotoy/passport/PassportProtocol.kt`
  and `PassportBleService.kt`; local records are in `DataStore.kt`.
- Android and firmware are directories in the same Git repository. There are no
  nested repositories or submodules involved in this application.
- Portable setup guidance remains in
  `docs/development/engineering/environment-setup.md`. The local, idempotent
  entry point is `tools/codex-env.ps1`.

## Android

### SDK

- SDK: `C:\Users\MPHNUC\AppData\Local\Android\Sdk`, also recorded in the ignored
  `android-app/local.properties`.
- SDK adb: `C:\Users\MPHNUC\AppData\Local\Android\Sdk\platform-tools\adb.exe`,
  version 1.0.41 / platform-tools 37.0.1.
- Installed platforms used by this project are named `android-37.0` and
  `android-36.1`; installed build tools include 36.0.0.
- The Android project does not configure NDK, CMake, or `externalNativeBuild`.

### JDK and Gradle

- JDK: Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr`.
- Runtime: OpenJDK 25.0.2. Java source and target compatibility are 17.
- Always use `android-app\gradlew.bat`; do not use a system Gradle installation.
- Gradle Wrapper: 9.6.1. Android Gradle Plugin: 9.2.0. Compose compiler plugin:
  2.2.10.
- `compileSdk`: 37.0; `targetSdk`: 36; `minSdk`: 26.

### Standard commands

From the repository root in PowerShell:

```powershell
. .\tools\codex-env.ps1 -Scope Android
Push-Location android-app
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
Pop-Location
```

The usual combined gate is:

```powershell
. .\tools\codex-env.ps1 -Scope Android
Push-Location android-app
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
Pop-Location
```

Useful physical-device commands use the SDK adb placed first on `PATH` by the
environment script:

```powershell
adb devices -l
adb install -r .\android-app\app\build\outputs\apk\debug\app-debug.apk
adb logcat -c
adb logcat -v threadtime
```

Use `adb devices -l` each time; do not persist a device serial. A physical phone
was validated over wireless adb, but connection transport and serial are session
state. Emulator use is not required by the project; if used, select it from the
same `adb devices -l` output.

## ESP-IDF

### Installation and activation

- Installation: Espressif's official Windows Tools Installer layout under
  `C:\Espressif`, confirmed by `esp_idf.json`, `idf-env.json`, `idf-env.exe`, and
  the installer-provided initialization scripts.
- `IDF_PATH`: `C:\Espressif\frameworks\esp-idf-v5.5.3`.
- `IDF_TOOLS_PATH`: `C:\Espressif`.
- Version: exactly ESP-IDF v5.5.3. The repository also requires 5.5.3; do not
  substitute another installed release.
- Standard entry for every new PowerShell process:

  ```powershell
  . .\tools\codex-env.ps1 -Scope EspIdf
  ```

  The leading dot and space are required. They keep the generated ESP-IDF
  environment and PowerShell functions in the current shell. The script invokes
  the installed environment's official `activate.py --export`, selects its
  generated PowerShell script, and dot-sources it. It does not install, download,
  or create a new environment.

### Python and tools

- ESP-IDF Python: `C:\Espressif\python_env\idf5.5_py3.11_env\Scripts\python.exe`.
- Python version: 3.11.2. This is the installer-managed ESP-IDF virtual
  environment and contains `idf.py` dependencies and esptool. Do not create a
  project venv unless this environment is proven unusable.
- `idf.py` source: `C:\Espressif\frameworks\esp-idf-v5.5.3\tools\idf.py`.
- Target: ESP32-C3, RISC-V, 8 MB flash, USB Serial/JTAG console, no PSRAM.
- Compiler: `riscv32-esp-elf-gcc` 14.2.0 from
  `C:\Espressif\tools\riscv32-esp-elf\esp-14.2.0_20251107\riscv32-esp-elf\bin`.
- CMake 3.30.2 and Ninja 1.12.1 are installer-managed under `C:\Espressif\tools`.
- OpenOCD: 0.12.0-esp32-20251215 under `C:\Espressif\tools\openocd-esp32`.
- esptool: 4.12.dev1 from the ESP-IDF Python environment.

### Build and inspection

From the repository root after activation:

```powershell
idf.py --version
idf.py reconfigure
idf.py build
idf.py size
idf.py size-components
idf.py menuconfig
idf.py fullclean
```

`idf.py set-target esp32c3` is only needed when creating or repairing a build
configuration. It can rename a local `sdkconfig`, so inspect and preserve
intentional settings first. `fullclean` removes build output and is not a general
environment repair step.

The repository's portable gates remain authoritative in CI and POSIX development
environments. On this native Windows host, run the equivalent isolated firmware
build, merged-image verification, and archive flow through native PowerShell:

```powershell
.\tools\validate-firmware.ps1
```

Do not run the ESP-IDF firmware gate through Git Bash/MSYS/MinGW on this host.
ESP-IDF v5.5.3 explicitly rejects that environment, so the Bash command cannot
produce a current merged image even though Git Bash itself starts successfully.

The repository/document check is also directly available and verified:

```powershell
python tools\check_repo.py
```

The native-Windows `--static`/complete gate is not recorded as a working command:
its host C tests use a GCC-compatible command line, while this machine currently
has Visual Studio `cl.exe` and an ESP-Clang whose default target is RISC-V rather
than a Windows host target. Use the Android unit-test gate above for companion-app
host tests, and use CI or a confirmed POSIX host compiler for the complete
repository host-test suite. Do not point `CC` at ESP-Clang for these tests.

### Flash, monitor, and serial discovery

The board uses ESP32-C3 native USB Serial/JTAG for its console and normal
development flashing. The previously measured device identity was Espressif
VID:PID `303A:1001`; the COM number is temporary and must be discovered for each
session:

```powershell
. .\tools\codex-env.ps1 -Scope EspIdf
python -m serial.tools.list_ports -v
```

Close other monitors before opening the port. Automatic reset into the bootloader
worked in the validated setup; do not require BOOT/RESET unless automatic entry
actually fails. Use 460800 baud for flashing and `Ctrl+]` to exit the ESP-IDF
monitor.

```powershell
idf.py -p <COMx> flash
idf.py -p <COMx> monitor
idf.py -p <COMx> flash monitor
python -m esptool --chip esp32c3 -p <COMx> -b 460800 write-flash 0x0 build\BugChecking-full.bin
```

The last command is the preferred delivery-image flow. The ordinary
`build\BugChecking.bin` is app-only and belongs at `0x10000`, never at
`0x0`. Flashing always requires explicit user authorization and does not imply a
full-chip erase.

## Android to ESP32 communication

The implemented companion link is BLE GATT: Passport is the peripheral and the
Android app is the central. There is no current Wi-Fi, classic Bluetooth, USB, or
serial application data path between them.

- UUID string and firmware byte-array definitions: `main/passport_ble_ids.h`.
- Android UUIDs, frame encoding/decoding, snapshot CRC-32/ISO-HDLC, opcodes, and
  fragment reassembly: `PassportProtocol.kt`.
- Android connection, MTU 247 request, acknowledged serialized writes, retry,
  reconnect, foreground service, location, and result handling:
  `PassportBleService.kt`.
- Firmware GATT service, authenticated indicate/notify request transport, write
  handlers, and transmit sequencing: `main/passport_ble_link.c`.
- Shared protocol authority, packet limits, time/result frames, and snapshot
  semantics: `docs/protocols/passport-companion-ble.md`.

Do not duplicate UUIDs or packet tables here; update the protocol document and
both implementations together when the protocol changes.

## Troubleshooting

Run `tools/codex-env.ps1` first. If one recorded component fails, investigate
that component and the exact error rather than rescanning all SDKs and tools.
Update this document and the script only after confirming a real environment
change.

### Known dead ends

- An unprepared shell has no `JAVA_HOME`; invoking the Gradle Wrapper there fails
  even though Android Studio's JBR is installed. Activate the Android scope first.
- Bare `adb` before activation resolves to Touch Portal's bundled adb under
  `C:\Program Files (x86)\Touch Portal\plugins\adb`, not this project's SDK adb.
- Bare `python` before ESP-IDF activation resolves to the WindowsApps launcher,
  not the ESP-IDF virtual environment.
- Bare `idf.py` in an ordinary shell may be unavailable or resolve to an
  unrelated host Python package reporting `v1.0.3`; it is not ESP-IDF. CMake,
  Ninja, GCC, and OpenOCD are also intentionally unavailable before activation.
  None of these results means the toolchain is missing. The project environment
  script binds `idf.py` explicitly to ESP-IDF v5.5.3.
- Directly dot-sourcing ESP-IDF's `export.ps1` in Codex's current non-interactive
  PowerShell 7.6.5 captures activation progress plus the generated script path;
  it then tries to dot-source an array and fails. Use `tools/codex-env.ps1`, which
  safely selects the generated script from the official activation output.
- Running `tools/codex-env.ps1` in a child PowerShell and then returning to the
  parent cannot preserve PowerShell functions. Dot-source it in the shell that
  will run `idf.py`.
- In a managed Codex filesystem sandbox, ESP-IDF activation can fail with Windows
  error 623 (`invalid system DLL relocation`) even though the recorded toolchain
  is intact. Retry the same native PowerShell command once outside the sandbox;
  do not rescan, reinstall, create another virtual environment, or switch to Git
  Bash. If that one retry fails, report the concrete activation error and stop.
- `./tools/validate.sh --firmware` is a POSIX/CI command, not a native-Windows
  workaround. ESP-IDF v5.5.3 rejects the installer Git Bash as MSYS/MinGW. Use
  `.\tools\validate-firmware.ps1` on this host.
- `./tools/validate.sh --static` stops at `cc: command not found` on this host.
  Run `python tools\check_repo.py` and the Android gate locally; leave the GCC
  host-C suite to CI or a confirmed POSIX host compiler.
- Do not use system Gradle, create another Python venv, reinstall ESP-IDF, or
  download duplicate tools while the recorded paths validate.
- Do not set `CC` to the installed `esp-clang` for repository host tests. Its
  default target is `riscv32-esp-unknown-elf`, so the resulting ELF cannot run on
  Windows. Visual Studio `cl.exe` is installed, but the current shell gate uses
  GCC-style flags and is not a drop-in replacement.
- ESP32-C3 uses the RISC-V toolchain. Selecting an Xtensa target/toolchain or
  changing the target merely because Xtensa tools are present is incorrect.
- Do not hard-code a previous COM port, Android device serial, temporary IP,
  connection handle, or build-directory state.
- Do not flash the app-only binary at `0x0`, and do not erase all flash as a
  routine recovery step.
