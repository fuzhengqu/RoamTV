# RoamTV

![RoamTV](docs/assets/roamtv-banner.png)

RoamTV is an Xray/V2Ray proxy client for **Android TV and Google TV**. It
connects to a server or subscription you already have and is operated entirely
with a TV remote: D-pad for focus, OK to select, Back to return one level, and
Menu (or the on-screen More button) for the actions of the focused item.

RoamTV does not provide proxy servers, accounts, or a VPN service. You need
your own server or subscription from a provider you trust.

![RoamTV Home screen](docs/assets/ui/home.png)

[![API](https://img.shields.io/badge/API-26%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/oreo)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg)](https://kotlinlang.org)
[![GitHub Releases](https://img.shields.io/github/downloads/fuzhengqu/RoamTV/latest/total?logo=github)](https://github.com/fuzhengqu/RoamTV/releases)

## Who it is for / 适用对象

- Android TV, Google TV, or an Android TV box running Android 8.0 (API 26) or
  newer.
- Users who already have a VLESS, VMess, Shadowsocks, Trojan, WireGuard, SOCKS,
  or HTTP proxy server, or an Xray/V2Ray subscription.
- Households that want to manage the connection from the couch, without a
  phone-style touch UI or a separate remote app.

RoamTV 面向 Android TV、Google TV 和 Android TV 盒子（Android 8.0 / API 26
及以上）。使用者需要自备 VLESS、VMess、Shadowsocks、Trojan、WireGuard、
SOCKS 或 HTTP 服务器，或已有的 Xray/V2Ray 订阅。应用不提供节点、账号或
VPN 服务。

## Features / 功能

**Television-first UI**

- One screen per task, 5% overscan-safe layout, visible focus rings, and a
  bottom key-hint bar.
- Every action is reachable with the D-pad, OK, Back, and the Menu key.
- Play/Pause connects or disconnects on the home screen.
- The Home action pane keeps Connect/Disconnect, Test, Edit, QR, subscription
  update, and Delete one click away.

**Servers and subscriptions**

- Manual server profiles and groups, including import from clipboard, a local
  file, a QR code shown by the app, or a full JSON configuration.
- Subscription groups with update, rename, and delete.
- Server editor for VMess, VLESS, Shadowsocks, SOCKS, HTTP, Trojan, WireGuard,
  Hysteria2, and custom configurations. See [Limitations](#limitations--限制)
  for the protocols the bundled core can actually carry.
- Search, sort, latency test, and live proxy speed.

**Routing and DNS**

- Routing rules with domain, IP, port, process, and inbound tags.
- Domain strategy, DNS servers, and DNS routing.
- Per-app proxy selection.
- `geoip.dat` and `geosite.dat` live in the app-private `app_assets` directory.
  The Asset files screen shows size and date and can replace them from a URL, a
  local file, or Download all.

**Connection modes**

- VPN mode: system-wide TUN through Xray and hev-socks5-tunnel.
- Proxy-only mode: a local SOCKS/HTTP proxy without a system VPN.
- Root mode: TUN through the bundled `libhevsockstun.so` binary.

**Import without a camera**

- The TV shows a QR code; the phone opens a local pairing page.
- The phone sends the configuration over the LAN, encrypted with a one-time
  session key and pairing code.
- No camera, no account, and no cloud relay are involved.

**Operations**

- Connection logs with level selection and search.
- Configuration backup and restore through the system document picker.
- Update check, open-source licenses, and an About screen with the bundled
  Xray and hev-socks5-tunnel versions.

**TV 端功能**

- 每个任务一个页面，5% 过扫描安全边距，清晰的焦点环和底部按键提示。
- 所有操作均可用方向键、OK、返回和菜单键完成；首页支持播放/暂停键快速连接。
- 节点与分组、订阅更新、搜索、排序、延时测试和实时代理速度。
- 路由规则、DNS、分应用代理、`geoip.dat` / `geosite.dat` 资源管理。
- VPN、仅代理、Root 三种模式。
- 无需摄像头的导入：剪贴板、本地文件、TV 显示二维码后由手机通过配对码
  在局域网内加密推送，以及完整 JSON 配置。
- 日志、配置备份与恢复、更新检查、开源许可。

## Protocols and ABIs / 协议与 ABI

The bundled **Xray-core** carries:

- VLESS (including Reality and XTLS flows)
- VMess
- Shadowsocks
- Trojan
- WireGuard
- SOCKS and HTTP (outbound)

Hysteria2 fields and share-link import exist in the editor for compatibility
with imported configurations, but the bundled Xray core does not provide a
Hysteria2 outbound. Use one of the protocols above for a working connection.

Release APKs are built for:

| ABI | Typical device |
|---|---|
| `armeabi-v7a` | Older 32-bit TVs and TV boxes |
| `arm64-v8a` | Most current TVs and boxes |
| `x86` / `x86_64` | Emulators and Intel-based boxes |
| `universal` | Any device; larger download |

内置 **Xray-core** 支持 VLESS（含 Reality / XTLS）、VMess、Shadowsocks、
Trojan、WireGuard、SOCKS 和 HTTP 出站。编辑器和分享链接解析保留了
Hysteria2 字段，但内置 Xray 核心没有 Hysteria2 出站；实际连接请使用上述
协议。

## Install / 安装

1. Open [Releases](https://github.com/fuzhengqu/RoamTV/releases) and download
   the APK matching your device ABI. `arm64-v8a` is the usual choice for a
   current TV; `armeabi-v7a` is for older 32-bit devices.
2. Allow installation from the file manager or browser you use, then open the
   APK on the TV.
3. Or sideload with ADB:

   ```sh
   adb install -r RoamTV_<version>_arm64-v8a.apk
   ```

4. Start RoamTV, import a server or subscription, select a node, and press
   Connect. The first VPN connection asks for Android's VPN consent.

Each release also publishes a `.sig` file for every APK and
`RoamTV-public-key.asc`. Verify the signature before installing a build you did
not compile yourself.

1. 在 [Releases](https://github.com/fuzhengqu/RoamTV/releases) 下载与设备 ABI
   匹配的 APK。常见电视用 `arm64-v8a`，较老的 32 位设备用 `armeabi-v7a`。
2. 允许文件管理器或浏览器安装应用，然后在电视上打开 APK。
3. 也可以使用 ADB 侧载：`adb install -r RoamTV_<版本>_arm64-v8a.apk`。
4. 打开应用，导入节点或订阅，选择节点并连接。首次使用 VPN 模式时，Android
   会显示 VPN 授权确认。

每个 APK 都附带 `.sig` 签名文件，发布页还提供 `RoamTV-public-key.asc`。
安装非自行编译的版本前，建议先校验签名。

## Import from a phone / 手机导入

1. On the TV, open Import and choose the phone/pairing option.
2. The TV shows a QR code and a short pairing code.
3. Scan the QR code with the phone, enter the pairing code, and paste the
   configuration or subscription URL.
4. The phone encrypts the payload and sends it to the TV over the local network.
   The pairing session expires after a short time.

The TV never opens a camera. The phone page is served by the TV and does not
load third-party scripts.

## Privacy / 隐私

RoamTV has no analytics, advertising, or telemetry. It stores profiles,
settings, and logs on the device. Network requests go to the servers,
subscriptions, update endpoint, and asset URLs that you configure. See
[PRIVACY.md](PRIVACY.md) for the exact data and endpoints.

RoamTV 不包含分析、广告或遥测。节点、设置和日志保存在设备本地；网络请求只
发往你配置的服务器、订阅、更新地址和资源地址。具体数据与端点见
[PRIVACY.md](PRIVACY.md)。

## Limitations / 限制

- No servers, accounts, or subscription service is included.
- Hysteria2 profiles can be stored and edited, but the bundled Xray core cannot
  connect with them. TUIC is not implemented.
- Some streaming services block proxy/VPN traffic regardless of the client.
- The interface is designed for a television. It runs on phones and tablets but
  is not a touch-first phone client.
- Root mode depends on the device's root implementation and is not available on
  stock TVs.
- Live testing has covered VLESS on a physical Sony BRAVIA and emulator runs for
  UI, import, routing, backup, and settings. Other protocols and servers have
  unit-test coverage but not a live server in the release record.

## Development / 开发

- Android project: `android/` (Kotlin, Gradle Kotlin DSL, Compose for TV).
- Toolchain, native build steps, release reproduction:
  [docs/COMPLIANCE.md](docs/COMPLIANCE.md).
- Native core versions and update procedure: [docs/NATIVE_CORES.md](docs/NATIVE_CORES.md).
- Frozen TV layout: [docs/UI_BASELINE.md](docs/UI_BASELINE.md).
- All build tools live under the repository's `.tooling/` directory; no system
  environment variable is changed.

## License / 许可证

RoamTV is free software under the **GNU General Public License, version 3**.
See [LICENSE](LICENSE), [NOTICE](NOTICE), and
[docs/COMPLIANCE.md](docs/COMPLIANCE.md) for the modification notice, upstream
attribution, third-party license texts, and the Corresponding Source offer.

RoamTV 是依据 **GNU 通用公共许可证第 3 版** 授权的自由软件。修改声明、
上游来源标注、第三方许可文本与对应源码提供方式见 [LICENSE](LICENSE)、
[NOTICE](NOTICE) 与 [docs/COMPLIANCE.md](docs/COMPLIANCE.md)。

This project is an independent derivative. The upstream authors are not
affiliated with and do not endorse these modifications. There is no warranty.
