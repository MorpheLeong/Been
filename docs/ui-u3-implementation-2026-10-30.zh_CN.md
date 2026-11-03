**简体中文** · [English](ui-u3-implementation-2026-10-30.md)

# BugChecking Android UI-U3 实施与验收检查点

日期：2026-10-30
状态：`ACCEPTED`
前置：UI-U2 已由所有者明确验收

## 1. 边界

UI-U3 只调整轨迹主页的动作层级和卡片标题溢出规则。四个主入口、“动态柔糖／夜雾”视觉身份、旅程带、隐藏长按收藏、整卡点击编辑、地图实现及 UI-U0～UI-U2 已验收契约均保持不变。

## 2. 实施结果

- “添加轨迹”保留为唯一浮动动作按钮，使用标准 Material 3 主 FAB 角色。
- 原来另外两个同权重 FAB 合并为滚动 viewport 上方的固定次级动作条；地图与地点库持续出现在首屏，永不藏入 overflow。
- 特色动作条通过 `secondaryContainer`／`onSecondaryContainer`、16 dp large shape 和 1 dp tonal elevation 解析。两个动作分别拥有 48 × 48 dp IconButton 目标与准确的 content description。
- 动作条顶部间隔为 8 dp。它占用真实布局空间，不覆盖轨迹卡片。
- 轨迹列表底部预留 104 dp 给唯一主 FAB，使最后一张卡可滚动避让。
- 所有地点名固定一行并使用尾部省略号；短名、中文长名和中英混合名均不换行、不改变卡片高度。
- 隐藏定时长按收藏与整卡点击编辑完全沿用原逻辑。

## 3. 验证

| 门禁 | 结果 | 证据 |
| --- | --- | --- |
| 首屏动作层级 | PASS | 添加、地图、地点库同时可见；只有添加使用 FAB |
| 特色入口目标 | PASS | PLZ110 UI hierarchy 显示两个独立的 168 × 168 px 目标，等效 48 × 48 dp |
| 卡片遮挡 | PASS | 特色动作条位于滚动 viewport 上方并占用布局空间，不覆盖卡片 |
| 1.0× 字体 | PASS | 短名、长名和中英混合 fixture 均保持单行尾部省略与一致卡片结构 |
| 2.0× 字体 | PASS | 三张可见 fixture 卡片实测高度均为 385 px；无动作与底部导航碰撞 |
| 导航 | PASS | 地图、地点库及整卡编辑均打开预期页面 |
| 收藏交互 | PASS | 卡片语义仍为 long-clickable；既有按住阶段单元测试通过，实现路径未改动 |
| Android 门禁 | PASS | 最终 token 化代码上的 `testDebugUnitTest`、`lintDebug`、`assembleDebug` 全部成功（51 tasks：15 executed，36 up-to-date） |
| 仓库检查 | PASS | `git diff --check` 无空白错误；无关脏工作区修改均保留 |

最终安装的调试 APK 为 `BugChecking-debug.apk`，SHA-256：`583DDD1E181973835396DBEE9B04BE5D07F40F4D65E3B0A355E86B4A4A51624C`。2.0× 测试后系统字体已恢复为 1.0×。截图包含私人地点数据，检查后已删除。

## 4. 已验收 token 与契约

| ID | 已验收值／行为 |
| --- | --- |
| `action/track-primary-fab` | “添加轨迹”是唯一 FAB，使用标准 Material 3 主 FAB 角色 |
| `action/track-signature-icon` | 共用次级动作条；`secondaryContainer`／`onSecondaryContainer`；16 dp shape；1 dp tonal elevation；每个动作 48 × 48 dp |
| `layout/track-action-gap` | 8 dp |
| `layout/track-action-safe-reserve` | 列表底部为唯一 FAB 预留 104 dp；特色入口位于滚动 viewport 外 |
| `track-card/title-max-lines` | 1 |
| `track-card/title-overflow` | 尾部省略号 |

这些是 BugChecking 项目契约，不自动成为天色 beta 规则或跨项目个人偏好。

## 5. 阶段状态

所有者于 2026-10-30 明确确认 UI-U3 验收通过。UI-U3 已封板，UI-U4“跨页一致性与 token 收敛”已解锁。
