# Native cores

RoamTV embeds two native components. The Android build consumes a prebuilt
`libv2ray.aar` for Xray and locally compiled HEV libraries; the source archives
include the matching submodule revisions.

| Component | Version | Revision | Build source |
|---|---|---|---|
| Xray-core (AndroidLibXrayLite) | 26.11.9 | `b5a3770` | `AndroidLibXrayLite` submodule; CI downloads `libv2ray.aar` from the matching release tag |
| hev-socks5-tunnel | 2.19.3 | `c5584fe` | `hev-socks5-tunnel` submodule, built by `compile-hevtun.sh` for all four ABIs |

sing-box is not embedded. Adding it would be a second core integration, not a
version bump.

## Update procedure

1. Pick a **stable** upstream tag; do not update to preview/alpha builds.
2. Fetch the tag in the submodule and check it out; confirm
   `git describe --tags --abbrev=0` reports the tag.
3. For Xray, download `libv2ray.aar` from the same AndroidLibXrayLite release
   into `android/app/libs/libv2ray.aar` (the file is gitignored; CI downloads it
   from the submodule tag automatically).
4. For HEV, run `compile-hevtun.sh` with the NDK version from
   `.github/workflows/build.yml`; verify four ABIs.
5. Confirm the component versions:
   - `unzip -l android/app/libs/libv2ray.aar` contains `jni/<abi>/libgojni.so`
     for `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`;
   - `aapt2 dump badging` on each APK lists the expected `native-code`;
   - About/logcat shows the new Xray version after connecting.
6. Run the full validation (`testPlaystoreDebugUnitTest`, `lintPlaystoreDebug`,
   `assemblePlaystoreDebug`), the signed release, and a real-TV protocol smoke
   test (VLESS plus any available profiles; HEV and Xray TUN modes).
7. Publish a patch release with the source archive; never replace a published
   APK in place.

The CI uses the AndroidLibXrayLite tag from the submodule to download the AAR,
so the submodule gitlink and the AAR release must always move together.
