**简体中文** · [English](android-ui-adjustment-roadmap-2026-10-28.md)

# BugChecking Android UI 调整排期与验收计划

状态：待实施草案 0.1
基线日期：2026-10-28
权威输入：[设计系统](DESIGN.zh_CN.md) · [UI/UX 审计](ux-audit-android-2026-10-28.zh_CN.md)

## 1. 目标与排期原则

本轮不是重做 UI，而是在保留“动态柔糖／夜雾”、城市地图、旅程带、隐藏长按收藏和个人工具密度的前提下，依次修正渲染正确性、可读性、动作层级和设计系统漂移。

排期按以下顺序决定：

1. 数据或地理表达错误优先于审美精修。
2. 真机已观察的不可读／裁切优先于代码推断问题。
3. 先完成低风险共享修复，再进入地图渲染结构调整。
4. 每阶段只固化已通过真机验收的规则；候选 token 不提前写入长期记忆。
5. 后一阶段不得顺手改变前一阶段已经验收的视觉方向。

工期为有效开发日估算，不是日历承诺。本轮阶段统一命名为 `UI-U0`～`UI-U5`，避免与历史上已完成的 Android Phase 0–3 混淆。执行顺序严格为 `UI-U0 → UI-U1 → UI-U2 → UI-U3 → UI-U4 → UI-U5`；上一阶段必须由用户明确说“UI-Ux 验收通过”才可晋级。实施探索可以提前，但不得合并、推广 token 或把下一阶段描述为完成。

## 2. 阶段总览

| 阶段 | 目标 | 主要范围 | 估算 | 依赖 |
| --- | --- | --- | ---: | --- |
| UI-U0 | 冻结基线与测试矩阵 | 截图、设备状态、候选 tokens、测试数据 | 0.5–1 日 | 无 |
| UI-U1 | 修复直接可见的可读与 inset 缺陷 | 深色内容色、2.0× 回流、底部动作安全区、地图署名 | 1–2 日 | UI-U0 已验收 |
| UI-U2 | 修复地图几何稳定性 | GeoJSON source/layer、聚类、全缩放范围 | 2–4 日 | UI-U1 已验收 |
| UI-U3 | 重排轨迹主页动作层级 | 只保留添加 FAB；地图／地点库持续可见；卡片等高 | 1–2 日 | UI-U2 已验收 |
| UI-U4 | 收敛跨页设计系统 | surface roles、收藏色、同步状态、QR 动作、组件 tokens | 2–3 日 | UI-U3 已验收 |
| UI-U5 | 平台与最终回归 | predictive back、窗口等级、完整真机矩阵、个人规范验收 | 1–3 日 | UI-U4 已验收 |

建议总量：7.5–15 个有效开发日。若本轮只针对当前手机，不实施 Medium／Expanded 布局，UI-U5 可缩短并将其列为明确延期项。

### 范围与延期规则

| 分类 | 项目 | 规则 |
| --- | --- | --- |
| 必须通过 | UI-U1 可读性／inset、UI-U2 地图几何、UI-U3 轨迹动作层级、UI-U4 本次触及页面的 token 一致性、UI-U5 PLZ110 回归 | 任一 FAIL 都不能宣告本轮完成 |
| 可明确延期 | Medium／Expanded、横屏、预测返回（仅在本轮未声明支持时）、未触及页面的 token 清理 | 必须记录原因、风险和后续入口；不能写成 PASS |
| 范围外 | 收藏数据迁移、BLE／同步协议语义、Geocoder 名称清洗、破坏性恢复实测、头像裁切重做 | 需要独立批准，不能顺手实施 |

“有条件通过”默认不允许晋级。只有用户同时明确说明“允许进入 UI-Ux，但以下条件保留”为例外；此时规则保持 `verified`，不得写入记忆或标为 `accepted`。

### 固定真机矩阵

UI-U0 记录具体 OS/API、系统 build、IME、动态色壁纸来源和 APK commit；这些值随后被冻结。最低矩阵如下：

| 维度 | 必测 |
| --- | --- |
| 设备 | PLZ110，1216 × 2640、560 dpi，竖屏，`zh-CN` |
| 主题 | 固定壁纸的动态浅／深；静态回退浅／深 |
| 字体 | 1.0×：全部触及页面；2.0×：全部触及页面；1.3×／1.5×：轨迹、我、轨迹编辑、二维码编辑 |
| 导航 | 手势导航：全部触及页面；三键导航：地图、带底部动作的编辑页 |
| 键盘 | UI-U0 记录的同一 IME；轨迹编辑、二维码编辑、智能导入 |
| 地图 | zoom 4／8／12／16／19；10 次快速捏合；连续平移、旋转、重新定位；加载失败与恢复 |

当前本轮不承诺横屏、平板、折叠屏或 TalkBack 完整验收；已存在的语义不得无故回退。

### token 与契约治理

候选项登记在本计划对应阶段；验收后稳定的 token 移入 `DESIGN.md`。每项记录：

```text
ID / rule-class(E | P | B | C) / kind(token | component-contract | architecture-contract)
scope / proposed value or behavior / status
owner / call sites / evidence / supersedes
```

- `token` 是可复用视觉或尺寸值。
- `component-contract` 是 `maxLines = 1` 等组件行为，不伪装成数值 token。
- `architecture-contract` 是“地理坐标由 renderer source/layer 持有”等结构规则。
- 唯一状态机：实现前 `proposed`；自动门禁和真机通过后 `verified`；用户明确验收后 `accepted`；随后才允许写入记忆。被替代规则标为 `superseded`，但保留替代关系。
- 本轮项目候选默认是 B（项目边界合同）；只有工程底线标 E。不得在本轮 UI 验收中直接把候选提升为跨项目 P；C／P 晋级交给个人规范的 Gate M。

### 本轮候选登记总表

这张表是实施时的唯一入口；阶段正文负责解释，`DESIGN.md` 只接收已验收项。具体数值在对应阶段验收前保持待定，不用临时值冒充标准。

| 类别 | 候选 ID／契约 | 规则类 | 所属阶段 | 当前状态 |
| --- | --- | --- | --- | --- |
| UI-U1 内容与表面 | `content/on-image-surface`、`surface/form-opaque` | B | UI-U1 | `accepted` |
| 后续内容与表面 | `surface/chrome`、`surface/list-card`、`surface/summary`、`surface/selected`、`surface/favourite-tier-*`、`ink/on-favourite-tier-*` | B | UI-U4 | `proposed` |
| UI-U1 布局 | `layout/editor-bottom-reserve`、`layout/safe-drawing-bottom` | B | UI-U1 | `accepted` |
| UI-U3 布局 | `layout/track-action-gap`、`layout/track-action-safe-reserve` | B | UI-U3 | `accepted` |
| UI-U4 前置一级外壳 | `layout/primary-floating-actions`、`navigation/profile-avatar`、`motion/device-capsule` | B | UI-U4 前置增量 | `proposed` |
| UI-U1 地图署名 | `map/attribution-scrim` | B | UI-U1 | `accepted` |
| 地图几何 | `map/route-line-color`、`map/route-line-width`、`map/point-size`、`map/point-outline-width`、`map/favourite-point-colors` | B | UI-U2 | `accepted` |
| 地图聚类 | `map/cluster-radius`、`map/cluster-zoom-thresholds` | B | UI-U2 后续兼容性工作 | `proposed` |
| 动作与动效 token | `motion/track-focus-cue` | B | UI-U2 | `accepted` |
| UI-U3 动作 token | `action/track-primary-fab`、`action/track-signature-icon` | B | UI-U3 | `accepted` |
| 后续动作与动效 token | `action/destructive-icon`、`motion/completion-spring`、`motion/favourite-hold-stage` | B | UI-U4 | `proposed` |
| 组件契约 | `track-card/title-max-lines = 1`、`track-card/title-overflow = ellipsis` | B | UI-U3 | `accepted` |
| 架构契约 | 地理坐标由 renderer source/layer 持有，UI 不自行重算投影 | E | UI-U2 | `accepted` |

## 3. 分阶段实施与验收

### UI-U0：基线冻结与证据准备

实施记录：[2026-10-29 UI-U0 基线与证据](ui-u0-baseline-2026-10-29.zh_CN.md)。

阶段状态：`ACCEPTED`。所有者于 2026-10-29 明确确认“UI-U0 验收通过”；UI-U1 前置门禁已满足。

**实施内容**

- 记录当前 APK 版本、commit／工作区状态、PLZ110 分辨率、dpi、主题、字体倍率和导航模式。
- 准备最短、最长地点名；普通／收藏轨迹；单日与跨日轨迹；二维码长文本；空列表和失败状态测试数据。
- 建立截图矩阵：四个主页面、轨迹新增／编辑、二维码新增／编辑、地点库、场所详情、轨迹地图。
- 为地图准备 zoom 4／8／12／16／19，以及快速捏合、平移、旋转、重新定位场景。
- 把本轮候选 token 登记为 `proposed`，不直接覆盖已验收 token。

**验收标准**

- 基线截图与状态条件一一对应，能在同一设备复现。
- 每项审计问题都有页面、状态、操作和预期结果。
- 地图错位至少有一段可重复操作步骤或录屏；若暂时无法复现，保留“所有者已复现”，不得宣称修复完成。
- `git status --short --branch` 已记录，且不覆盖无关用户修改。

**阶段记忆**

UI-U0 没有产生可晋级的视觉 token；候选 token 保持 `proposed`。长期记忆只登记阶段通过、冻结证据、明确延期边界和“不得把 NOT RUN 写成 PASS”的门禁，不把尚未验证的实现方案提升为规范。

### UI-U1：可读性、回流与系统安全区

实施记录：[2026-10-29 UI-U1 实施与验收检查点](ui-u1-implementation-2026-10-29.zh_CN.md)。

阶段状态：`ACCEPTED`。动态浅／深色、静态回退浅／深色、手势／三键导航、1.0×／2.0×、指定 1.3×／1.5× 页面及剩余 IME 组合均已通过；自动门禁已在最终代码上重跑。所有者于 2026-10-29 明确确认“UI-U1 验收通过”，本阶段五项候选现为 `accepted`，UI-U2 已解锁。

**实施内容**

- 修复透明主 Scaffold 的共享 `onSurface` 内容色，覆盖浅色／夜雾／动态色。
- 修复 2.0× 下标题裁切、数值／单位断裂、编辑器底部动作遮挡。
- 底部固定动作使用透明承载层，不增加外层 tonal Surface；滚动内容仍按实测高度加 navigation／IME inset 预留空间，并使用 edge-to-edge、随主题自适应的系统栏。
- 地图署名与 viewport 共同避让 `safeDrawing`，并增加可读 scrim。
- 编辑器长值保持单行策略：聚焦可滚动，非聚焦有明确尾部省略；不改变轨迹卡高度。

**验收标准**

- PLZ110 上浅色、深色、动态色的所有页面标题和分区标题可读，没有黑字落在夜雾表面。
- 1.0×、2.0× 在全部触及页面无文字重叠、硬裁切、控件遮挡；1.3×、1.5× 在固定矩阵指定的轨迹、我、轨迹编辑、二维码编辑中同样通过；`15 m` 等值与单位不拆开。
- 键盘打开时最后一个字段、helper、错误和按钮均可滚动到可见区域。
- 手势导航与三键导航下地图署名完整、不与系统导航重叠。
- `testDebugUnitTest`、`lintDebug` 零错误、`assembleDebug` 通过。

**已验收 token**

- `content/on-image-surface`
- `surface/form-opaque`
- `layout/editor-bottom-reserve`
- `layout/safe-drawing-bottom`
- `map/attribution-scrim`

以上五项已在自动门禁、固定真机矩阵及用户明确验收后升为 `accepted`。UI-U2 也已于 2026-10-30 验收完成，见下节。

### UI-U2：地图点线几何正确性

实施记录：[2026-10-29 UI-U2 实施检查点](ui-u2-implementation-2026-10-29.zh_CN.md)。

阶段状态：`ACCEPTED`。所有者于 2026-10-30 明确确认“UI-U2 验收完成”。renderer 持有的 GeoJSON 路线／点位图层、普通圆点、收藏五角星、精简地图详情、轨迹卡定位跳转及其 1.8 秒反馈均通过；UI-U3 已解锁。Android 17 PLZ110 的原生聚类 source 兼容性仍单独延期，因此 cluster tokens 不在本次验收范围，保持 `proposed`。

**实施内容**

- 以 MapLibre GeoJSON source + LineLayer／CircleLayer／SymbolLayer 取代缩放后 `clear()` 并重建全部 annotation 的路径。
- 轨迹线按真实 `LatLng` 数据进入 source；点位样式、收藏样式和聚类只改变表现，不改变坐标。
- 定义聚类半径、展开阈值、点位优先级和最多显示数量；避免在相机回调中产生过期更新。
- 保留重新定位与点位选择；点位详情精简为名称、打卡时间和唯一的轨迹卡定位动作。

**验收证据与延期边界**

- 使用 UI-U0 固定轨迹 fixture；相机 idle 后 Marker 的屏幕中心与 `map.projection.toScreenLocation(LatLng)` 的距离 <=2 px，路线抽样顶点同样 <=2 px。
- 快速捏合 10 次、连续平移、旋转、重新定位后无点位闪失、跳到错误位置、线段漂移或旧聚类残留。
- fixture 明确记录 GeoJSON point feature 总数；每个缩放级别中“未聚类 point feature 数量 + 各聚类 `point_count` 总和”恒等于该总数，不以轨迹数、日期组数或屏幕可见数代替。
- 收藏点必须显示既有五角星，普通点保持圆形；两者位置计算完全相同。
- 通过断网并清除本次测试缓存注入加载失败；返回不受影响，恢复网络后 source/layer 正确重建。
- 录屏与至少五个关键缩放截图留作阶段证据。

所有者的全范围缩放观察与 PLZ110 浅／深色真机检查共同覆盖 renderer 点线、收藏五角星、点位选择、地图到轨迹卡跳转和定时定位反馈。原生聚类、聚类数量守恒、自动 2 px 投影测量工具和清缓存离线恢复仍按原状态延期，不因阶段验收被改写成 PASS。

**可晋级 token／契约**

- `map/route-line-color`
- `map/route-line-width`
- `map/point-size`
- `map/point-outline-width`
- `map/favourite-point-colors`
- `motion/track-focus-cue`（2 dp primary 描边；滚动后保持 1.8 秒；450 ms 淡出）
- “地理坐标由 renderer source/layer 持有，UI 不按屏幕位置重算”的渲染契约。

以上项目于 2026-10-30 晋级为 `accepted`。`map/cluster-radius` 与 `map/cluster-zoom-thresholds` 因 PLZ110 原生聚类兼容性问题继续保持 `proposed`，不得误写为通过。

### UI-U3：轨迹主页特色动作层级

实施记录：[2026-10-30 UI-U3 实施与验收检查点](ui-u3-implementation-2026-10-30.zh_CN.md)。

阶段状态：`ACCEPTED`。所有者于 2026-10-30 明确确认“UI-U3 验收通过”。“添加轨迹”是唯一 FAB；地图与地点库保留在占用真实布局空间的紧凑次级动作条中，持续可见且不覆盖卡片。UI-U4 已解锁。

**实施内容**

- “添加轨迹”使用标准 Material 3 主 FAB。地图与地点库共用 `secondaryContainer`／`onSecondaryContainer` 次级动作条，16 dp shape、1 dp tonal elevation，每个入口各有 48 × 48 dp 点击目标。
- 特色动作条使用 8 dp 顶部间距。滚动列表底部预留 104 dp 给唯一 FAB；短名、长名、中英混合名始终单行尾部省略并保持相同高度。
- 不新增常驻帮助文案、教程或重复文字按钮；隐藏长按收藏与整卡点击编辑保持既有逻辑。

**验收标准**

- “添加”、地图、地点库三个入口始终在轨迹主页首屏可见，无需 overflow；三个目标均至少 48 × 48 dp。
- “添加”使用唯一 FAB 角色；地图／地点库不得使用与其相同的尺寸、容器色和 elevation 组合。
- 两个特色入口不遮挡轨迹卡，不因 2.0× 字体与底部导航发生碰撞。
- 短名、长名、中英混合名下轨迹卡测得高度差为 0 dp；尾部省略稳定。
- 长按收藏不被新增图标或手势抢占；点击仍只进入编辑。
- 不新增教程、提示段落或重复文字按钮。

**可晋级 token／契约**

- `action/track-primary-fab`
- `action/track-signature-icon`
- `layout/track-action-gap`
- `layout/track-action-safe-reserve`
- `track-card/title-max-lines = 1`（component contract）
- `track-card/title-overflow = ellipsis`（component contract）

以上项目于 2026-10-30 晋级为 `accepted`。PLZ110 上地图、地点库与整卡编辑导航均通过；2.0× 字体下三张可见 fixture 卡片高度均为 385 px，测试后已恢复 1.0×。完整 Android 单元测试、lint 与 assemble 门禁通过。

### UI-U4：跨页一致性与 token 收敛

UI-U4 之前，所有者批准了独立的一级外壳增量。当前实现移除实体顶栏与一级页面标题，把“设备与我”从底栏／rail 迁移到 48 dp 圆形头像，在轨迹页顶部中央保留地图／地点库，并将设备入口放入中性、右锚定、可展开的胶囊。操作行位于 safe top inset 下方 8 dp；轨迹、清单、二维码首屏统一增加 64 dp 透明呼吸区，滚动后仍可经过无底色悬浮层后方。“设备与我”作为独立二级页面打开，不显示底栏／rail，并在左上角提供返回按钮。窄屏优先省略状态文字，不移动或隐藏中央特色入口。本增量在 PLZ110 浅／深色、1.0×／2.0×、导航模式及全部设备状态通过前保持 `proposed`，且不代表 UI-U4 完成。

**实施内容**

- 建立 chrome、普通列表卡、摘要／选中卡、收藏卡、表单容器的 surface 角色。
- 验证收藏容器与文字的最终合成可读性，不以随意 alpha 替代语义 ink。
- 同步失败保持可诊断，不要求增加常驻说明文案。
- QR 卡保留图标优先，降低展示、收藏、编辑、删除之间的视觉竞争。
- 只提取重复且稳定的 tokens，删除重复 magic values；不 token 化所有数字。

**验收标准**

- 同一角色跨页面使用同一 token；没有页面私自复制近似颜色、圆角或透明度。
- 主动作、特色次要动作、普通次要动作、破坏性动作在各页层级一致。
- 在固定动态／静态浅深主题截图中，对地图背景最亮、最暗样本和每种 surface role 的最终合成色测量：普通文字 >=4.5:1，大字和 UI 边界 >=3:1；这不改变隐藏收藏的发现性取舍。
- QR 删除与日常动作有清楚的空间或色调隔离，仍不要求文字标签。
- `PassportSpacing`、shape、type roles 与组件 token 清单有引用位置和所有者。

**可晋级 token**

- `surface/chrome`
- `surface/list-card`
- `surface/summary`
- `surface/selected`
- `surface/favourite-tier-*`
- `ink/on-favourite-tier-*`
- `action/destructive-icon`
- `motion/completion-spring`
- `motion/favourite-hold-stage`

### UI-U5：平台回归、最终实机与个人规范验收

**实施内容**

- 验证系统返回、顶栏返回与状态恢复；只有本轮声明支持预测返回时才将其列为必测，否则按延期项记录。
- 当前手机为必做；Medium／Expanded 只有在本轮决定支持时实施，否则记录延期而不伪装完成。
- 执行完整浅／深、1.0×／2.0×、键盘、系统 inset、地图动态操作和跨页一致性回归。
- 使用本轮实际结果校正 [个人应用开发规范](personal-app-development-standard.zh_CN.md)。按该规范的依赖顺序完成 Gate A–G、I、K、L 后才能执行 H；触及权限／数据／迁移时，J 预审必须在 E 前且真机后复核，未触及时记录不适用理由。Gate M 不属于本轮完成条件。只验收与本轮 UI 直接相关的颜色／表面、布局／溢出、控件层级、动效和外部视觉资源；数据同步、固件及天色 beta 候选不属于本轮完成条件。

**验收标准**

- Android 自动门禁全部通过，APK 由当前工作区构建并安装。
- PLZ110 上按 UI-U1～UI-U4 的矩阵逐项 PASS；无“代码看起来正确”代替真机证据。
- 没有破坏轨迹／清单／二维码／设备与我的可达性（三个导航项＋头像入口）、旅程带、当前配色、隐藏收藏、地图／地点库持续可见和轨迹卡等高契约。
- 个人应用开发规范中本轮适用的 E／P／B 规则都能追溯到本轮证据或明确个人声明；未验证偏好只能标为 C／“候选”。
- 用户明确说出“UI-U5 验收通过”后，本轮才算完成。

## 4. 统一验收记录格式

每阶段使用同一张记录，避免只凭印象通过：

```text
阶段（UI-Ux）：
Build：PASS / FAIL / NOT RUN
Unit tests：PASS / FAIL / NOT RUN
Lint：PASS / FAIL / NOT RUN
Device：PASS / FAIL / NOT RUN
设备／主题／字体／导航模式：
通过的页面与状态：
失败项与复现步骤：
延期项及原因：
用户结论：验收通过 / 有条件通过（默认不晋级） / 不通过
可晋级规范／token：
被替代的旧规则：
```

## 5. 记忆登记协议

阶段规则只有在用户明确验收后才进入记忆：

1. 实施中：写入项目文档，状态为 `proposed`。
2. 真机与自动门禁通过：状态改为 `verified`，但仍不自动泛化为个人偏好。
3. 用户明确说“UI-Ux 验收通过”：在 `C:\Users\MPHNUC\.codex\memories\extensions\ad_hoc\notes\` 新增一条小型记录，包含 `scope`、`applies_to`、证据、已接受 token／契约、被替代规则和禁止泛化的边界。
4. BugChecking 专属决定先进入项目记忆；只有在 BugChecking 与天色 beta 两个项目都得到验证，或用户明确要求提升，才进入跨项目个人规范。
5. 跨项目规则稳定后，再用它们创建个人开发 skill；skill 必须包含触发条件、反例、决策树、验收模板和不得自动套用的产品例外。

本计划本身不写入记忆，因为尚未实施和验收。
