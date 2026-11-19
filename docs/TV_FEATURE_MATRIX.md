# RoamTV TV feature matrix

Status values: **Verified** means the named check ran on this branch; **In
progress** means code exists but the acceptance check has not run; **Planned**
means not implemented. Do not promote a row to Verified without the check.

| Feature | TV destination | Status | Verification |
|---|---|---|---|
| Single-Activity TV shell, rail, overscan, theme | `ui/MainActivity`, `TvApp` | Verified (M0) | `assemblePlaystoreDebug`, lint, unit tests, API 37 TV-sized emulator launch, D-pad focus screenshot |
| Typed back stack + Back behaviour | `ui/navigation/TvNavigator` | Verified (M0, v1.10 AVD) | `TvNavigatorTest` JVM tests cover details, rail roots and Home exit; v1.10 AVD: Back from a rail destination returns Home and Back at Home backgrounds the app. Physical TV Back check for v1.10 not run |
| Home server list, groups, selection | Home | Verified (M1, v1.10 AVD) | API 37 TV-sized AVD: server list, group tabs, D-pad focus and the 720p split. v1.10 AVD: moving the list cursor only previews a node; OK moves focus to the action pane and does not reconnect. Physical TV not run for v1.10 |
| Home action pane, import/add, batch and live speed | Home | In progress (v1.10) | v1.10 AVD 1920x1080: header shows Import/Add node/Batch, right pane shows Connect/Test/Edit/Share/Delete (update subscription for subscription nodes), top band shows `Connected · node` and `Proxy ↑/↓` when enabled; the speed setting is UI-only and did not reload the core. Non-zero live speed values, the Batch dialog, 720p/4K and physical TV not run |
| Focus ring clipping and rail focus restore | Rail / lists | Verified (v1.10 AVD) | v1.10 AVD: the first node ring top is fully visible; Left from Settings lands on the Settings rail item instead of Routing. 720p, 4K and physical TV not run |
| Connect / disconnect / restart, VPN consent | Home | Verified (M1, VPN mode) | Local SOCKS profile reached Connected, tun0 present, Disconnect visible; physical Sony BRAVIA (Android 12/API 31, `armeabi-v7a`) connected and disconnected with tun0 present/removed; proxy-only/root modes not run |
| Latency / real-ping / test-all / cancel | Home | In progress (M1) | Single real-ping exercised; environment could not reach the default test URL. Test-all/cancel not run |
| Search, delete, sort, group management | Home | In progress (M1) | Search and More dialogs open with D-pad; delete/sort/group flows not yet exercised end-to-end |
| Import from phone (pairing) | Import | In progress (v1.10) | API 37 AVD + desktop Chromium previously imported `FromPhoneE2E` over an adb-forwarded port. v1.10 keeps the QR session open after each send so several configs can be imported without rescanning; the QR dialog shows the imported count and a Done button. A physical phone multi-import run and the new dialog are not run |
| Import from clipboard / file / text | Import | In progress (M2) | Import hub and text dialog implemented; clipboard/file end-to-end not yet exercised |
| Pairing crypto and HTTP protocol | Pairing | Verified (M2, v1.10) | JVM tests: SJCL-to-Java AES-GCM vector, tamper rejection, page/JS GET, wrong-session 403, and two sequential encrypted uploads on one session. The unused verification code was removed. A physical phone multi-import run is not run |
| Share QR | Home / More | Verified (M2, UI) | QR dialog opened from More with the selected server; scan from a phone not run |
| Server editor: VMess, VLESS, SS, SOCKS, HTTP, Trojan, WireGuard, Hysteria2, custom | Server editor | In progress (M3) | API 37 AVD: add VLESS, save, edit the selected SOCKS server, save and restart, list refresh; validation and field mapping covered by JVM tests. Other protocols not individually exercised |
| Policy groups and proxy chains | Group / chain editor | In progress (M3) | API 37 AVD: add a policy group and a two-member proxy chain, save, list refresh; chain runtime connection not run |
| Subscriptions and subscription editor | Subscriptions | In progress (M4) | API 37 AVD: list, empty state, add/update actions and the editor form render with D-pad focus; save/delete/update and URL validation not exercised end-to-end |
| Routing rules and rule editor | Routing | In progress (M4b) | API 37 AVD: list, domain-strategy change, add rule, save and list refresh verified; rule deletion, preset/clipboard import/export not run |
| Per-app proxy and app picker | Per-app | In progress (M4c) | API 37 AVD: app list, per-app/bypass switches, app toggle and bulk-action dialog verified; search, auto-select, import/export not run |
| User assets (GeoIP/GeoSite/SRS/URLs) | Assets | In progress (M4d) | API 37 AVD: list with built-in sizes/dates, geo source change and persistence, URL add/edit/delete, validation, SAF local-file import and delete, download-all with updated metadata verified; pairing handoff for a URL not run |
| Language: follow system + 10 locales | Settings | Verified (M1) | API 37 AVD: English → 简体中文 → Follow system; Settings route and language-row focus restored after recreation |
| Settings | Settings | In progress (M5, v1.10 AVD) | API 37 AVD: full group list, bypass-LAN dropdown, VPN MTU text edit, observatory invalid-duration toast, VPN/proxy-only dependency dimming, language switch and System VPN settings intent verified. Physical Sony BRAVIA on v1.03: leaving Settings reloaded the core in place and a UI-only switch did not reload. v1.10 AVD: Left from Settings returns to the Settings rail item; "Show speed on Home" is a UI-only setting. v1.10 physical TV and root-mode toggles not run |
| Local backup / restore and WebDAV | Backup | In progress (M5) | API 37 AVD: local backup written to Downloads through SAF, restore of that zip succeeded, WebDAV editor renders and disables Save without a URL; WebDAV upload/download, share target and activity recreation not run |
| Logcat, About / legal, update check | Logs / About / Update | In progress (M5/M6) | API 37 AVD: log list, search dialog, license text, translators, version info and no-update check verified; update selection now picks the highest stable or pre-release version and is covered by `UpdateCheckerManagerTest`. Physical Sony BRAVIA on v1.03: Logcat streamed live entries, About showed v1.03/Lib v40/Xray-core v26.10.31 and the legal entries, and the update check reached GitHub through the active connection and selected v1.03 (same as installed, no update); log copy/clear, new-version dialog and browser download not run |
| 720p / 1080p / 4K, RTL, 1.3x font, TalkBack | All | In progress (M6) | API 36 TV AVD: 1280x720, 1920x1080 and 3840x2160 Home/Assets/Settings checked; 720p Arabic RTL and 1.3x spot checks pass; TalkBack and full traversal not run |
| Android 8 (API 26) compatibility | All | Verified (M6, partial) | API 26 TV AVD: launch, rail, Settings, Update, More dialog, Routing editor and Assets checked; full feature suite not run |
| GPL legal notices and source archive | About / Releases | Verified (M6) | About → Legal notices includes GPLv3, NOTICE and SJCL; every release carries `RoamTV-<tag>-source.zip` with submodule sources and native binaries |
| Keyboard / remote-only traversal on every screen | All | In progress (M6) | Every implemented screen has D-pad paths; full screen-by-screen sweep and keyboard/TalkBack traversal not run |

## Platform capability notes

| Phone integration | TV decision |
|---|---|
| Camera QR scanner | Removed from the TV app; replaced by phone pairing |
| Home-screen widget | Removed; TV launchers do not host widgets |
| Quick Settings tile | Removed; TV has no Quick Settings shade |
| Launcher shortcuts | Intents are handled by `MainActivity`; TV launchers may not display shortcuts |
| Tasker `EDIT_SETTING` | Routed to `MainActivity`; configuration UI is a later milestone |
| `roamtv://` and `ACTION_SEND` | Handled by `MainActivity` |
| Light/dark and dynamic colour | Removed; fixed dark TV theme |
| Double-column toggle | Replaced by the TV master-detail layout |
