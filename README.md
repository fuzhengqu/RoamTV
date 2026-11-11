# RoamTV

A remote-first network client built for Android TV. RoamTV is operated entirely
with a TV remote: the interface is designed for D-pad navigation at a 10-foot
viewing distance, with an overscan-safe layout and explicit focus states.

[![API](https://img.shields.io/badge/API-26%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/oreo)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg)](https://kotlinlang.org)
[![GitHub Releases](https://img.shields.io/github/downloads/fuzhengqu/RoamTV/latest/total?logo=github)](https://github.com/fuzhengqu/RoamTV/releases)

---

## Download / 下载

Download the latest release here / 在这里下载最新版本：

<https://github.com/fuzhengqu/RoamTV/releases>

---

## Remote controls / 遥控器操作

- Directional pad: move focus. **OK**: select. **Back**: return one level.
- **Menu** (or the on-screen More button): open actions for the focused item.
- Play/Pause is reserved for quick connect/disconnect on the home screen.
- 方向键移动焦点，**OK** 选择，**返回** 回上一级；**菜单键** 或页面上的“更多”
  按钮打开当前项的操作。首页支持播放/暂停键快速连接或断开。

## Features / 功能

- Connect / disconnect, server profiles and groups, subscriptions
- Routing rules, DNS, per-app proxy, user assets
- Connection testing, logs, configuration backup and restore
- Settings, update check, open-source licenses
- Config import without a camera: clipboard, a pairing-code LAN push from a
  phone, document picker, and a step-by-step wizard
- 连接/断开、节点与分组、订阅；路由、DNS、分应用代理、资源文件
- 测速、日志、配置备份与恢复；设置、更新检查、开源许可
- 无需摄像头的配置导入：剪贴板、手机扫码后经配对码局域网推送、
  文件选择器、分步向导

## Geoip and Geosite / Geoip 与 Geosite

- `geoip.dat` and `geosite.dat` are stored under
  `Android/data/tv.roam.client/files/assets` (the path may differ on some devices).
- Rule data can be imported manually; third-party rule files are supported.
- `geoip.dat` 和 `geosite.dat` 位于
  `Android/data/tv.roam.client/files/assets`（部分设备路径可能不同）。
- 规则数据可手动导入，亦支持第三方规则文件。

---

## Development guide / 开发指南

- The Android project is in the `android/` Gradle directory. Open it in Android
  Studio or use the Gradle wrapper.
- All build tools (a project-local JDK, Gradle cache, SDK platform, AVD) live
  inside this repository under `.tooling/`; no system-level environment variable
  is changed.
- Full toolchain versions, native build steps and release reproduction are in
  [docs/COMPLIANCE.md](docs/COMPLIANCE.md).
- Android 工程位于 `android/` Gradle 目录，可用 Android Studio 或 Gradle wrapper 构建。
- 全部构建工具（项目内 JDK、Gradle 缓存、SDK 平台、AVD）位于仓库内 `.tooling/`，
  不修改任何系统级环境变量。
- 工具链版本、原生库构建步骤与发布复现方法见
  [docs/COMPLIANCE.md](docs/COMPLIANCE.md)。

---

## Privacy / 隐私

RoamTV has no analytics, advertising, or telemetry. See [PRIVACY.md](PRIVACY.md)
for what is stored on the device and which endpoints are contacted.
RoamTV 不包含分析、广告或遥测；设备本地数据与网络端点说明见
[PRIVACY.md](PRIVACY.md)。

---

## License / 许可证

RoamTV is free software licensed under the **GNU General Public License,
version 3**. See [LICENSE](LICENSE), [NOTICE](NOTICE) and
[docs/COMPLIANCE.md](docs/COMPLIANCE.md) for the modification notice, the
upstream attribution, third-party license texts and the Corresponding Source
offer.

RoamTV 是依据 **GNU 通用公共许可证第 3 版** 授权的自由软件。修改声明、
上游来源标注、第三方许可文本与对应源码提供方式见
[LICENSE](LICENSE)、[NOTICE](NOTICE) 与 [docs/COMPLIANCE.md](docs/COMPLIANCE.md)。

There is no warranty. This project is an independent derivative; see `NOTICE`
for its provenance. The upstream authors are not affiliated with and do not
endorse these modifications.

本项目不提供担保，且为独立衍生作品；来源关系见 `NOTICE`。
上游作者与本修改无关，亦未为本修改背书。
