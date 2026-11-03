**简体中文** · [English](ui-u0-baseline-2026-10-29.md)

# BugChecking Android UI-U0 基线与证据记录

状态：`ACCEPTED`，所有者于 2026-10-29 明确确认“UI-U0 验收通过”
记录时间：2026-10-29 01:31（Asia/Shanghai）
阶段来源：[UI 调整排期与验收计划](android-ui-adjustment-roadmap-2026-10-28.zh_CN.md#ui-u0基线冻结与证据准备)

## 1. 范围与证据规则

本阶段只冻结事实、准备 synthetic fixture 和记录证据，不修改 UI 代码、不清除或导出个人数据、不晋级 token。经所有者明确授权，使用 `adb install -r` 保留数据更新到当前工作区 APK；未执行应用内保存、删除或数据迁移。受众按“个人使用的熟练用户工具”处理；定位和轨迹数据仍属于敏感数据，不因个人工具定位降低保护要求。

证据优先级为真机运行 > 真机截图／录屏 > 当前代码／资源 > 产品所有者描述。代码只能证明实现机制，不能替代渲染结果。本轮在所有者解锁设备后完成真机观察；原始截图和 UI hierarchy 含私人地点、精确坐标、二维码密钥或头像，审阅后仅保留 SHA-256 清单并删除原文件，不提交隐私内容。

## 2. 冻结的工作区与 APK

| 项目 | 基线值 |
| --- | --- |
| 分支 | `feature/location-passport-app` |
| 基础 commit | `009e387d8645f9a7177b9b76d439e7919b23a8a6` |
| commit 摘要 | `feat(passport): add place library and layered backups` |
| commit 时间 | 2026-10-28 22:34:39 +08:00 |
| 工作区 | dirty；仅本轮文档修改／新增，详见附录状态 |
| tracked diff 指纹 | Git blob SHA-1 `cc994bce7da40a4ec8025571cac520461006f997` |
| Android applicationId | `com.bugchecking.app` |
| 源码版本 | `versionCode=1`，`versionName=0.1.0` |
| 已安装版本 | `versionCode=1`，`versionName=0.1.0`，`targetSdk=36` |
| 更新前已安装 APK | 2026-10-28 04:51:02；61,029,498 bytes；SHA-256 `717B0019A47F04943ADB8707271600FA88079119A23A1DA1432484F95FAA9060` |
| 更新后已安装 APK | 2026-10-29 01:19:24；123,055,609 bytes；SHA-256 `221750AD8548B68C3A354AC20DE777B62F2B60BE303A62D1FFE3760D9C915C9E` |
| 当前工作区构建 | `assembleDebug` PASS；`BugChecking-debug.apk` 123,055,609 bytes；SHA-256 `221750AD8548B68C3A354AC20DE777B62F2B60BE303A62D1FFE3760D9C915C9E` |

已安装 APK 只拉取到系统临时目录计算 hash，随后删除；没有复制到仓库。

**产物对应已解决：** 更新前 APK 与当前工作区构建不同。所有者明确确认更新后，`adb install -r` 返回 `Success`；随后重新拉取已安装 APK 计算 SHA-256，与当前工作区构建完全一致。以下真机观察可归属于本记录冻结的工作区产物。

## 3. 固定真机条件

| 维度 | 读取值 | 解释／后续 |
| --- | --- | --- |
| 设备 | OnePlus PLZ110 / `OP64DDL1` | 固定目标真机 |
| Android | Android 17，API 37 | build `PLZ110_17.0.0.106(CN01)` |
| build fingerprint | `OnePlus/PLZ110/OP64DDL1:17/CP2A.260605.016/B.34f780d-ee9c62-ef681b:user/release-keys` | U0 冻结 |
| 屏幕 | 1216 × 2640 px，560 dpi，竖屏 | roadmap 的固定矩阵一致 |
| Locale | `zh-Hans-CN` | U0 冻结 |
| 字体倍率 | 1.0× | 1.3×／1.5×／2.0× 尚未执行 |
| 导航模式 | raw `navigation_mode=2` | 手势导航；已由底部手势条视觉确认 |
| IME | `com.oplus.keyboard/.service.ImeService` | 固定为本轮键盘基线 |
| 夜间模式 | `custom_schedule`，23:00–06:00；当前 computed night = true | 当前是动态深色基线 |
| 动态色 | home wallpaper，`TONAL_SPOT`，system palette/accent `#1A8CFF` | `material_you_overlay_enable=1` |
| 访问 | 所有者已解锁 | 未绕过认证；观察完成后保持应用数据不变 |
| 产物对应 | 已安装 hash = 当前工作区 hash | 当前真机证据可归属于冻结产物 |

静态回退浅／深、动态浅色、三键导航仍需在保持可恢复的前提下逐项采集；任何系统设置变化都必须在证据记录后恢复。

## 4. Synthetic fixture 目录

以下数据只定义测试条件，不写入当前个人数据库。后续若需导入，应使用隔离调试数据集或在备份现有数据并获得明确同意后执行。

| Fixture ID | 数据条件 | 用途 |
| --- | --- | --- |
| `TR-SHORT` | 地点名“外滩”，普通轨迹，精度 15 m | 默认卡片与值／单位原子性 |
| `TR-LONG` | 超长中文地点名“上海外滩老码头亚朵酒店秀水苑南门临江观景平台”，普通轨迹 | 单行省略、编辑器长值、卡片等高 |
| `TR-MIXED` | “M.O.D TOKYO 池袋店 Extension Studio 2F”，收藏等级 3 | 中英数字混排与收藏色 |
| `TR-SAME-DAY-A/B/C` | 同一天三个不同坐标与时间，两个坐标彼此接近 | 旅程带、路线顺序、聚类／拆分 |
| `TR-NEXT-DAY` | 次日轨迹 | 跨日分组与路线不得跨日相连 |
| `TR-FAV-1/2/3` | 收藏等级 1／2／3，坐标计算相同 | 收藏样式不得改变地理位置 |
| `PL-SHORT/LONG` | 短名与超长名场所，含 1／3 级收藏 | 地点库与场所详情 |
| `QR-SHORT` | 短名称、短 payload、未星标 | 默认二维码卡／展示页 |
| `QR-LONG` | 长名称；UTF-8 payload 接近 192 bytes；已星标并绑定清单项 | 编辑器回流、动作竞争、展示错误边界 |
| `CL-PENDING/DONE` | 同组多项、已完成项、长名称、绑定二维码项 | 清单密度、排序、状态分组 |
| `EMPTY-*` | 轨迹／清单／二维码／地点库为空 | 空状态基线 |
| `FAIL-MAP` | 地图样式加载失败后恢复 | 错误、返回、重试与 layer 重建 |
| `FAIL-SYNC` | 可重试同步失败，错误文本超过 Compact 顶栏宽度 | 顶栏错误持续性与完整诊断 |

地图 fixture 必须额外记录 GeoJSON point feature 总数；后续验收使用“未聚类 feature 数 + 各 cluster `point_count` 总和”与该总数比较，不使用轨迹卡数量或日期组数量。

## 5. 截图与录屏矩阵

证据文件统一放入 [`docs/evidence/ui-u0-2026-10-29/`](evidence/ui-u0-2026-10-29/README.zh_CN.md)，命名格式为：

```text
<theme>-<font>-<navigation>-<screen>-<state>.<png|mp4>
```

| 页面／状态 | 动态深色 1.0× | 动态浅色 1.0× | 2.0× | 其它 |
| --- | --- | --- | --- | --- |
| 轨迹主页：当前个人数据 | OBSERVED | NOT RUN | NOT RUN | 长名称保持单行省略，卡片等高；三 FAB 遮挡列表 |
| 清单主页：当前个人数据 | OBSERVED | NOT RUN | NOT RUN | “待打卡”分区标题深色低对比 |
| 二维码主页：当前个人数据 | OBSERVED | NOT RUN | NOT RUN | 三个同权重行内动作已确认；未执行删除 |
| 我：个人身份／设备设置 | OBSERVED | NOT RUN | NOT RUN | “个人身份”分区标题深色低对比 |
| 添加轨迹 | OBSERVED | NOT RUN | NOT RUN | 未保存；长地点值横向截断 |
| 编辑轨迹 | OBSERVED | NOT RUN | NOT RUN | 未保存；长地点值横向截断 |
| 添加／编辑／展示二维码 | OBSERVED | NOT RUN | NOT RUN | 未保存；真实 payload 未留存 |
| 智能导入 | OBSERVED：空页 | NOT RUN | NOT RUN | 未执行解析／失败／预览 |
| 地点库／场所详情 | OBSERVED | NOT RUN | NOT RUN | 未保存；详情页固定底部动作已覆盖 |
| 轨迹地图 | OBSERVED：静态 | NOT RUN | NOT RUN | 署名与系统手势区冲突；动态错位保留所有者已复现 |

固定矩阵仍要求：全部触及页面 1.0×／2.0×；轨迹、我、轨迹编辑、二维码编辑追加 1.3×／1.5×；手势导航覆盖全部触及页面，三键导航覆盖地图和带底部动作的编辑页。

## 6. 地图动态复现脚本

当前状态：产品所有者已复现动态点线错位；本阶段执行者已观察静态地图和署名冲突，但未独立完成连续捏合复现。按排期门禁保留以下可重复步骤，不得描述为已验证或已修复。

1. 打开轨迹主页，再打开“地图模式”。
2. 等待地图样式、路线和点位全部稳定，记录 fixture point feature 总数。
3. 依次停在 zoom 4／8／12／16／19，每级截图并检查点位、路线、署名。
4. 连续执行 10 次快速双向捏合；每次相机 idle 后观察点位是否闪失、挤压、跳位，路线是否漂移或断裂。
5. 连续平移；旋转地图；点“重新定位／适配全部轨迹”。
6. 在每个 idle 状态比较单点 + cluster `point_count` 与 fixture feature 总数。
7. 对普通点和收藏点使用同一坐标，确认仅样式不同。
8. 录制完整过程；若复现，记录首次出现的 zoom、操作序列、持续时间和恢复方式。

预期结果：点和路线始终锚定真实地理坐标；相机 idle 后没有旧 annotation／cluster 残留；署名不进入系统手势区；返回和失败恢复可用。

## 7. 审计问题复现索引

| ID | 页面／状态 | 操作 | 预期基线结果 |
| --- | --- | --- | --- |
| A11Y-01 | 轨迹日期标题、我页分区标题，动态深色 | 打开页面并滚动全部分区 | 标题对比可读；当前审计证据预期复现不可读问题 |
| RESP-01 | 我、轨迹、二维码编辑，1.3×／1.5×／2.0× | 切换字体倍率并重新进入页面；打开键盘 | 无裁切／重叠；值与单位不拆分；当前审计证据预期 2.0× 失败 |
| VIS-01 | 轨迹主页，普通与 2.0× | 滚动到 FAB 覆盖区域 | 三入口可用；记录三个同权重 FAB 的遮挡面积 |
| INPUT-01 | 编辑轨迹，`TR-LONG` | 非聚焦观察、聚焦移动光标 | 全值可核对；记录末尾消失与滚动反馈 |
| MAP-01 | 地图，手势／三键导航 | 查看底部署名和地图 viewport | 署名完整避让；当前审计证据预期冲突 |
| MAP-02 | 地图，完整 zoom fixture | 执行第 6 节脚本 | 点线不消失／错位；当前由所有者报告故障 |
| A11Y-02 | 收藏轨迹／地点，动态浅深 | 查看普通文字和副文本 | 最终合成对比可读；需截图后像素测量 |
| UX-02 | 二维码卡 | 点击展示；分别触发星标、编辑、删除确认 | 动作边界明确；删除只到确认不执行 |
| DS-01 | 我页高密度表单 | 在地图最亮／最暗区域滚动 | 字段与地图纹理不竞争；记录透出程度 |
| PLAT-01 | Medium／Expanded | 本轮不声明支持 | NOT RUN，明确延期，不伪装 PASS |
| PLAT-02 | 独立页面返回 | 本轮若声明支持才测试预测返回 | 默认延期；系统／顶栏返回仍记录 |
| FBK-01 | `FAIL-SYNC` | 触发失败并等待超过 5 秒，再重试 | 错误持续且完整；当前需 isolated fixture |
| DS-02 | 全部触及页面 | 对照截图与代码登记重复角色值 | 只登记重复稳定项，不 token 化所有数值 |
| TASTE-01 | 四个主页面 | 比较 chrome、摘要、列表、表单 | 只记录层级，不把审美偏好升级为缺陷 |

## 8. 当前 SEEN／NOT SEEN

**SEEN**

- 当前工作区 APK 在 PLZ110、动态深色、1.0×、手势导航下的全部四个主页面，以及轨迹添加／编辑／地图、二维码添加／编辑／展示、智能导入、地点库／场所详情。
- A11Y-01 在轨迹日期、清单“待打卡”和我页“个人身份”标题上复现；VIS-01、INPUT-01、MAP-01、UX-02 也由真机画面确认。
- 轨迹长地点名在卡片上保持单行、省略号和等高，符合所有者约束；编辑器中的长值仍存在核对困难。
- 真机型号、系统、显示、字体、主题、动态色、IME、手势导航和更新后 APK hash。

**NOT SEEN**

- synthetic fixture 的空／极长／失败边界：未向个人数据库注入测试数据。
- 地图缩放错位的本轮独立录屏：仍保留“产品所有者已复现”。
- 动态浅色、静态回退浅／深、1.3×／1.5×／2.0×、三键导航。

## 9. UI-U0 当前判定

`ACCEPTED`。所有者于 2026-10-29 明确确认“UI-U0 验收通过”。工作区、更新前后 APK hash、设备条件、fixture、截图矩阵、地图脚本、审计问题索引和当前动态深色 1.0× 真机观察已经冻结；地图动态故障满足“所有者已复现 + 可重复步骤”的 U0 证据门槛。动态浅色、静态回退浅／深、1.3×／1.5×／2.0×、三键导航和 synthetic 失败边界仍为明确延期项，不得回写为 PASS。UI-U1 现可开始；UI-U0 没有产生可晋级的视觉 token，候选 token 状态不变。

## 附录：冻结时工作区状态

```text
## feature/location-passport-app
 M docs/README.md
 M docs/README.zh_CN.md
?? docs/android-ui-adjustment-roadmap-2026-10-28.md
?? docs/android-ui-adjustment-roadmap-2026-10-28.zh_CN.md
?? docs/personal-app-development-standard.md
?? docs/personal-app-development-standard.zh_CN.md
```

这些修改属于此前已生成、尚未提交的文档；UI-U0 不覆盖或清理它们。
