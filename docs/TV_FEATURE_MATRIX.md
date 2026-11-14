# RoamTV TV feature matrix

Status values: **Verified** means the named check ran on this branch; **In
progress** means code exists but the acceptance check has not run; **Planned**
means not implemented. Do not promote a row to Verified without the check.

| Feature | TV destination | Status | Verification |
|---|---|---|---|
| Single-Activity TV shell, rail, overscan, theme | `ui/MainActivity`, `TvApp` | Verified (M0) | `assemblePlaystoreDebug`, lint, unit tests, API 37 TV-sized emulator launch, D-pad focus screenshot |
| Typed back stack + Back behaviour | `ui/navigation/TvNavigator` | Verified (M0) | `TvNavigatorTest` JVM tests; emulator Back from Import returned Home |
| Home server list, groups, selection | Home | Verified (M1) | API 37 TV-sized AVD: server list, selected row, detail pane, D-pad focus; 720p fallback |
| Connect / disconnect / restart, VPN consent | Home | Verified (M1, VPN mode) | Local SOCKS profile reached Connected, tun0 present, Disconnect visible; proxy-only/root modes not run |
| Latency / real-ping / test-all / cancel | Home | In progress (M1) | Single real-ping exercised; environment could not reach the default test URL. Test-all/cancel not run |
| Search, delete, sort, group management | Home | In progress (M1) | Search and More dialogs open with D-pad; delete/sort/group flows not yet exercised end-to-end |
| Import from phone (pairing) | Import | Verified (M2, browser) | API 37 AVD + desktop Chromium over an adb-forwarded debug port: QR page loaded, SJCL AES-GCM POST accepted, `FromPhoneE2E` imported into the TV list; a physical phone was not used |
| Import from clipboard / file / text | Import | In progress (M2) | Import hub and text dialog implemented; clipboard/file end-to-end not yet exercised |
| Pairing crypto and HTTP protocol | Pairing | Verified (M2) | JVM tests: SJCL-to-Java AES-GCM vector, tamper rejection, page/JS GET, wrong-session 403, import callback |
| Share QR | Home / More | Verified (M2, UI) | QR dialog opened from More with the selected server; scan from a phone not run |
| Server editor: VMess, VLESS, SS, SOCKS, HTTP, Trojan, WireGuard, Hysteria2, custom | Server editor | In progress (M3) | API 37 AVD: add VLESS, save, edit the selected SOCKS server, save and restart, list refresh; validation and field mapping covered by JVM tests. Other protocols not individually exercised |
| Policy groups and proxy chains | Group / chain editor | In progress (M3) | API 37 AVD: add a policy group and a two-member proxy chain, save, list refresh; chain runtime connection not run |
| Subscriptions and subscription editor | Subscriptions | In progress (M4) | API 37 AVD: list, empty state, add/update actions and the editor form render with D-pad focus; save/delete/update and URL validation not exercised end-to-end |
| Routing rules and rule editor | Routing | In progress (M4b) | API 37 AVD: list, domain-strategy change, add rule, save and list refresh verified; rule deletion, preset/clipboard import/export not run |
| Per-app proxy and app picker | Per-app | In progress (M4c) | API 37 AVD: app list, per-app/bypass switches, app toggle and bulk-action dialog verified; search, auto-select, import/export not run |
| User assets (GeoIP/GeoSite/SRS/URLs) | Assets | Planned (M4) | — |
| Language: follow system + 10 locales | Settings | Verified (M1) | API 37 AVD: English → 简体中文 → Follow system; Settings route and language-row focus restored after recreation |
| Settings (VPN, core, Mux, Fragment, Observatory, advanced, mode) | Settings | Planned (M5) | — |
| Local backup / restore and WebDAV | Backup | Planned (M5) | — |
| Logcat, About / legal, update check | Logs / About / Update | Planned (M5) | — |
| 720p / 1080p / 4K, RTL, 1.3x font, TalkBack | All | Planned (M6) | — |
| Android 8 (API 26) compatibility | All | Planned (M6) | — |
| Keyboard / remote-only traversal on every screen | All | Planned (M6) | — |

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
