# v2rayNG upstream tracking

RoamTV is an independent Android TV derivative of
[v2rayNG](https://github.com/2dust/v2rayNG). This document records the upstream
baseline, the feature-porting policy, and the recurring review procedure.

## Baseline

| Item | Value |
|---|---|
| Upstream project | `2dust/v2rayNG` |
| Stable baseline | `2.3.15` |
| Baseline commit | `57907cf354d9769200ed4717b4430d605abe0952` |
| Baseline date | 2026-11-01 |
| Last reviewed | 2026-11-27 |
| Reviewed pre-release | `2.4.3` (2026-11-27) |
| Xray core in RoamTV | `26.11.9` (AndroidLibXrayLite `v26.11.9`) |

RoamTV is not a general-purpose phone client. The TV screens, navigation,
focus model and layout are the frozen v1.14 baseline. Upstream UI refactors are
not ported. Protocol, transport, import/export, routing, DNS, subscription and
security behavior is ported through the existing screens.

## Porting policy

1. Port protocol and transport support, import/export formats, subscription
   behavior, routing/DNS behavior, and security/stability fixes.
2. Keep the v1.14 layout, navigation, focus and operation semantics unchanged.
3. Add new protocol types/fields through the existing server editor, type
   picker and dialogs. Do not add a new screen for a protocol field.
4. Do not port phone-only or TV-launcher UI work, floating pill tabs, snackbars,
   dynamic layout changes, or upstream's dedicated TV main/settings screens.
5. Every port needs JVM tests for parsers/builders and a TV/emulator check.
6. Update `NOTICE` and `docs/COMPLIANCE.md` when the upstream baseline changes.

## Feature matrix

Status: **Ported** means code and a named check exist in this repository;
**Present** means the feature already existed before the upstream review;
**Not applicable** means a phone/TV-launcher-only feature; **Pending** means a
known gap.

| Area | Upstream feature | Status | Notes |
|---|---|---|---|
| Transport | xdrive | Ported (v1.18) | Service, remote folder, secrets, extra JSON; Mux forced; VLESS flow dropped; no xdrive share-link parameters |
| Protocol | MASQUE (+WARP) | Ported (v1.17) | `masque://`, WARP keys, local address, remote DNS, headers, ALPN h2/h3 |
| Protocol | Xray 26.11.9 MASQUE transport | Present | AndroidLibXrayLite `v26.11.9` |
| Protocol | VLESS, VMess, Shadowsocks, Trojan, WireGuard, SOCKS, HTTP | Present | Existing core and editor |
| Protocol | Hysteria2/TUIC | Pending upstream core | Editor/share parsing only; no Hysteria2/TUIC outbound in Xray |
| Import | Custom outbound JSON | Ported (v1.17) | `CUSTOM_OUTBOUND`, raw outbound injected by the config builder |
| Import | Full custom client JSON | Present | `CUSTOM` path |
| Import | `v2rayn://` internal share | Present | Unsupported entries now skip instead of failing the batch |
| Import | Ruleset from network URL in clipboard/QR | Ported (v1.17) | Existing clipboard import detects HTTP(S) and fetches the JSON |
| Subscription | Custom HTTP headers | Present | Per-subscription JSON headers |
| Subscription | Header leak across redirects | Fixed (v1.17) | Custom headers and embedded credentials only on the original origin |
| Subscription | Auto-test/delete-invalid/sort after update | Present | Existing settings and update pipeline |
| Subscription | Usage info (traffic/expiry/support link) | Data present; UI pending | Upstream's info box is UI-only and is not ported; data is parsed/stored |
| Diagnostics | TCP ping with concurrency | Present | `onlyTcp` path and `PREF_REAL_PING_CONCURRENCY` |
| Transport | WireGuard remoteDNS | Present | Existing WireGuard builder |
| Security | HTTPS→HTTP redirect blocked | Ported (v1.17) | `HttpUtil.getUrlContentWithUserAgent` |
| Fix | Proxy chain member remarks containing commas | Ported (v1.18) | Added `proxyChainProfileList`; legacy comma string migrated once |
| Fix | Routing rule IDs unique after import | Ported (v1.18) | Blank/duplicate IDs regenerated; order preserved |
| Fix | Imported profiles found in Default group | Pending | Needs import destination review |
| Fix | Hy2 global port hopping interval | Not applicable | Hysteria2 has no outbound in the bundled Xray core |
| DNS | Validate region, parallel query, serve stale | Pending | Behavior-changing; needs a dedicated DNS test matrix |
| Subscription | Traffic info and single-subscription update | Pending/UI | UI-only upstream change; data is parsed but not displayed |
| Fix | Root hotspot DNS when LAN sharing is off | Pending | Needs root-mode audit |
| UI | Monet dynamic colour | Ported (v1.17) | Settings switch; Android 12+ system palette; default brand palette otherwise |
| UI | Phone Compose migration, TV main/settings screens, pill tabs, snackbars | Not applicable | Frozen TV baseline |
| Phone | Camera QR scan, widgets, phone-only navigation | Not applicable | TV has no camera and uses the phone pairing flow |

## Review procedure

1. Check `https://api.github.com/repos/2dust/v2rayNG/releases/latest` and the
   tag list. Pre-releases are reviewed but are not treated as the stable
   baseline without an explicit decision.
2. Compare the release notes and commits since the last reviewed commit.
3. Classify each change as protocol/feature, security/fix, UI-only, or
   phone-only.
4. Port protocol/feature/security items through the existing TV screens; add
   JVM tests and a TV/emulator check.
5. Update this matrix, `NOTICE` and `docs/COMPLIANCE.md` when needed.
6. Publish a patch release; never replace a published APK in place.

## Recurring review

A weekly agent task checks the upstream stable and pre-release tags, compares
them with this document, and reports new gaps. It must not modify app code or
UI without approval.
