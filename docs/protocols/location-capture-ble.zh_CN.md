<p align="right">
  <strong>简体中文</strong> · <a href="location-capture-ble.md">English</a>
</p>

# AI Passport 手机定位记录 BLE 协议（v1）

> 当前固件与 Android App 使用 v3，v1 报文不再用于当前功能。请参阅[配套 App BLE v3 协议](passport-companion-ble.zh_CN.md)。

AI Passport 作为 BLE 外设广播；手机 App 作为中心设备连接。定位由手机系统提供，手机 App 先保存定位记录，再把结果回传。AI Passport 不提供 Swarm 功能。

## GATT 服务

按广播中的服务 UUID 扫描；完整设备名 `AI Passport` 放在扫描响应中，手机需使用主动扫描才能显示名称。UUID 字符串按常见大端文本格式表示：

| 用途 | UUID | 属性 |
| --- | --- | --- |
| 定位记录服务 | `01504941-6E8C-214A-10A9-415000000001` | Primary service |
| 设备请求特征 | `02504941-6E8C-214A-10A9-415000000001` | Notify |
| 手机结果特征 | `03504941-6E8C-214A-10A9-415000000001` | Write / Write Without Response |

手机连接后必须订阅“设备请求特征”的通知。设备只在订阅成功后发送请求。整数均为 little-endian；坐标采用 WGS-84 十进制度乘以 `10^7` 的有符号整数。

## 设备请求

设备确认键短按后发送 4 字节：

| 字节 | 内容 |
| --- | --- |
| 0 | 协议版本 `0x01` |
| 1 | 定位记录请求 `0x01` |
| 2–3 | Request ID，`uint16` |

手机 App 应请求系统定位权限与当前定位，使用坐标、精度、UTC 时间和当时 UTC 偏移保存一条记录。记录持久化成功后再回传成功帧；用户拒绝权限、定位失败或写入失败时回传失败帧。设备等待 30 秒，等待时显示循环动画；匹配 Request ID 的成功响应会触发成功动画。超时、断开或失败响应显示失败状态。

## 手机结果

成功响应为 20 字节，可在默认 BLE ATT MTU 下直接传输，不需分包：

| 字节 | 内容 |
| --- | --- |
| 0 | 协议版本 `0x01` |
| 1 | 结果 `0x81` |
| 2 | 成功状态 `0x00` |
| 3–4 | 对应 Request ID，`uint16` |
| 5–8 | Unix UTC 秒，`uint32` |
| 9–12 | 纬度，`int32`，WGS-84 E7 |
| 13–16 | 经度，`int32`，WGS-84 E7 |
| 17 | 水平精度米，`uint8`（超过 255 米时按 255 发送） |
| 18–19 | 当地 UTC 偏移分钟，`int16` |

失败响应为 6 字节：`version(0x01), opcode(0x81), nonzero status, request_id(uint16), error_code(uint8)`。失败状态码由手机 App 定义；设备当前统一显示“PHONE COULD NOT SAVE”。设备校验时间、坐标、精度、时区和 Request ID，只接纳当前等待中的请求。

设备收到成功结果后，会把时间和坐标写入本机 NVS 记录，并把设备系统时钟校准到手机返回的 UTC 时间。场所名称由移动端地图/地点服务继续解析，不通过这个 BLE 帧传输。
