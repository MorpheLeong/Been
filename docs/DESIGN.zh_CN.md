**简体中文** · [English](DESIGN.md)

---
name: BugChecking Android
description: BugChecking Android 配套 App 的强制 UI 与交互规范。
colors:
  route-blue: "#4B69A2"
  passport-ink: "#172A3A"
  candy-blue: "#D9E5FF"
  waypoint-mint: "#3A7566"
  candy-mint: "#C5F1DF"
  journey-peach: "#95566B"
  candy-peach: "#FFD9E3"
  candy-lemon: "#FFE8A3"
  mist-surface: "#FFF9F2"
  night-surface: "#202B36"
  night-on-surface: "#F0F4F8"
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

视觉世界由居中裁切的网点城市地图、浅色 Material 3“动态柔糖”容器和深色“夜雾”蓝灰表面组成。地图是一层安静的地理记忆，不是正文；卡片与控件作为可读、面向任务的层级覆盖在其上。品牌感来自旅程带、地图纹理、柔糖语义容器和精准动效，而不是装饰渐变、光晕或新奇控件。

**关键特征：**

- 可辨识城市地图底图和按角色区分的半透明表面。
- 明亮但安静的柔糖容器；深色使用蓝灰“夜雾”，不用纯黑。
- 克制的 Material 3 组件词汇与 Material 图标家族。
- 每个页面一个主操作；轨迹页的地图与地点库是必须持续可见的特色入口，但视觉权重低于添加。
- 动效只服务状态，不做装饰编舞。
- 深色、长内容、地图缩放和系统 inset 下不得发生不可读、错位或数据表达错误。

**保留地图册规则。** 精修可以调整透明度、层级与对比，但没有明确产品批准，不得移除地图识别、“动态柔糖”、“夜雾”或旅程带。

**任务优先规则。** 当视觉表达与阅读、目标点击、错误恢复或 Android 规范冲突时，任务优先。

**个人工具规则。** 不为假设中的新用户增加常驻教程、提示段落或文字按钮。只有错误恢复、不可逆／高成本操作、权限原因或无法从状态本身理解的歧义需要显式说明。

## Colors

### 架构

颜色采用三层结构：

1. **基础色板：** frontmatter 中的命名静态颜色，以及 `PassportDesign.kt` 内完整浅色／深色色调。
2. **语义角色：** `MaterialTheme.colorScheme` 的 `primary`、`secondary`、`tertiary`、对应 container／`on*`、surface-container 阶梯、`outline`、`outlineVariant` 与 `error`。
3. **组件角色：** 主页面 chrome、导航、卡片、摘要、选中、收藏等级和地图标记。除非固定产品色确有必要并被记录，组件角色必须通过语义角色解析。

### 浅色模式

- Route Blue 是动作与旅程强调色。
- Candy Blue 是高强调摘要／主容器。
- Waypoint Mint 与 Candy Mint 用于次要动作和地点／收藏含义。
- Journey Peach 与 Candy Peach 只作互补强调，不作通用装饰。
- Candy Lemon 只保留给最高中性表面，不代表警告状态。
- Mist Surface 是静态回退底色。

### 深色模式

- “夜雾”使用从 lowest 到 highest 的蓝灰色调阶梯，禁止替换为纯黑。
- 文字与图标必须使用 `night-on-surface` 和已定义的 on-container 色。
- 透明 Scaffold 上的页面级内容必须显式继承 `onSurface`，不得依赖环境默认值。
- 动态深色可向已验收的“夜雾”容器柔化，但成对 on 色必须持续有效。

### 地图与透明度

- 城市地图居中裁切，可按主题降低饱和度和亮度。
- 透明度按角色定义，不由页面随意决定：顶／底 chrome、普通列表卡、摘要／选中卡、表单容器是不同角色。
- 地图署名、缩放／定位控件和 viewport 必须消费 `safeDrawing`／navigation bar inset；署名不得贴住手势条、被地图标签覆盖或在屏幕边缘裁切。
- 轨迹点与轨迹线使用地图渲染器的地理坐标 source／layer 绘制，随缩放、旋转和平移保持锚定；不得依赖相机空闲后清空并重建屏幕注记来维持位置。
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

四个主页面共享同一主内容起点和外壳几何。内容在最大宽度内顶部对齐。主列表为底栏和唯一允许的浮动主动作预留空间，禁止依赖不明来源的 magic padding。

独立编辑页的底部主动作可以固定，但滚动内容必须按动作栏实测高度加 navigation/IME inset 预留空间；在 2.0× 字体与键盘打开时，最后一个字段、helper、错误和分区标题仍须可滚动到动作栏上方。

布局节奏使用 4／8／12／16／24／32 dp。只为重复结构添加组件 tokens，例如页面 inset、卡片 padding、头像尺寸、底部内容预留与图标尺寸；不得 token 化偶发测量，也不得无理由覆盖 Material 组件默认值。

### 导航与流程

- 主入口固定为轨迹、清单、二维码、我。
- 主导航保留 tab、滚动与流程状态。
- 系统返回与顶栏返回一致；支持的独立路由必须支持预测返回。
- 滚动表单、异步解析、多步骤工作、大列表、破坏性管理和需要恢复状态的流程使用独立页面。
- AlertDialog 只用于单字段快速增改、删除／不可逆确认或极短选择。
- ModalBottomSheet 用于无需独立恢复的上下文详情或短选择。

**单一主动作规则。** 每屏只有一个视觉主动作；禁止多个相同尺寸 FAB。轨迹页只保留“添加”为 FAB；地图与地点库以两个持续可见、易点击的低强调图标入口保留在主界面，不藏入 overflow，也不强制添加文字标签。

**独立状态规则。** 离开再返回时若必须保留有意义工作，该流程必须是页面，不是大型对话框或内嵌管理区。

## Elevation & Depth

深度以色调为主。普通卡片使用平面或低层级表面；选中／摘要使用更强语义容器；导航、sheet、dialog 和正在拖拽的清单项可高于内容。

阴影不是默认分层方式。浮动／拖拽元素需要从繁忙地图中分离时可使用，但不能替代 surface-container 阶梯。深色模式通过“夜雾”色调阶梯表达层级，不使用灰色投影。

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

外壳负责地图背景、顶栏、底栏／rail、内联设备状态、最大内容宽度和主动作槽。顶／底 chrome 使用对应半透明角色。当前入口通过图标、标签和选中容器共同表达，不仅靠颜色。

设备状态区分未连接、已保存手机、待同步、同步中、已同步和可重试失败。成功可短暂；失败持续到解决或关闭，并可查看完整信息。

### 按钮与动作

- Filled Button：唯一建设性主动作。
- Filled Tonal Button：重要次要动作。
- Outlined Button：三级或替代动作。
- Text Button：dialog 与低强调动作。
- Icon Button：熟悉的紧凑动作，必须有精确 content description；破坏性动作与常规动作分离。
- FAB：每页最多一个浮动主动作。轨迹页地图与地点库使用持续可见的次要图标控件，不与添加使用相同尺寸、色调或 elevation。

每个动作按需支持 default、pressed、focused、disabled、loading、成功／失败反馈，以及至少 48 × 48 dp 点击区域。

### 卡片与行

普通卡片使用 card surface 和 12 dp 形状；摘要／选中使用对应语义容器。整卡点击只有一个主含义，相邻内联动作不得与之竞争。轨迹卡地点名始终单行尾部省略，卡片高度不随名称长度改变；其他长名称按已记录的内容规则处理。

轨迹卡垂直中点与旅程带节点对齐；同日第一项只向下连接，中间项上下连接，最后一项只向上连接。整卡进入编辑，不添加重复编辑图标。

### 清单

清单分为“待打卡”和“已完成”。不显示序号或可见排序按钮。长按排序只能在当前组内，松手后持久化，并保留 TalkBack“上移／下移”自定义动作。完成反馈使用有界、克制的 spring。

### 表单

表单放在有边界表面中，最大宽度 720 dp。输入后标签仍可见；helper／error 与字段相邻，使用语义色 + 文字／图标，禁止只靠颜色。点击空白可清焦，但不得消费子控件手势。

真实名称、网址和标识符必须定义溢出策略。可编辑长名称优先增长到 2–3 行；必须单行时，聚焦态允许水平滚动，非聚焦态使用明确省略并向无障碍暴露完整值。禁止在字段边缘无提示硬裁切。

破坏性恢复／导入必须在替换前显示审阅摘要。备份管理等长管理流程使用独立页面；“我”页只提供摘要入口。

### 空、加载与错误状态

每个数据视图按需定义 loading、首次为空、过滤为空、error、offline、overflow。个人工具的空状态优先使用安静图标或一句短文案，不默认增加说明段落和文字 CTA；只有没有明显入口或下一步有成本时才给出动作。错误必须保留足够诊断信息。

### 收藏等级

收藏等级与地点库是轨迹体验的特色，但其数据迁移、BLE／同步含义与兼容性仍需单独批准。定时长按是被明确接受的主要收藏交互：它可以保持隐蔽、新奇，不要求常驻文字入口或首次提示；按住后的阶段反馈必须及时、可控且不误触发编辑。地点库入口则必须在轨迹主页持续可见。

## Do's and Don'ts

### Do

- **Do** 保留“动态柔糖”、“夜雾”、城市地图和旅程带。
- **Do** 以 `MaterialTheme.colorScheme`、typography、shapes 作为语义真相来源。
- **Do** 在透明 Scaffold 上显式建立 `onSurface` 内容色。
- **Do** 验证最终合成色，而不是只检查不透明 token 对。
- **Do** 验收 1.0×／2.0× 字体、浅色／深色、地图全缩放范围和 Compact／Medium／Expanded。
- **Do** 保持地图与地点库入口持续可见，同时让“添加”保持唯一主动作。
- **Do** 默认少写提示；让一致的图标、位置和反馈承担说明。
- **Do** 选择附近地点名称时保留真实打卡坐标，只修改显示名称。
- **Do** 新增外部 UI 与文档统一使用 `BugChecking`；旧 Passport 命名只保留在兼容／历史处。
- **Do** 用非颜色的文字／图标通道表达同步、完成、选中与错误状态。

### Don't

- **Don't** 恢复已被取代的旧色板，也不要把“夜雾”换成纯黑。
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
