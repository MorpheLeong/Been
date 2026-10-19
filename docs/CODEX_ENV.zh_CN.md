<p align="right">
  <strong>简体中文</strong> · <a href="CODEX_ENV.md">English</a>
</p>

# Codex 开发环境

本文记录 Codex 使用的 AI Passport checkout 在本机上已经验证的开发环境，作为
可移植环境准备指南的补充。新任务先读根目录 `AGENTS.md`；只有需要环境细节时
才读本文。以下快照验证于 2026-10-15。

## 仓库布局

- 仓库根目录：`C:\Users\MPHNUC\Documents\ChatGPT\AI Passport\ai-passport-fork-checkout\ai-passport-main`
- 主机：Windows 11 build 26300、x64；Codex 命令使用 PowerShell 7.6.5。
- Android 根目录：`android-app`；唯一的 Gradle module 和应用 module 均为 `app`。
- ESP-IDF 项目根目录：仓库根目录（`CMakeLists.txt`、`main`、`components`、
  `sdkconfig.defaults` 与 `partitions.csv`）。
- 协议文档：`docs/protocols/passport-companion-ble.zh_CN.md`。
- 固件协议与连接代码：`main/passport_ble_ids.h`、`main/passport_ble_link.c`、
  `main/passport_location_protocol.*`，以及 `main` 中的轨迹/位置 model 与 store 文件。
- Android 协议与连接代码：`android-app/app/src/main/java/com/folotoy/passport/PassportProtocol.kt`
  与 `PassportBleService.kt`；本地记录位于 `DataStore.kt`。
- Android 和固件位于同一个 Git 仓库中。本应用不涉及嵌套仓库或 submodule。
- 可移植环境准备规范仍位于
  `docs/development/engineering/environment-setup.zh_CN.md`。本机幂等入口是
  `tools/codex-env.ps1`。

## Android

### SDK

- SDK：`C:\Users\MPHNUC\AppData\Local\Android\Sdk`，同时记录在被忽略的
  `android-app/local.properties` 中。
- SDK adb：`C:\Users\MPHNUC\AppData\Local\Android\Sdk\platform-tools\adb.exe`，
  版本 1.0.41 / platform-tools 37.0.1。
- 本项目使用的已安装平台目录名为 `android-37.0` 和 `android-36.1`；已安装
  build tools 包含 36.0.0。
- Android 项目未配置 NDK、CMake 或 `externalNativeBuild`。

### JDK 与 Gradle

- JDK：Android Studio JBR，路径为 `C:\Program Files\Android\Android Studio\jbr`。
- 运行时：OpenJDK 25.0.2。Java source/target compatibility 为 17。
- 始终使用 `android-app\gradlew.bat`，不要使用系统 Gradle。
- Gradle Wrapper：9.6.1；Android Gradle Plugin：9.2.0；Compose compiler plugin：
  2.2.10。
- `compileSdk`：37.0；`targetSdk`：36；`minSdk`：26。

### 标准命令

在仓库根目录的 PowerShell 中执行：

```powershell
. .\tools\codex-env.ps1 -Scope Android
Push-Location android-app
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
Pop-Location
```

常用组合门禁为：

```powershell
. .\tools\codex-env.ps1 -Scope Android
Push-Location android-app
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
Pop-Location
```

真机调试使用环境脚本放到 `PATH` 首位的 SDK adb：

```powershell
adb devices -l
adb install -r .\android-app\app\build\outputs\apk\debug\app-debug.apk
adb logcat -c
adb logcat -v threadtime
```

每次用 `adb devices -l` 获取设备，不要持久化设备 serial。已经验证过无线 adb
连接实体手机，但连接方式和 serial 都属于 session 状态。项目不强制使用 emulator；
如需使用，也从同一条 `adb devices -l` 输出选择。

## ESP-IDF

### 安装与激活

- 安装来源：乐鑫官方 Windows Tools Installer，目录位于 `C:\Espressif`；已由
  `esp_idf.json`、`idf-env.json`、`idf-env.exe` 与安装器初始化脚本确认。
- `IDF_PATH`：`C:\Espressif\frameworks\esp-idf-v5.5.3`。
- `IDF_TOOLS_PATH`：`C:\Espressif`。
- 版本：严格为 ESP-IDF v5.5.3。仓库也要求 5.5.3，不得替换为其他已安装版本。
- 每个新 PowerShell process 的标准入口：

  ```powershell
  . .\tools\codex-env.ps1 -Scope EspIdf
  ```

  开头的点和空格不可省略；它们让生成的 ESP-IDF 环境和 PowerShell function
  留在当前 shell。脚本调用现有环境官方的 `activate.py --export`，明确选择它
  生成的 PowerShell 脚本并 dot-source。它不会安装、下载或创建新环境。

### Python 与工具链

- ESP-IDF Python：`C:\Espressif\python_env\idf5.5_py3.11_env\Scripts\python.exe`。
- Python 版本：3.11.2。这是安装器管理的 ESP-IDF virtualenv，包含 `idf.py`
  依赖和 esptool。除非确认该环境已失效，否则不要新建项目 venv。
- `idf.py` 来源：`C:\Espressif\frameworks\esp-idf-v5.5.3\tools\idf.py`。
- 目标：ESP32-C3、RISC-V、8 MB Flash、USB Serial/JTAG 控制台、无 PSRAM。
- 编译器：`riscv32-esp-elf-gcc` 14.2.0，位于
  `C:\Espressif\tools\riscv32-esp-elf\esp-14.2.0_20251107\riscv32-esp-elf\bin`。
- CMake 3.30.2 与 Ninja 1.12.1 均由安装器管理，位于 `C:\Espressif\tools`。
- OpenOCD：0.12.0-esp32-20251215，位于 `C:\Espressif\tools\openocd-esp32`。
- esptool：4.12.dev1，来自 ESP-IDF Python 环境。

### 构建与检查

激活后，在仓库根目录执行：

```powershell
idf.py --version
idf.py reconfigure
idf.py build
idf.py size
idf.py size-components
idf.py menuconfig
idf.py fullclean
```

只有新建或修复 build 配置时才需要 `idf.py set-target esp32c3`。它可能重命名
本地 `sdkconfig`，所以要先检查并保留有意设置。`fullclean` 只清理 build 输出，
不是通用环境修复命令。

仓库可移植门禁在 CI 与 POSIX 开发环境中仍是权威入口。在本机原生 Windows
环境中，使用原生 PowerShell 完成等价的隔离固件构建、合并镜像验证与归档：

```powershell
.\tools\validate-firmware.ps1
```

本机不得通过 Git Bash/MSYS/MinGW 运行 ESP-IDF 固件门禁。ESP-IDF v5.5.3 会
明确拒绝该环境；Git Bash 本身能够启动，并不代表它能生成本次有效的合并镜像。

仓库/文档检查也可直接执行，且已经验证：

```powershell
python tools\check_repo.py
```

本机原生 Windows 的 `--static`/完整门禁不作为已验证命令记录：其中的 host C
tests 使用 GCC 兼容参数，而本机目前只有 Visual Studio `cl.exe`，以及默认 target
为 RISC-V、不是 Windows host 的 ESP-Clang。companion App 的 host tests 使用上方
Android 单元测试门禁；完整仓库 host-test suite 使用 CI 或已经确认的 POSIX host
compiler。不得把 ESP-Clang 指给这些 host tests 的 `CC`。

### 烧录、monitor 与串口识别

开发板使用 ESP32-C3 原生 USB Serial/JTAG 控制台和常规开发烧录。此前实测设备
标识为乐鑫 VID:PID `303A:1001`；COM 号是临时状态，每次连接后重新获取：

```powershell
. .\tools\codex-env.ps1 -Scope EspIdf
python -m serial.tools.list_ports -v
```

打开串口前关闭其他 monitor。已验证自动复位进入 bootloader 可用；只有自动进入
确实失败时才要求 BOOT/RESET。烧录使用 460800 baud；按 `Ctrl+]` 退出 ESP-IDF
monitor。

```powershell
idf.py -p <COMx> flash
idf.py -p <COMx> monitor
idf.py -p <COMx> flash monitor
python -m esptool --chip esp32c3 -p <COMx> -b 460800 write-flash 0x0 build\BugChecking-full.bin
```

最后一条是首选交付镜像流程。普通 `build\BugChecking.bin` 只是 app，
只能写入 `0x10000`，绝不能写入 `0x0`。烧录始终需要用户明确授权，也不隐含
全片擦除。

## Android 与 ESP32 通信

当前 companion link 是 BLE GATT：Passport 是 peripheral，Android App 是 central。
两端之间目前没有 Wi-Fi、Bluetooth Classic、USB 或串口应用数据通道。

- UUID 字符串和固件 byte array 定义：`main/passport_ble_ids.h`。
- Android UUID、frame 编解码、snapshot CRC-32/ISO-HDLC、opcode 与分片重组：
  `PassportProtocol.kt`。
- Android 连接、MTU 247 请求、带回调的串行写入、重试、重连、foreground service、
  定位与结果处理：`PassportBleService.kt`。
- 固件 GATT service、认证后的 indicate/notify 请求传输、write handler 与发送时序：
  `main/passport_ble_link.c`。
- 共享协议权威文档、packet 限制、时间/结果 frame 与 snapshot 语义：
  `docs/protocols/passport-companion-ble.zh_CN.md`。

不要在本文重复 UUID 或 packet 表；协议变化时同步更新协议文档和两端实现。

## 故障处理

先运行 `tools/codex-env.ps1`。某个已记录组件失败时，只根据具体报错调查该组件，
不要重新扫描全部 SDK 与工具。只有确认环境真实变化后，才同步更新本文和脚本。

### Known dead ends

- 未准备的 shell 没有 `JAVA_HOME`；此时运行 Gradle Wrapper 会失败，即使 Android
  Studio JBR 已安装。先激活 Android scope。
- 激活前直接运行 `adb` 会解析到 Touch Portal 内置的
  `C:\Program Files (x86)\Touch Portal\plugins\adb`，而不是项目的 SDK adb。
- ESP-IDF 激活前直接运行 `python` 会解析到 WindowsApps launcher，而不是
  ESP-IDF virtualenv。
- 普通 shell 中的裸 `idf.py` 可能找不到，也可能解析到报告 `v1.0.3` 的无关
  host Python package；它不是 ESP-IDF。CMake、Ninja、GCC 和 OpenOCD 在激活前
  也有意不可用。以上结果都不代表工具链未安装；项目环境脚本会把 `idf.py`
  明确绑定到 ESP-IDF v5.5.3。
- 在 Codex 当前非交互 PowerShell 7.6.5 中直接 dot-source ESP-IDF 的 `export.ps1`
  会同时捕获激活进度和生成脚本路径，然后尝试 dot-source 一个 array 并失败。
  使用 `tools/codex-env.ps1`；它会从官方激活输出中安全选择生成脚本。
- 在子 PowerShell 中运行 `tools/codex-env.ps1` 后返回父 shell，无法保留 PowerShell
  function。必须在即将运行 `idf.py` 的 shell 内 dot-source 它。
- 在 Codex managed filesystem sandbox 中，ESP-IDF 激活即使工具链完好，也可能
  报 Windows error 623（`invalid system DLL relocation`）。只在 sandbox 外用同一条
  原生 PowerShell 命令重试一次；不得重新扫描、重装、另建 virtualenv 或改用 Git
  Bash。一次重试仍失败时，报告具体激活错误并停止。
- `./tools/validate.sh --firmware` 是 POSIX/CI 命令，不是本机 Windows workaround。
  ESP-IDF v5.5.3 会把安装器 Git Bash 判为 MSYS/MinGW 并拒绝；本机使用
  `.\tools\validate-firmware.ps1`。
- 本机执行 `./tools/validate.sh --static` 会停在 `cc: command not found`。本地运行
  `python tools\check_repo.py` 和 Android 门禁；GCC host-C suite 留给 CI 或已确认的
  POSIX host compiler。
- 已记录路径仍有效时，不要使用系统 Gradle、另建 Python venv、重装 ESP-IDF
  或重复下载工具。
- 不要把已安装的 `esp-clang` 设为仓库 host tests 的 `CC`。它的默认 target 是
  `riscv32-esp-unknown-elf`，生成的 ELF 无法在 Windows 上运行。Visual Studio
  `cl.exe` 已安装，但当前 shell 门禁使用 GCC 风格参数，不能直接替换。
- ESP32-C3 使用 RISC-V 工具链。不能因为目录中存在 Xtensa 工具就选择 Xtensa
  target/toolchain 或修改 target。
- 不要硬编码以前的 COM 号、Android 设备 serial、临时 IP、connection handle
  或某次 build 目录状态。
- 不要把 app-only binary 烧到 `0x0`，也不要把全片擦除当作常规恢复步骤。
