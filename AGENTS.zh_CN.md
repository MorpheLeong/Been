<p align="right">
  <strong>简体中文</strong> · <a href="AGENTS.md">English</a>
</p>

# AI Agent 仓库规范

本文是本仓库 AI 辅助工作的唯一必读入口。根据下方路由表读取当前任务所需文档，不要默认加载全部 README。

## 必需 AI 技能

以下五个技能是本仓库 AI 辅助开发的必需项：`passport-develop`、
`passport-setup`、`passport-build`、`passport-device-test`、`passport-debug`。
技能权威源文件位于 `skills/`。

开始开发前，AI 必须自行检查这五个技能是否已在当前环境安装并可用。缺少时，
由 AI 根据所用工具和环境，自行选择合适的安装方式、完成安装并验证可用性。
不等待用户另外提出安装要求，也不把选择安装方式或执行安装命令交给用户。
不强制使用特定脚本、安装目录或 AI 客户端。

安装必须遵守环境的权限审批要求，保留已有技能和用户配置。权限、冲突或平台
限制导致无法安装时，说明阻碍，只请求必要的用户配合，不得谎报安装成功。
五个技能必须可用，不代表每次任务都加载或调用全部技能；只使用匹配当前任务
的技能。烧录、Git 写操作和发布仍须分别取得相应授权。

## 项目与安全基线

- 目标平台：ESP32-C3、8 MB Flash、无 PSRAM、ESP-IDF 5.5.3。
- 仓库默认分区表保持最简：只包含 NVS、PHY data，以及占用 8 MB Flash
  剩余空间的单个 factory app。用户固件可以按需求明确调整布局；修改后必须
  验证结果，不得把产品专用分区变成模板的强制契约。
- 保留用户已有修改。先执行 `git status --short --branch`，不得覆盖或清理无关文件。
- 下载（烧录）新固件无需备份设备内原有固件，不得把读取 Flash 备份作为前置条件。这不保证保留用户数据，也不授权全片擦除；遵循[烧录与数据说明](docs/development/engineering/firmware-layout.zh_CN.md#烧录与已存数据)。
- 硬件事实优先级：产品规格与实测结果 → `components/bsp/include/bsp_pins.h` → BSP 头文件与实现 → 硬件指南 → README/demo。任务所需硬件细节未在这些来源中定义时，直接询问用户，不得猜测。
- 可复用板级逻辑放入 `components/bsp`；页面、状态机、动画和应用任务放入 `main`。
- 二次开发应用必须重新设计并实现独立 UI，禁止沿用当前 demo 测试菜单、页面或界面外壳；仅改名、换颜色或在原界面增加功能不算重新设计。BSP API 和非 UI 逻辑仍可复用。详见[二次开发 UI 强制重新设计规则](docs/development/ai-guide.zh_CN.md#二次开发-ui-强制重新设计)。
- LVGL 非线程安全。LVGL 任务之外访问 LVGL 对象时必须持有 `bsp_lvgl_lock()`。
- 添加中文 UI 前必须遵循[字体检查清单](docs/development/engineering/coding-conventions.zh_CN.md#中文字体与缺字排查)。默认 Montserrat 字体不含中文字形；UTF-8 正确、编译成功均不代表能够显示中文。必须核对字形覆盖、控件实际字体并完成真机显示验收。
- 按键回调不得阻塞。音频、存储、网络等慢操作必须放入工作任务。
- demo 删除 screen 前，必须停止所有可能访问其 UI 的任务、定时器、回调和事件处理器。
- 可测试的状态机、协议、计时和布局计算应与 ESP-IDF/LVGL 解耦，并由 host tests 覆盖。
- 禁止提交凭证、设备二维码秘密、私钥、个人数据或未脱敏日志。
- 所有维护中的 Markdown 默认 `.md` 路径必须为英文，简体中文使用配对的 `.zh_CN.md` 文件。两种语言必须保持一致并保留互相切换链接。

## 按任务加载上下文

| 任务 | 修改前读取 |
| --- | --- |
| 任意代码修改 | `docs/development/ai-guide.zh_CN.md`、相关头文件和相邻实现 |
| 应用工作流或核心技能配置 | `skills/README.zh_CN.md`；确认五个必需技能可用，再使用匹配当前任务的技能 |
| 环境引导或缺少工具链 | `docs/development/engineering/environment-setup.zh_CN.md` |
| BSP、引脚、总线、显示、音频、电池 | `docs/hardware-design/AI_HARDWARE_DEVELOPMENT_GUIDE.zh_CN.md`、`components/bsp/include/bsp_pins.h` |
| Demo 或菜单 | `main/demo.h`、`main/main.c`、最近的 `main/demo_*.c` 实现 |
| 中文 UI 或字体 | `docs/development/engineering/lvgl-chinese-fonts.zh_CN.md`、应用字体素材、配置与控件样式 |
| Wi-Fi 联网或蓝牙配网 | `docs/development/engineering/wifi-provisioning.zh_CN.md`、其中引用的 `demo/blufi-provisioning` 实现 |
| 构建、测试、依赖、分区 | `docs/development/engineering/build-and-test.zh_CN.md`、`docs/development/engineering/firmware-layout.zh_CN.md`、`sdkconfig.defaults`、`partitions.csv` |
| CI 或发布 | `docs/development/ci/CI-*.zh_CN.md` 中的对应文件与 `.github/workflows/` |
| 项目开发完成 | `docs/development/release/project-completion.zh_CN.md`（再进入 `issue-suggestions` 或 `experience-pr` skill） |
| 文档 | `docs/contribution/doc-conventions.zh_CN.md`、`docs/README.zh_CN.md` |
| Commit 或 PR | `docs/contribution/commit-and-pr.zh_CN.md` |

产品概览与文档索引见 `docs/README.zh_CN.md`。详细的 AI 开发工作流（上下文建立、事实来源优先级、应用/BSP 边界、运行时规则、素材放置、交付格式）见 `docs/development/ai-guide.zh_CN.md`。Fork 专用流程见 `docs/fork-guide.zh_CN.md`，普通上游开发无需读取。

## 必须执行的验证与交付格式

迭代时运行最小相关检查，交付前运行完整门禁：

```bash
./tools/validate.sh --static    # 仓库检查 + host tests
./tools/validate.sh --firmware  # ESP-IDF 构建 + 合并镜像验证
./tools/validate.sh             # 完整门禁
```

完整门禁要求已激活 ESP-IDF 5.5.3 环境。不得把编译成功描述成硬件验证成功。最终交付必须分别报告：

```text
Build: PASS / FAIL / NOT RUN
Host tests: PASS / FAIL / NOT RUN
Device tests: PASS / FAIL / NOT RUN
Unverified: 仍需板卡、仪器或用户确认的事项
```

每次完整实现用户提出的固件需求后，必须主动询问是否将固件刷写到设备中进行
测试，不能只等到发布时才询问。未检测到设备时，提示用户将设备开机，再用
支持数据传输的数据线连接电脑 USB 接口。遵循
[真机测试交接流程](docs/development/ai-guide.zh_CN.md#主动询问真机测试)，
烧录前须取得用户同意；检测到设备本身不代表获得烧录授权。

仅在用户请求或当前工作流明确要求时创建 commit 和 push。普通功能、应用和文档 PR 不得修改 `docs/CHANGELOG.md` 或 `docs/CHANGELOG.zh_CN.md`；用户可见行为、兼容性和发布流程影响改为写入 PR 正文及对应权威文档。发布准备期间，由发布负责人在创建 tag 前把已合并的用户可见变化统一汇总到两份变更日志。

社区规范见 `.github/CONTRIBUTING.zh_CN.md`、`.github/CODE_OF_CONDUCT.zh_CN.md`、`.github/SECURITY.zh_CN.md` 与 `.github/SUPPORT.zh_CN.md`。

## Development Environment / Codex Workflow

本 checkout 已有经过验证的本机环境记录。新任务必须复用它，不得重复探索 SDK
与工具链。只有需要环境事实或命令细节时才读取：
[`docs/CODEX_ENV.zh_CN.md`](docs/CODEX_ENV.zh_CN.md)。

### 项目布局

- 仓库与 ESP-IDF 根目录：`C:\Users\MPHNUC\Documents\ChatGPT\AI Passport\ai-passport-fork-checkout\ai-passport-main`
- Android 根目录/module：`android-app` / `app`
- 共享协议：`docs/protocols/passport-companion-ble.zh_CN.md`
- 固件实现：`main`；Android 实现：`android-app/app/src/main`

### Android 环境

- Android SDK：`C:\Users\MPHNUC\AppData\Local\Android\Sdk`
- JDK：Android Studio JBR，路径为 `C:\Program Files\Android\Android Studio\jbr`
- 入口：`. .\tools\codex-env.ps1 -Scope Android`，随后使用
  `android-app\gradlew.bat`。使用脚本选中的 SDK adb，不要使用未准备 host
  `PATH` 中的裸 `adb`。
- 已验证门禁：在 `android-app` 中执行
  `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`。

### ESP-IDF 环境

- `IDF_PATH`：`C:\Espressif\frameworks\esp-idf-v5.5.3`
- 版本/目标：ESP-IDF v5.5.3、ESP32-C3、RISC-V。
- Python：`C:\Espressif\python_env\idf5.5_py3.11_env\Scripts\python.exe`
- 每个新 PowerShell process 的入口：
  `. .\tools\codex-env.ps1 -Scope EspIdf`。
- 标准构建：`idf.py build`。用 `python -m serial.tools.list_ports -v` 获取
  当前串口，绝不能复用以前记录的 COM 号。
- 烧录/monitor 仍须按常规取得明确授权。命令和镜像 offset 见
  `docs/CODEX_ENV.zh_CN.md`。

### 标准命令

```powershell
# 在当前 PowerShell process 中准备两套工具链
. .\tools\codex-env.ps1

# Android
Push-Location android-app
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
Pop-Location

# ESP-IDF
idf.py --version
idf.py build
```

### Agent 启动策略

- 每个新任务先读本 `AGENTS.md`。本文件记录的路径、环境与命令是当前项目事实。
- 不得只为“再次确认环境”而执行大范围 `ls`、`find`、`where`、
  `Get-ChildItem`，或重新扫描 SDK、JDK、Gradle、adb、ESP-IDF、Python、
  CMake、Ninja、工具链与项目目录。
- 新 Codex conversation 不构成重新探索环境的理由。已记录命令可用时，应直接
  进入相关代码和实现。
- 某条命令失败时，只根据具体报错调查对应组件；不要重新探索无关工具链。
  Android 失败不能成为重查 ESP-IDF 的理由，反之亦然。
- 不要把临时 shell 状态当成永久事实。每个新 PowerShell process 需要环境时，
  dot-source `tools/codex-env.ps1`。
- 确认环境真实变化后，同步更新本节、`docs/CODEX_ENV.md` 及中文版和环境脚本。

### Known dead ends

- 未准备的 shell 没有有效 `JAVA_HOME`，直接运行 Gradle Wrapper 会失败。
- 裸 `adb` 会解析到 Touch Portal 内置 adb，不是 Android SDK adb。
- 裸 `python` 会解析到 WindowsApps，不是 ESP-IDF virtualenv。
- 项目激活前的裸 `idf.py` 可能找不到，也可能解析到报告 `v1.0.3` 的无关 host
  package；它不是 ESP-IDF。两种结果都不能据此认为 ESP-IDF 未安装。
- 当前非交互 PowerShell 中直接用 `export.ps1` 激活，会把多条输出一起
  dot-source 并失败；使用 `tools/codex-env.ps1`。
- 只在子 PowerShell 中激活不会准备父 shell；必须在即将构建的 shell 中
  dot-source 项目入口。
- 不得另建 ESP-IDF Python venv、使用系统 Gradle、为 ESP32-C3 选择 Xtensa、
  硬编码 COM 号/设备 serial、把 app-only 镜像烧到 `0x0`，或把全片擦除当作
  常规修复方法。
