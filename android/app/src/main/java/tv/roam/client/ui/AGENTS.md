# TV UI agent guide

These rules apply to `android/app/src/main/java/tv/roam/client/ui/` and
`android/app/src/test/java/tv/roam/client/ui/`. Follow the repository-root
`AGENTS.md` as well; a scoped rule overrides a root rule only when the two
require incompatible actions.

## UI stack

- Every screen is Compose for TV. Use `androidx.tv.material3` for the theme,
  surfaces, buttons, cards, icons and switches, and `androidx.tv.foundation` for
  TV-specific text/IME helpers. Do not add `androidx.compose.material3`,
  Leanback, Fragments, XML screen layouts, ViewBinding, or RecyclerView.
- There is one Activity (`ui/MainActivity.kt`). A screen is a `TvRoute`
  destination rendered by `TvApp`; do not add another Activity without an
  explicit task requirement. Services, receivers and providers are not screens.
- Screens own presentation only. Durable state, validation, persistence, network
  and native work belong to `feature/` ViewModels or the existing `handler/`,
  `core/` and `service/` owners. Composables must not read or write MMKV,
  `SettingsManager`, repositories, or files directly.
- Collect state with `collectAsStateWithLifecycle()` and send user operations
  through the feature's action contract. Use `rememberSaveable` only for
  transient presentation state that must survive recreation.

## Remote control and focus

- Every action must be reachable with D-pad direction keys, OK, Back and the
  optional Menu key. A touch-only or pointer-only path is a bug. Long-press is
  an enhancement, never the only way to reach an action.
- Give every interactive element a visible focus state that is not colour-only:
  the theme uses a bright surface, a 3dp border and a small scale change. Keep
  the focused element inside the overscan safe area and bring it into view.
- Use stable domain identifiers (server GUIDs, group IDs, subscription IDs,
  ruleset IDs, package names) for keys and saved focus. Never use a list index,
  adapter position, or paging offset as identity.
- After a refresh, import, deletion, sort or subscription replacement, resolve
  the focused item again by its domain ID. If it disappeared, move focus once to
  the final neighbouring item; do not focus an intermediate item.
- Dialogs must trap focus and restore it to the invoking element on dismissal.
  Destructive confirmations start focus on Cancel.
- Back walks the route stack first. On the root route it backgrounds the app; it
  must not stop the VPN service or leave a half-open dialog.
- Top-level destinations keep the left rail. Secondary screens replace the rail
  with a back top bar and a title; do not mix the two patterns on one screen.

## Ten-foot layout

- Use the `TvOverscan`/`TvRailLayout` components rather than raw fixed padding.
  Keep content inside the 5% safe area at 720p, 1080p and 4K.
- Body text is at least 16sp; primary rows and buttons are at least 52dp high.
  Do not use thin font weights or low-contrast grey for essential information.
- The theme is fixed dark. Do not add light/dark switching, dynamic colour, or a
  phone-style bottom navigation bar.
- Layouts must survive a 1.3x font scale, RTL locales (ar/fa/bqi), and a short
  content area. Scroll the content, not the key-hint bar.
- The bottom key-hint bar is part of the screen contract: it tells the viewer
  which remote actions exist. Keep it localized and visible.

## Text input

- Prefer a field-row that opens a large edit dialog over an inline tiny text
  field. Short values use the system IME and numeric steppers.
- Every long text field (subscription URL, UUID, JSON, routing list) must offer
  a "send from phone" path through the pairing flow. A television without a
  keyboard must still be able to import and edit every value.
- Forms apply `imePadding()`, keep the focused field above the keyboard, and
  restore focus to the field row after the IME closes.

## Import and pairing

- The TV has no camera. Do not re-add CameraX or a scanner Activity. QR import
  is performed by the phone: the TV shows a QR, the phone opens the local page,
  and the payload is encrypted before it leaves the browser.
- Pairing sessions are one-time, short-lived, LAN-bound, rate-limited and never
  persisted. Never log a session token, pairing key, configuration body, or
  proxy URL. Require `ACCESS_LOCAL_NETWORK` before opening the socket.
- The pairing page must be self-contained (no CDN), validate its input, and
  work in a normal mobile browser over the local network.

## Validation

Apply the repository-root validation matrix and, additionally:

- JVM tests for navigation stack, settings mapping, editor validation, pairing
  crypto/session logic, filtering and selection.
- Compose UI tests for focus traversal, dialog focus traps, deletion focus
  restoration, RTL and back-stack behaviour.
- Emulator/device check with `adb shell input keyevent` for every new screen,
  plus a `uiautomator dump` of focused nodes and screenshots at 720p and 1080p.
- A screenshot verifies appearance only. It does not verify focus, activation,
  state ownership, persistence or recreation. Record any check that did not run
  as `Not run`; never describe it as verified.
