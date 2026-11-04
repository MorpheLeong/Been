<p align="right">
  <strong>简体中文</strong> · <a href="README.md">English</a>
</p>

# Been

**一套以 AI Passport 和 Android 为载体的个人地点记忆系统。**

我一直有打卡和记录地点的习惯。对我来说，这些记录不只是一份位置历史，也是在把自己的偏好、生活轨迹和当时的片段逐渐数字化，让它们在日后仍然可以被回看和整理。

但位置信息往往散落在不同服务里。为了留下记录，通常需要拿出手机、打开对应应用、找到入口，再走完一套流程。这种操作很容易打断正在做的事情，也让本来应该自然发生的记录变成一种负担。

Been 把 AI Passport 变成一个轻量的实体入口：在设备上执行一次简单操作，即可让 Android 伴侣应用获取一次手机定位并保存到本地。等到更合适的时候，再回到手机上确认结果、修改地点名称、补充信息并整理轨迹。

## 主要组成

- **AI Passport 固件：**快速打卡、轨迹历史、打卡清单、二维码展示和 BLE 通信。
- **Android 应用：**旅程时间线、轨迹编辑、地图模式、地点库、清单、二维码管理和设备同步。
- **本地优先：**手机上的修改默认保存在本地，只有用户明确操作时才同步到 AI Passport。
- **主动记录：**Been 只在用户触发时获取一次位置，不以持续后台追踪作为产品前提。

Android 应用使用 Jetpack Compose 与 Material 3 构建；设备固件面向 ESP32-C3 版 AI Passport，基于 ESP-IDF 5.5.3 开发。

## 隐私

Been 目前没有项目方运营的账户系统、分析后台或位置服务器。当前双端的数据处理方式如下：

- **本地保存：**Android 应用把轨迹、地点、清单、二维码内容、个人资料和设置保存在应用私有空间；AI Passport 只保存经已配对 BLE 连接同步给它的内容和设置。双端都不会把这些数据自动上传到 Been 自建服务器。
- **定位：**用户触发打卡时，手机才获取一次位置；Been 不以持续后台定位作为产品前提。地点名称解析使用 Android 系统 `Geocoder`，其具体实现可能把坐标发送给设备所配置的地理编码服务商。
- **地图：**打开地图时，应用会从 [OpenFreeMap](https://openfreemap.org/) 加载地图样式和瓦片，底层数据包含 OpenStreetMap。请求会按照 [OpenFreeMap 隐私政策](https://openfreemap.org/privacy/) 向 OpenFreeMap 及其内容分发服务商暴露所请求的地图区域和常规网络信息。
- **图片文字识别：**导入图片使用随应用打包的 Google ML Kit 模型在设备端识别。Google 声明输入图片和识别结果不会发送到其服务器，但 ML Kit 可能按其[条款与隐私说明](https://developers.google.com/ml-kit/terms)发送运行和使用指标。
- **设备同步：**Android 与 AI Passport 通过系统已配对的 BLE 链路通信，不经过云端中转。只有当用户主动打开外部地图、分享、导出或选择备份目录时，相应数据才会交给所选应用或存储服务商处理。
- **备份：**Been 已关闭 Android 系统云备份。用户主动创建的备份是写入 Android 文件选择器所选目录的 ZIP 文件，Been 不会再对其额外加密；如果用户选择了云盘目录，文件会受对应云服务商的规则约束。

本项目目前未集成广告 SDK、专用统计分析 SDK 或崩溃上报 SDK。本仓库公开截图和文档使用虚构地点、脱敏坐标和非真实地理路线；真实打卡坐标会按记录结果保留，选择附近地点只修改显示名称。

## 项目来源

Been 是基于开源 [FoloToy AI Passport](https://github.com/FoloToy/ai-passport) 硬件平台和板级支持基础开发的独立衍生项目。产品构想、Android 伴侣应用、交互设计和定制固件体验均由本项目独立维护，并非 FoloToy 官方发行版。

本仓库保留上游 MIT 许可证及版权声明，详见 [LICENSE](LICENSE)。

## 开源软件与外部服务

Been 直接使用的主要外部项目和服务包括：

- [FoloToy AI Passport](https://github.com/FoloToy/ai-passport)：上游硬件与板级支持基础，采用 MIT License。
- [ESP-IDF](https://github.com/espressif/esp-idf) 及 Espressif 固件组件：主要采用 Apache License 2.0；各随附组件保留其自身许可证和声明。
- [LVGL](https://github.com/lvgl/lvgl)：用于嵌入式界面，采用 MIT License。
- [AndroidX、Jetpack Compose 与 Material 3](https://developer.android.com/jetpack/androidx)：用于 Android 界面，采用 Apache License 2.0。
- [MapLibre Native](https://github.com/maplibre/maplibre-native)：用于地图渲染，采用 BSD 2-Clause License。
- [OpenFreeMap](https://openfreemap.org/)：提供在线地图样式与瓦片，使用时受其[服务条款](https://openfreemap.org/tos/)约束；底层 [OpenStreetMap](https://www.openstreetmap.org/copyright) 数据采用 ODbL 1.0，并要求保留署名。
- [Google ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)：用于设备端 OCR，使用时受 Google ML Kit 条款约束。
- [Apache POI](https://poi.apache.org/)、[ZXing](https://github.com/zxing/zxing) 与 [Reorderable](https://github.com/Calvin-LL/Reorderable)：采用 Apache License 2.0；[jsoup](https://jsoup.org/)：采用 MIT License。

本仓库的 [MIT License](LICENSE) 适用于 Been 自有及改编的项目代码。第三方软件、在线服务、地图数据、模型和传递依赖仍分别受各自许可证与服务条款约束。
