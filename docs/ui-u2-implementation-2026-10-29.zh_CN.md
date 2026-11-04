**简体中文** · [English](ui-u2-implementation-2026-10-29.md)

# Been Android UI-U2 实施检查点

日期：2026-10-29
更新：2026-10-30
状态：`ACCEPTED`
前置：UI-U1 已由所有者明确验收

## 1. 本阶段边界

UI-U2 用 renderer 持有的地理几何替换屏幕空间 annotation 重建；保留现有 OpenFreeMap 样式、轨迹／收藏颜色和回中动作，并把点位详情收敛为轨迹卡定位流程。不改变轨迹主页动作层级。

## 2. 已实施候选

- 直接使用原始经纬度生成路线 GeoJSON source，并由 MapLibre `LineLayer` 绘制。
- 使用同一份原始坐标生成点位 GeoJSON source；普通点由 `CircleLayer` 表达，收藏等级 1～3 由 `SymbolLayer` 恢复既有五角星语义与等级色。
- 五角星以 36 dp 密度适配位图注册进 MapLibre style，关闭符号碰撞隐藏；图形变化不改变 feature 坐标，也不恢复屏幕 annotation。
- feature 属性保留轨迹 ID，点击 renderer 实际绘制的点位后继续打开原详情工作表；命中范围额外放宽 12 dp。
- 地图详情 sheet 精简为地点名、打卡日期时间和唯一的“在轨迹记录中定位”按钮；移除精度、编辑和外部地图动作。按钮复用既有轨迹焦点机制，返回主页并滚动到对应卡片，同一轨迹重复进入地图后也可再次定位。
- 定位反馈在滚动完成后显示 2 dp 主题主色描边，保持 1.8 秒并用 450 ms 淡出；不改变卡片高度、收藏底色或交互语义。
- 每个本地日期内按时间排列路线顶点，保留全部点 feature，包括坐标重合点。
- 只有轨迹数据真正变化时才更新 source；相机回调不再清空或重建 annotation。
- 诊断升级到 MapLibre Android 13.6.1 后，PLZ110 原生聚类仍未恢复，因此项目维持原 11.11.0 依赖。

## 3. 原生聚类阻塞

当前安装候选未启用原生聚类。Android 17 的 PLZ110 上，只要 runtime `GeoJsonSource` 启用 cluster，source feature 数和渲染 feature 数都变为 0；相同数据关闭 cluster 后可正常绘制。

以下诊断组合都复现空聚合 source：

- MapLibre Android 11.11.0 与 13.6.1；
- `FeatureCollection`、raw JSON 字符串和应用私有 file URI；
- 默认异步更新与 `withSynchronousUpdate(true)`；
- 构造时提供数据与 source 挂载后再写入；
- 正常 cluster filter 与无 filter 的圆层；
- 完整 27 点 fixture 与两个合法且不同坐标的最小 fixture。

fixture 统计为 27 个合法 point feature、26 个唯一坐标、1 组重合点，没有非有限值或越界坐标。关闭聚类后全部点位可渲染，因此问题已隔离到原生聚类路径，不是坐标生成或 layer 样式。临时 13.6.1 诊断路径已删除，包括所有日志和缓存文件代码。

相关上游资料：[官方聚类示例](https://maplibre.org/maplibre-native/android/examples/styling/circle-layer/)、[`synchronousUpdate` 图层消失问题](https://github.com/maplibre/maplibre-native/issues/4035)、[GeoJSON runtime 渲染问题](https://github.com/maplibre/maplibre-native/issues/4471)。

## 4. 当前验证结果

| 门禁 | 结果 | 说明 |
|---|---|---|
| GeoJSON helper 单元测试 | PASS | 每个点保留坐标、ID 与收藏层级；路线顶点保持时间顺序 |
| `testDebugUnitTest` | PASS | 已在稳定的无聚类候选上重跑 |
| `assembleDebug` | PASS | 已保留数据安装到连接的 PLZ110 |
| 最终候选 `lintDebug` | PASS | 稳定候选无 lint 阻断项 |
| 点位详情 | PASS | 可见点位打开精简 sheet：地点名、打卡日期时间和唯一轨迹卡定位动作 |
| 收藏五角星 | PASS | PLZ110 浅色真机可见两个收藏星标；无遮挡星标可打开正确地点详情 |
| 收藏／普通点坐标源 | PASS | 圆点与五角星查询同一 point source，收藏只改变 renderer layer 表现 |
| 地图详情到轨迹卡片 | PASS | PLZ110 深色真机确认：无精度、单按钮、返回轨迹主页并滚动到正确卡片 |
| 目标卡定位反馈 | PASS | 滚动完成后描边清晰可见；1.8 秒保持并在 450 ms 内淡出，无残留或布局变化 |
| 5 次双击缩放、平移、回中 | PASS（仅崩溃／恢复） | 回中后恢复完整点线视图 |
| 10 次快速双指缩放与旋转 | NOT RUN | ADB 不能代替所有者手势 |
| 相机 idle 后投影误差不超过 2 px | NOT RUN | 需最终聚类架构和测量工具 |
| 聚类数量守恒 | FAIL / BLOCKED | 此设备上的原生聚合 source 为空 |
| 清缓存离线失败与重试 | NOT RUN | 所有者验收前必须执行 |

截图含私人地点数据，只作为仓库外临时本地证据。

最终自动门禁 `testDebugUnitTest lintDebug assembleDebug` 结果为 `BUILD SUCCESSFUL`（51 个任务，8 个执行、43 个 up-to-date）；仓库检查与 `git diff --check` 同样通过。2026-10-30 已安装延长定位反馈后的最终候选 APK SHA-256：`7F8E0948B81608E5742E57B7F97C109C53DA920F3CD366777F01FE36B8BF3E64`。

## 5. 门禁状态

- 所有者于 2026-10-30 明确确认“UI-U2 验收完成”；UI-U3 已解锁。
- `map/route-line-color`、`map/route-line-width`、`map/point-size`、`map/point-outline-width`、`map/favourite-point-colors`、`motion/track-focus-cue` 与 renderer 坐标架构契约晋级为 `accepted`。
- 原生聚类、数量守恒、自动投影测量和离线门禁维持表中原状态；`map/cluster-radius`、`map/cluster-zoom-thresholds` 仍为 `proposed`，没有被本次验收错误晋级。

## 6. 下一项决策

UI-U2 已封板。下一阶段进入 UI-U3 轨迹主页特色动作层级；原生聚类兼容性作为独立延期项保留，不阻塞当前个人数据规模，也不污染 UI-U3 范围。
