# RoamTV architecture

## Shape of the app

RoamTV is a single-Activity Android TV application. `ui/MainActivity` only owns
the window and the Compose root; every screen is a destination in an
app-owned, typed back stack (`ui/navigation/TvNavigator`).

```
MainActivity
  └─ RoamTvTheme
       └─ TvApp
            ├─ TvNavRail (primary destinations)
            └─ current TvRoute
                 ├─ HomeScreen
                 ├─ feature screens (M1+)
                 └─ dialogs / text editors
```

The app deliberately has one Activity: notification taps, the TV launcher,
`roamtv://` deep links, text shares, shortcuts and Tasker all resolve to
`MainActivity`, so the back stack and focus owner are unambiguous. Phone-era
Activity-per-screen navigation is archived in git history and is not compiled.

## Layers

| Layer | Responsibility |
|---|---|
| `ui/` | Compose for TV screens, components, theme, navigation and focus |
| `feature/` | Per-feature ViewModels, immutable UI state, actions and repositories |
| `mvvm/` | Shared ViewModel base class and event primitives |
| `handler/` | Profiles, subscriptions, MMKV settings and application operations |
| `core/` | Native core configuration, lifecycle and service coordination |
| `service/` | VPN, proxy-only, root, test and subscription-update services |
| `dto/` | Persisted and serialized data models |

`ui/` must not read or write MMKV or a repository directly. Durable state lives
in `feature/` or the owning `handler/`/`core/`/`service/` class.

## Navigation and focus

`TvNavigator` stores `TvRoute` values in a `SnapshotStateList`, saves stable
route keys through a `Saver`, and exposes `push`, `pop`, `popToRoot`,
`openRoot` and `replace`. Routes carry GUIDs/IDs, never list positions.

The back stack is intentionally small rather than a full navigation graph:
there are no deep-link arguments beyond a configuration URL, and TV focus
restoration needs a deterministic owner. Navigation 3 was evaluated; it adds
no capability this app needs while its focus/scene integration is still
evolving.

Each screen receives focus on first composition. Dialogs are platform focus
traps. Lists and grids must use stable domain keys and restore focus by ID.

## Theme and layout

`ui/theme/TvTheme.kt` defines the fixed Deep Field palette and 10-foot
typography. `TvOverscan` reserves 5% of the viewport on each edge; `TvRailLayout`
adds the navigation rail. Primary destinations keep the rail. Secondary
screens use `TvTopBar` with a back action and no rail.

The theme is dark-only. TV panels are often viewed in a dark room, and a fixed
palette removes the phone-era light/dark and dynamic-colour branches.

## Import and pairing

The TV has no camera. The import screen starts a temporary HTTP server on the
active LAN address and shows a QR code. The phone opens a self-contained page,
encrypts the payload with AES-GCM using a key carried in the URL fragment, and
posts the ciphertext. The server accepts one short-lived session, rate-limits
requests, never logs the payload, and stops when the import completes or times
out. `ACCESS_LOCAL_NETWORK` is required on Android 17+.

## Build and dependencies

- Android project: `android/`
- compileSdk/targetSdk 37, minSdk 26
- AGP 9.5.1, Gradle 9.8.2, Kotlin 2.4.21
- Compose BOM 2026.09.00, tv-material 1.1.0, tv-foundation 1.0.0
- Project-local JDK under `.tooling/`; no system environment changes

The phone UI archive lives outside the compiled source set and is not shipped.
