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
| Import from phone (pairing) | Import | Planned (M2) | — |
| Import from clipboard / file / manual | Import | In progress (M1) | `ACTION_SEND` and `roamtv://` text import seed the profile store; the Import screen is still a placeholder |
| Share QR, export configuration | Home / Share | Planned (M2) | — |
| Server editor: VMess, VLESS, SS, SOCKS, HTTP, Trojan, WireGuard, Hysteria2, custom | Server editor | Planned (M3) | — |
| Policy groups and proxy chains | Group / chain editor | Planned (M3) | — |
| Subscriptions and subscription editor | Subscriptions | Planned (M4) | — |
| Routing rules and rule editor | Routing | Planned (M4) | — |
| Per-app proxy and app picker | Per-app | Planned (M4) | — |
| User assets (GeoIP/GeoSite/SRS/URLs) | Assets | Planned (M4) | — |
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
