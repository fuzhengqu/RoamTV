# RoamTV — GPL-3.0 Compliance and Source Reproduction

This document is part of RoamTV's Corresponding Source. It describes how to
reproduce a binary release from this repository, as required by GNU GPL
version 3, section 6.

## 1. License and derivative status

RoamTV is derived from v2rayNG 2.3.15 (upstream `2dust/v2rayNG`, commit
`57907cf354d9769200ed4717b4430d605abe0952`), licensed GPL-3.0. The full text
is in `LICENSE` at the repository root and must remain verbatim. The
modification declaration and date are in `NOTICE`. Third-party copyright and
license texts are in `android/app/src/main/assets/open_source_licenses.html`,
`AndroidLibXrayLite/LICENSE` (LGPL-3.0) and `hev-socks5-tunnel/LICENSE`
(MIT). None of these files may be removed from a source distribution.

## 2. Toolchain (all inside this repository)

| Component | Version | Location |
|---|---|---|
| JDK | 17 (Temurin 17.0.21+9) | `.tooling/jdk17/` |
| Gradle | 9.8.2 (wrapper) | `.tooling/gradle/`, `android/gradle/wrapper/` |
| Android Gradle Plugin | 9.5.1 | `android/gradle/libs.versions.toml` |
| Kotlin | 2.4.21 | `android/gradle/libs.versions.toml` |
| Android SDK Platform | 37.2 (API 37, extension 24) | `.tooling/android-sdk/platforms/android-37.2/` |
| Android Build Tools | 37.0.0 | `.tooling/android-sdk/build-tools/37.0.0/` |
| Android NDK | 30.0.16248370 | external SDK, referenced read-only |

`scripts/env.sh` sets every path session-scoped. It never modifies system or
user-level environment variables, the registry, or any file outside this
repository. Source it before any build command:

```sh
source scripts/env.sh
```

## 3. Native inputs (vendored)

- `android/app/libs/libv2ray.aar` — the Xray core AAR. Its source is the
  `AndroidLibXrayLite` submodule (LGPL-3.0). The source revision must match
  the gitlink recorded by `git ls-tree HEAD AndroidLibXrayLite`.
- `android/app/libs/<abi>/libhev-socks5-tunnel.so` and
  `libhevsockstun.so` — built from the `hev-socks5-tunnel` submodule
  (MIT) with `compile-hevtun.sh` and NDK 30.0.16248370 for the four ABIs
  `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`.

These files are build inputs, not source. Their complete source is included in
the submodules. Plain `git archive` does not copy submodule work trees, so the
release source archive must be assembled from the main repository **and** each
submodule (plus the vendored binaries used by the release) before the tag is
published. This is a release-blocking check for M6.

## 4. Reproducing a release from source

```sh
git clone --recurse-submodules https://github.com/fuzhengqu/RoamTV.git
cd RoamTV
# Place JDK 17 in .tooling/jdk17 and SDK Platform 37.2 in
# .tooling/android-sdk/platforms/android-37.2 (see scripts/env.sh).
source scripts/env.sh
bash compile-hevtun.sh
cp -r libs/. android/app/libs/
cd android
./gradlew :app:assembleRelease
```

On Windows/Git Bash, `compile-hevtun.sh` automatically selects the NDK's
POSIX wrapper, forces `SHELL=cmd`, uses the bundled Windows GNU Make, and
restores the 31 symbolic links that Git for Windows checks out as plain text
files. All of this happens in a temporary directory inside `.tooling/tmp`.

Verification baseline for every release:

```sh
source scripts/env.sh
bash scripts/verify-stage.sh <stage>
```

## 5. Source archive for a binary release

For each published tag, produce and attach:

```sh
git archive --format=zip --prefix=RoamTV-<tag>/ -o RoamTV-<tag>-source.zip <tag>
```

The archive must contain `LICENSE`, `NOTICE`, this document, all Gradle
scripts and version catalogs, `compile-hevtun.sh`, the submodule sources,
and the vendored binaries needed to build. No API keys, tokens, keystores,
signing passwords, or account credentials are stored in this repository;
release signing material is supplied only at build time as build parameters.

## 6. In-app Appropriate Legal Notices

The application's Settings > Legal / About screens display the copyright
notice, the no-warranty statement, the GPLv3 license text, the upstream
source attribution, and the third-party license texts from local assets.
No network access is required to view them.

## 7. Scope note

This document and `NOTICE` describe legal and technical provenance only.
They do not imply endorsement by, or any ongoing association with, the
upstream project. The repository is a GitHub fork; the platform-level fork
relationship cannot be removed and is disclosed here for completeness.
