**简体中文** · [English](DESIGN.md)

---
name: BugChecking Android
description: BugChecking Android 配套 App 的强制 UI 与交互规范。
colors:
  action-neon: "#53FF9A"
  on-action-neon: "#00351D"
  device-disconnected: "#007A49"
  journey-light: "#C3004F"
  journey-dark: "#FF5A86"
  light-canvas: "#FBE8DB"
  light-card: "#F5D9D5"
  dark-canvas: "#121212"
  dark-on-surface: "#E8BDC8"
typography:
  family: Android system sans
  headline: Material 3 headlineSmall semibold
  title: Material 3 titleLarge/titleMedium medium-to-semibold
  body: Material 3 bodyLarge/bodyMedium/bodySmall
  label: Material 3 labelLarge/labelMedium/labelSmall
rounded:
  small: 8dp
  medium: 12dp
  large: 16dp
  extra-large: 28dp
spacing:
  xxs: 4dp
  xs: 8dp
  sm: 12dp
  md: 16dp
  lg: 24dp
  xl: 32dp
components:
  page-horizontal-inset-compact: 16dp
  page-horizontal-inset-medium: 24dp
  minimum-touch-target: 48dp
  form-max-width: 720dp
  list-max-width: 840dp
---

# BugChecking Android 设计系统

本文档是新增 Android UI 和修改现有页面时必须遵循的规范，记录已验收的产品方向以及后续必须保留的边界。本次审计发现的现状缺口见 [BugChecking Android UI/UX 设计审计 — 2026-10-28](ux-audit-android-2026-10-28.zh_CN.md)；规则写入本文不代表当前每个页面已经满足。

## Overview

**创意北极星：“为日常使用柔化的旅程地图册。”**

BugChecking 是用于旅行准备、打卡内容管理、真实到访记录，以及向 BugChecking 设备同步身份和部分内容的个人 Android 配套 App。当前受众是熟悉其隐藏手势与数据结构的产品所有者／开发者，不以首次使用转化或零学习成本为目标。它属于带有 Explore／Play 特征的 Operate 型界面：功能准确、视觉辨识和操作乐趣优先，说明文案与显式标签保持克制。

视觉世界由居中裁切的网点城市地图、淡杏橘浅色画布、玫瑰桃色表面和炭黑／灰粉深色表面组成。荧光绿与糖果红是稀缺的功能强调，不是组件默认色。地图是一层安静的地理记忆，不是正文；卡片与控件作为可读、面向任务的层级覆盖在其上。品牌感来自旅程带、地图纹理、暖色糖果表面和精准动效，而不是装饰渐变、光晕或新奇控件。

**关键特征：**

- 可辨识城市地图底图和按角色区分的半透明表面。
- 浅色使用减眩的淡杏橘画布与暖色卡片；深色使用炭黑画布与灰粉文字。
- 克制的 Material 3 组件词汇与 Material 图标家族。
- 每个页面一个主操作；轨迹页的地图与地点库是必须持续可见的特色入口，但视觉权重低于添加。
- 动效只服务状态，不做装饰编舞。
- 深色、长内容、地图缩放和系统 inset 下不得发生不可读、错位或数据表达错误。

**保留地图册规则。** 精修可以调整透明度、层级与对比，但没有明确产品批准，不得移除地图识别、暖色糖果体系、炭黑／灰粉深色体系或旅程带。

**任务优先规则。** 当视觉表达与阅读、目标点击、错误恢复或 Android 规范冲突时，任务优先。

**个人工具规则。** 不为假设中的新用户增加常驻教程、提示段落或文字按钮。只有错误恢复、不可逆／高成本操作、权限原因或无法从状态本身理解的歧义需要显式说明。

## Colors

### 架构

颜色采用三层结构：

1. **基础色板：** frontmatter 中的命名静态颜色，以及 `PassportDesign.kt` 内完整浅色／深色色调。
2. **语义角色：** `MaterialTheme.colorScheme` 的 `primary`、`secondary`、`tertiary`、对应 container／`on*`、surface-container 阶梯、`outline`、`outlineVariant` 与 `error`。
3. **组件角色：** 主页面 chrome、导航、卡片、摘要、选中、收藏等级和地图标记。除非固定产品色确有必要并被记录，组件角色必须通过语义角色解析。

### 已验收的 UI-U1 契约

| ID | 已验收定义 |
| --- | --- |
| `content/on-image-surface` | 透明根 Scaffold 显式使用 `MaterialTheme.colorScheme.onSurface` 作为内容色。 |
| `surface/form-opaque` | 高密度编辑／表单表面通过不透明 `MaterialTheme.colorScheme.surface` 或稳定 surface role 解析，不继承主页面卡片／chrome 的半透明处理。 |
| `layout/editor-bottom-reserve` | 滚动内容按固定动作实测高度加 navigation／IME inset 预留空间；动作承载层透明，不新增外层 tonal Surface。 |
| `layout/safe-drawing-bottom` | 地图署名与贴边控件消费水平和底部 `safeDrawing`／系统导航 inset。 |
| `map/attribution-scrim` | 署名使用 `surface.copy(alpha = 0.88f)`、匹配的 `onSurface`、8 dp 圆角、1 dp tonal elevation、内边距，并感知 safe drawing。 |

这些 BugChecking 项目契约已于 2026-10-29 通过 UI-U1 的 PLZ110 真机矩阵验收；它们不晋级仍由 UI-U2 负责的地图几何候选。

### 已验收的 UI-U2 契约

| ID | 已验收定义 |
| --- | --- |
| `map/route-line-color` | 轨迹线解析为 `MaterialTheme.colorScheme.primary`。 |
| `map/route-line-width` | renderer `LineLayer` 宽度为 4f style units，端点与连接为 round。 |
| `map/point-size` | 普通点使用 9f style-unit 半径的 renderer circle。 |
| `map/point-outline-width` | 普通点使用 2.5f style-unit 语义 surface 描边；36 dp 收藏星位图沿用等效 2.5 dp 描边。 |
| `map/favourite-point-colors` | 收藏等级 1～3 使用既有 `trackFavoriteMapColor` 语义色，并在地图上统一表现为 36 dp 五角星。 |
| `motion/track-focus-cue` | 滚动到目标卡后显示 2 dp primary 描边 1.8 秒，再以 450 ms 淡出。 |
| renderer 坐标契约 | 点位与轨迹线由 MapLibre GeoJSON source／layer 持有；UI 不自行重算投影或在相机 idle 后重建 annotation。 |

以上 BugChecking 项目契约于 2026-10-30 通过 UI-U2 的 PLZ110 真机与所有者验收。`map/cluster-radius` 和 `map/cluster-zoom-thresholds` 因原生聚类兼容性延期，仍为 `proposed`。

### 已验收的 UI-U3 契约

| ID | 已验收定义 |
| --- | --- |
| `action/track-primary-fab` | “添加轨迹”是轨迹主页唯一 FAB，使用标准 Material 3 主 FAB 角色；地图与地点库不得复用它的完整尺寸、色调和 elevation 组合。 |
| `action/track-signature-icon` | 地图与地点库共用一个持续可见的次级动作条，使用 `secondaryContainer`／`onSecondaryContainer`、16 dp large shape、1 dp tonal elevation，每个动作各有 48 × 48 dp IconButton 目标。 |
| `layout/track-action-gap` | 特色动作条与轨迹内容顶部保持 8 dp 间隔；它占用真实布局空间，不悬浮覆盖卡片。 |
| `layout/track-action-safe-reserve` | 轨迹列表底部预留 104 dp，使最后一张卡可避开唯一主 FAB；特色动作条位于滚动 viewport 外，不需要额外覆盖预留。 |
| `track-card/title-max-lines` | 轨迹卡地点名固定为一行。 |
| `track-card/title-overflow` | 溢出使用尾部省略号；禁止自动换行和因名称长度改变高度。 |

以上 BugChecking 项目契约于 2026-10-30 通过 PLZ110 1.0×／2.0× 字体矩阵及所有者验收。UI-U4 已解锁；这些规则仍为项目级契约，不自动晋级为跨项目个人偏好。

已验收的产品意图——“添加”是唯一 FAB、地图／地点库持续可见、轨迹卡等高单行——继续有效。旧特色动作条占据内容布局的几何由下述待验收无底色外壳合同接替；新几何必须通过独立真机验收后才可标记为 `accepted`。

### UI-U3.5 当前实施规则（待真机验收）

UI-U3.5 改用固定品牌配色，不再从 Android 壁纸动态取色。下列规则是当前实施合同；只有通过实机矩阵与所有者验收后才能标记 `accepted` 或写入长期记忆：

| ID | 当前定义 |
| --- | --- |
| `color/action-neon` | 荧光绿 `#53FF9A` 是稀缺动作／状态强调；不得因为组件是 Button、TonalButton、Slider 或图标而自动获得该颜色。 |
| `color/device-disconnected` | 悬浮设备入口断联时使用深绿 `#007A49`；连接中、已连接、同步中与可同步图标使用荧光绿，其容器始终保持中性。设备入口的强调级高于 FAB。 |
| `color/journey` | 旅程线与清单进度使用糖果红：浅色 `#C3004F`、深色 `#FF5A86`。UI-U2 的地图几何合同不变，UI-U3.5 只把轨迹线从旧 primary 映射改为 journey 角色。 |
| `surface/light-canvas` | 浅色画布为淡杏橘 `#FBE8DB`；普通卡片、chrome 与表单使用相邻的玫瑰桃色 surface 阶梯，禁止只改背景而让其余表面成为苍白孤岛。 |
| `surface/dark-canvas` | 深色画布为 `#121212`，主文字／边界使用灰粉语义阶梯；深色不是浅色机械反相。 |
| `action/editor-helper-neutral` | 添加／编辑轨迹页的“展开／查找”、地图与名称识别图标、重新定位及同组加载反馈使用 `onSurfaceVariant` 或中性 surface，不使用荧光绿。 |
| `card/checklist-progress` | 完成度卡片使用普通卡片底色；进度值使用 journey 色，轨道使用中性 surface。 |
| `card/qr-star` | 二维码卡片全部使用普通卡片底色；只有已星标图标保留荧光绿，未星标、编辑和删除保持中性。 |
| `action/profile-backup-neutral` | “更换头像”“保存资料”、音量条、“更改”“立即备份”保持中性，不使用荧光绿。 |
| `layout/primary-floating-actions` | 一级页面不使用实体顶栏或页面标题。无底色悬浮层位于 safe top inset 下方 8 dp，并与页面边界对齐 48 dp 的头像、特色入口和设备入口；未被按钮覆盖的区域不得拦截内容。轨迹、清单、二维码的首屏内容统一保留 64 dp 透明呼吸区，滚动后仍可经过其后方。 |
| `navigation/profile-avatar` | 底栏／rail 只保留轨迹、清单、二维码；圆形头像将“设备与我”作为独立二级页面打开。“设备与我”隐藏底栏／rail，并以左上返回按钮取代头像。 |
| `motion/device-capsule` | 中性设备按钮高度 48 dp、右边界固定；状态文字向左展开，320 ms 进入、260 ms 退出，在中央特色入口前单行省略，并通过 polite live region 暴露完整状态。 |

**操作重要性优先。** 先按真实任务定义核心状态／动作、特色状态与普通辅助动作，再分配颜色。Material 组件种类只决定结构和交互行为，不自动决定产品强调级。

**同色单角色。** 同一界面不得并列多种相近绿色表达同一层级；若状态需要区分，必须有明确语义命名和非颜色线索。当前绿色仅保留荧光绿动作／状态强调与断联深绿两个角色。

### 浅色模式

- 淡杏橘 `#FBE8DB` 是画布；卡片、顶／底 chrome 与表单从玫瑰桃色 surface 阶梯解析。
- 荧光绿只用于 UI-U3.5 表中明确保留的高价值动作或状态，不作普通控件默认色。
- 糖果红用于旅程线、轨迹点与清单进度，不作通用错误色。
- 任何背景调整必须同时检查地图遮罩、旅程线、卡片与 chrome 的合成对比，禁止只替换画布。

### 深色模式

- 深色使用 `#121212` 炭黑画布与相邻中性 surface 阶梯；文字和边界使用灰粉语义色。
- 荧光绿和糖果红保持与浅色相同的功能归属，不按视觉明暗重新分配语义。
- 透明 Scaffold 上的页面级内容必须显式继承 `onSurface`，不得依赖环境默认值。
- 每个容器继续使用匹配的 `on*` 或 surface 前景色，禁止机械反相浅色主题。

### 地图与透明度

- 城市地图居中裁切，可按主题降低饱和度和亮度。
- 透明度按角色定义，不由页面随意决定：顶／底 chrome、普通列表卡、摘要／选中卡、表单容器是不同角色。
- 地图署名、缩放／定位控件和 viewport 必须消费 `safeDrawing`／navigation bar inset；署名不得贴住手势条、被地图标签覆盖或在屏幕边缘裁切。
- 轨迹点与轨迹线使用地图渲染器的地理坐标 source／layer 绘制，随缩放、旋转和平移保持锚定；不得依赖相机空闲后清空并重建屏幕注记来维持位置。
- 普通轨迹点使用圆点；收藏地点在地图上始终使用五角星，并以既有收藏等级语义色区分。五角星必须作为 renderer symbol layer 的图标绑定同一地理 point feature，不得为了图形差异回退到屏幕坐标 annotation。
- 点按普通点或收藏五角星后，地图详情 sheet 只显示地点名与打卡日期时间，并提供唯一主动作“在轨迹记录中定位”。该动作返回轨迹主页并滚动到对应轨迹卡片；sheet 不重复显示精度，也不提供编辑或外部地图按钮。
- 定位滚动完成后，目标轨迹卡显示 2 dp 主题主色描边 1.8 秒，再以 450 ms 淡出；描边不得改变卡片尺寸、底色、收藏等级或点击行为，也不附加 Toast／说明文字。
- 高密度表单使用比稀疏列表更不透明的容器或局部 scrim。
- 最终合成后的文字／背景必须满足：普通文字 4.5:1，大字和 UI 边界 3:1；只检查未合成 hex 不足以验收。
- 除非最终合成仍达到 4.5:1，否则不得降低普通字号副文本透明度。

**成对颜色规则。** 容器必须使用匹配的 `on*` 色，禁止随意跨角色配色，也禁止用 alpha 变淡充当替代 ink。

**地图是记忆规则。** 用户应先看到内容，再注意地图。字段、标签或状态与地图竞争时，增强局部表面，不要全局移除地图。

## Typography

使用 Android 系统无衬线字体与 Material 3 类型阶梯。产品 UI 禁止引入 display font。系统字体是有意选择，可支持中文、拉丁字符、数字、无障碍缩放与平台熟悉度。

- `headlineSmall`：顶栏不是唯一标题时的主要页内标题。
- `titleLarge`：独立页面和 sheet 标题。
- `titleMedium`：分区标题、卡片标题与日期分组。
- `titleSmall`：设置／表单内的紧凑子标题。
- `bodyLarge`：可编辑值与主要行内容。
- `bodyMedium`：普通说明内容。
- `bodySmall`：副事实与帮助文字，仅在保持 AA 时使用。
- `labelLarge`：按钮与突出紧凑数值。
- `labelMedium`／`labelSmall`：状态、紧凑 metadata 与次要说明。

加强层级优先使用字重，再考虑颜色／透明度。能由 `PassportTypography` 表达时，不在调用点重复 `FontWeight`。

2.0× 字体下，文字必须回流而不裁切、重叠或隐藏控件。数值／单位作为原子整体；副信息一行放不下时，重排为有意的两行，不留下孤立单位。标题与标签默认可换行，除非存在经过验证、对用户安全的省略规则。

轨迹卡地点名是明确例外：固定单行并使用尾部省略号，禁止自动换行改变卡片高度。若提供滚动播放，只能在聚焦、按住或明确选中时启动，不做持续自动 marquee，并尊重系统减少动态效果设置。

**先角色后字号规则。** 先选择语义类型角色，页面代码不得使用一次性字号。

**两行优于破坏规则。** 大字体下，有意的第二行优于被裁切的标签、重叠动作或不可读省略。

## Layout

### 窗口等级

- **Compact（<600 dp）：** 底部 NavigationBar、单栏、16 dp 页面水平 inset。
- **Medium（600–839 dp）：** NavigationRail、受限单栏、24 dp inset；表单不超过 720 dp，列表不超过 840 dp。
- **Expanded（>=840 dp）：** NavigationRail + list-detail 或 supporting pane；若第二栏能改善任务，不得只把手机宽页面居中。
- 折叠屏禁止把主交互跨铰链放置；未做铰链验收前，不得宣称 Expanded 支持完成。

四个一级路由共享同一外壳几何。首屏内容从 48 dp 悬浮操作行下方开始；滚动后内容可经过悬浮层后方，未被按钮覆盖的区域保持视觉与交互开放。主列表为底栏和唯一允许的 FAB 预留空间，禁止依赖不明来源的 magic padding。

独立编辑页的底部主动作可以固定，但滚动内容必须按动作栏实测高度加 navigation/IME inset 预留空间；在 2.0× 字体与键盘打开时，最后一个字段、helper、错误和分区标题仍须可滚动到动作栏上方。

应用窗口使用 edge-to-edge 透明状态栏／导航栏，并关闭 Android 强制导航栏对比 scrim。页面底色延伸至手势区，系统图标与手势条外观随当前浅／深主题切换。

布局节奏使用 4／8／12／16／24／32 dp。只为重复结构添加组件 tokens，例如页面 inset、卡片 padding、头像尺寸、底部内容预留与图标尺寸；不得 token 化偶发测量，也不得无理由覆盖 Material 组件默认值。

### 导航与流程

- 底栏／rail 主入口为轨迹、清单、二维码；左上圆形头像进入“设备与我”，并取代旧“我”底栏／rail 入口。
- 主导航保留 tab、滚动与流程状态。
- 系统返回与顶栏返回一致；支持的独立路由必须支持预测返回。
- 滚动表单、异步解析、多步骤工作、大列表、破坏性管理和需要恢复状态的流程使用独立页面。
- AlertDialog 只用于单字段快速增改、删除／不可逆确认或极短选择。
- ModalBottomSheet 用于无需独立恢复的上下文详情或短选择。

**单一主动作规则。** 每屏只有一个视觉主动作；禁止多个相同尺寸 FAB。轨迹页只保留“添加”为 FAB；地图与地点库以两个持续可见、易点击的低强调图标入口保留在主界面，不藏入 overflow，也不强制添加文字标签。

**独立状态规则。** 离开再返回时若必须保留有意义工作，该流程必须是页面，不是大型对话框或内嵌管理区。

## Elevation & Depth

深度以色调为主。普通卡片使用平面或低层级表面；状态差异优先由内容、图标和明确语义色表达，不把整张卡片默认染成强调色；导航、sheet、dialog 和正在拖拽的清单项可高于内容。

阴影不是默认分层方式。浮动／拖拽元素需要从繁忙地图中分离时可使用，但不能替代 surface-container 阶梯。深色模式通过炭黑 surface 阶梯表达层级，不使用灰色投影。

动效传达状态：

- 详情进入：短促 decelerate slide/fade。
- 详情退出：更短 accelerate slide/fade。
- 清单完成与排序：克制 spring + 触觉反馈。
- 同步：只在工作进行时持续旋转图标。
- 尊重 Android 动画时长设置，禁止环境动效和页面加载编舞。

**色调优先规则。** 静止 elevation 先用容器色调；阴影／elevation 变化只用于移动、聚焦或模态分离。

## Shapes

形状语言柔和、系统化：

- Small（8 dp）：Material 组件未提供形状时的紧凑控件和输入框。
- Medium（12 dp）：普通卡片与列表容器。
- Large（16 dp）：突出卡片、头像框与高层容器。
- Extra large（28 dp）：dialog、sheet 与大型表达容器。
- Full／pill：仅用于语义需要的按钮、chip、进度轨与圆节点。

旅程带节点和连接线只用于真实有序轨迹内容，不得作为装饰、badge 或普通列表圆点复用。

**有序内容规则。** 数字、节点、连接线都暗示顺序，只在数据确有顺序时出现。

## Components

### App 外壳

外壳负责地图背景、无底色悬浮操作层、底栏／rail、设备状态、最大内容宽度和主动作槽。一级页面不再使用全宽顶部 chrome 或页面标题；二级页面继续使用正常标题／返回顶栏。当前底栏／rail 入口通过图标、标签和选中容器共同表达，不仅靠颜色。

设备状态区分未连接、已保存手机、待同步、同步中、已同步和可重试失败。成功可短暂；失败持续到解决或关闭，并可查看完整信息。

### 按钮与动作

- Filled Button：唯一建设性主动作。
- Filled Tonal Button：重要次要动作。
- Outlined Button：三级或替代动作。
- Text Button：dialog 与低强调动作。
- Icon Button：熟悉的紧凑动作，必须有精确 content description；破坏性动作与常规动作分离。
- FAB：每页最多一个浮动主动作。轨迹页地图与地点库使用持续可见的次要图标控件，不与添加使用相同尺寸、色调或 elevation。
- 固定底部保存／添加动作使用透明、负责 inset 的承载层；不得包裹全宽 tonal 色块。动作强调由按钮本身承担，同时保留 navigation bar 与 IME 避让。

每个动作按需支持 default、pressed、focused、disabled、loading、成功／失败反馈，以及至少 48 × 48 dp 点击区域。

### 卡片与行

普通卡片使用 card surface 和 12 dp 形状；摘要／选中使用对应语义容器。整卡点击只有一个主含义，相邻内联动作不得与之竞争。轨迹卡地点名始终单行尾部省略，卡片高度不随名称长度改变；其他长名称按已记录的内容规则处理。

轨迹卡垂直中点与旅程带节点对齐；同日第一项只向下连接，中间项上下连接，最后一项只向上连接。整卡进入编辑，不添加重复编辑图标。

### 清单

清单分为“待打卡”和“已完成”。不显示序号或可见排序按钮。长按排序只能在当前组内，松手后持久化，并保留 TalkBack“上移／下移”自定义动作。完成反馈使用有界、克制的 spring。

### 表单

表单放在有边界表面中，最大宽度 720 dp。输入后标签仍可见；helper／error 与字段相邻，使用语义色 + 文字／图标，禁止只靠颜色。点击空白可清焦，但不得消费子控件手势。

轨迹添加／编辑页顶栏只包含页面标题。记录方式、日期时间和精度不作为伪副标题；精度显示在坐标组附近，也可继续保留在轨迹卡片上。

真实名称、网址和标识符必须定义溢出策略。可编辑长名称优先增长到 2–3 行；必须单行时，聚焦态允许水平滚动，非聚焦态使用明确省略并向无障碍暴露完整值。禁止在字段边缘无提示硬裁切。

破坏性恢复／导入必须在替换前显示审阅摘要。备份管理等长管理流程使用独立页面；“我”页只提供摘要入口。

### 空、加载与错误状态

每个数据视图按需定义 loading、首次为空、过滤为空、error、offline、overflow。个人工具的空状态优先使用安静图标或一句短文案，不默认增加说明段落和文字 CTA；只有没有明显入口或下一步有成本时才给出动作。错误必须保留足够诊断信息。

### 收藏等级

收藏等级与地点库是轨迹体验的特色，但其数据迁移、BLE／同步含义与兼容性仍需单独批准。定时长按是被明确接受的主要收藏交互：它可以保持隐蔽、新奇，不要求常驻文字入口或首次提示；按住后的阶段反馈必须及时、可控且不误触发编辑。收藏 1～3 档统一采用“桃红—糖果粉—覆盆子红”的糖果红递进，轨迹卡、脉冲填充／描边、地点库筛选／卡片／星标和地图星标必须共用同一等级语义；交互荧光绿不得兼任收藏等级色。轨迹首次创建或加入收藏场所时，所有能够无歧义匹配该场所的未关联历史轨迹必须立即建立关联并继承等级。地点库入口则必须在轨迹主页持续可见。

## Do's and Don'ts

### Do

- **Do** 保留暖色糖果／炭黑灰粉体系、城市地图和旅程带；UI-U3.5 验收前保留明确的 `proposed` 状态。
- **Do** 以 `MaterialTheme.colorScheme`、typography、shapes 作为语义真相来源。
- **Do** 在透明 Scaffold 上显式建立 `onSurface` 内容色。
- **Do** 保持系统栏 edge-to-edge 且随主题自适应；固定底部动作承载层保持透明，同时完整处理 navigation／IME inset。
- **Do** 验证最终合成色，而不是只检查不透明 token 对。
- **Do** 验收 1.0×／2.0× 字体、浅色／深色、地图全缩放范围和 Compact／Medium／Expanded。
- **Do** 保持地图与地点库入口持续可见，同时让“添加”保持唯一主动作。
- **Do** 默认少写提示；让一致的图标、位置和反馈承担说明。
- **Do** 选择附近地点名称时保留真实打卡坐标，只修改显示名称。
- **Do** 新增外部 UI 与文档统一使用 `BugChecking`；旧 Passport 命名只保留在兼容／历史处。
- **Do** 用非颜色的文字／图标通道表达同步、完成、选中与错误状态。

### Don't

- **Don't** 仅为承接 inset 而在底部保存／添加按钮外增加全宽 tonal Surface。
- **Don't** 用顶栏副标题重复已由可编辑字段或邻近元信息表达的状态事实。

- **Don't** 恢复已被取代的旧色板，也不要让普通组件通过默认 primary／secondary 自动获得荧光绿。
- **Don't** 移除城市地图或把它重画为通用渐变、玻璃壁纸或装饰插图。
- **Don't** 堆叠多个同权重 FAB，也不要把地图与地点库藏入 overflow。
- **Don't** 让轨迹卡地点名换行或因名称长度改变卡片高度。
- **Don't** 为假设中的新用户在页面上堆叠教程、帮助段落和重复按钮名称。
- **Don't** 在导致文字对比失败时用透明度制造层级。
- **Don't** 把手机布局拉伸到平板，也不要跨折叠铰链放内容。
- **Don't** 把有状态表单、备份管理或多步骤导入放进长 Dialog。
- **Don't** 添加可见清单排序控件或重复轨迹卡编辑图标。
- **Don't** 在没有诊断证据与针对性测试时按后缀拆分 Geocoder 名称。
- **Don't** 通过纯视觉改动引入新的收藏／BLE／数据迁移语义。

## Acceptance gates

相关 gate 未通过前，UI 改动不算完成：

1. **视觉：** Compact 浅色／深色与 1.0×／2.0× 截图；涉及布局时补 Medium／Expanded。
2. **对比度：** 普通文字 >=4.5:1，大字和 UI 边界 >=3:1；以最终合成实测或计算。
3. **操作性：** 48 dp 目标、状态反馈与现有 TalkBack 语义不得无故回退；隐藏收藏手势不以首次发现性作为验收项。
4. **状态：** 按需覆盖 loading、empty、error、offline、overflow、disabled 与破坏性确认。
5. **导航：** 系统返回、顶栏返回、预测返回、状态恢复和 tab 保留。
6. **Android 验证：** `testDebugUnitTest`、零错误 `lintDebug`、`assembleDebug`。
7. **真机：** 任何视觉、手势、inset、动态色、BLE 状态或键盘改动都要在目标手机验收。
8. **地图几何：** 在最小、中间和最大缩放级别，以及缩放、平移、旋转连续操作后，所有点位与轨迹线都与地理坐标稳定锚定。
