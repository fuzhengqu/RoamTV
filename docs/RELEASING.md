# Releasing RoamTV

This document is the single source of truth for RoamTV version numbers,
GitHub tags and GitHub Releases. Apply it to every release.

## Version numbers

`versionName` uses exactly two numeric components: `major.minor`.

`versionCode` is derived from the name:

```text
versionCode = major * 100 + minor
```

| versionName | versionCode |
|---|---:|
| 0.01 | 1 |
| 0.02 | 2 |
| 0.09 | 9 |
| 0.10 | 10 |
| 0.20 | 20 |
| 0.99 | 99 |
| 1.00 | 100 |
| 1.01 | 101 |

Rules:

- `versionCode` must increase for every published build. Never reuse a code.
- Do not use a patch component such as `0.01.1`; ship the next minor instead.
- `0.99` is followed by `1.00`. Never create `0.100`.
- A minor jump is allowed only for a milestone or major change:
  `0.09 -> 0.10`, `0.19 -> 0.20`, `0.20 -> 1.00`.
- The declared `versionCode` above is the source of truth. Release APKs add a
  fixed per-ABI offset in `android/app/build.gradle.kts`; the offset is
  deterministic and still increments with the declared code.

The current source version is `0.03` (`versionCode = 3`). The first public
version was `0.01` (`versionCode = 1`). Development debug APKs use the same
version code but a different signing key; testers must uninstall a debug build
before installing a release APK.

## What gets a GitHub Release

Create a GitHub Release for:

- the first public version, `v0.01`;
- every new top-level feature area (for example routing, per-app proxy, assets);
- `x.10`, `x.20` and `1.00` milestones;
- the first public test or store listing;
- breaking changes: save format, application ID, permissions, signing key;
- a large visual or architecture rewrite.

Only create a git tag for:

- bug fixes;
- small features inside an existing area;
- copy, translation and UI polish;
- ordinary performance or internal refactors.

Tag-only versions have no GitHub Release and no signed APK assets. Testers can
use CI debug artifacts or build locally.

## Planned cadence

| Version | Scope | GitHub Release |
|---|---|---|
| 0.01 | TV shell, home, import/pairing, editors, subscriptions | Yes, pre-release |
| 0.02 | Routing settings and rule editor | Yes |
| 0.03 | Per-app proxy | Yes |
| 0.04 | User assets | Yes |
| 0.05-0.09 | Fixes and small improvements | No, tag only |
| 0.10 | Settings, backup, logs, about, update | Yes |
| 0.11-0.19 | Fixes and small improvements | No, tag only |
| 0.20 | First wider public test / M6 | Yes |
| 0.21-0.99 | Fixes and small improvements | No, tag only |
| 1.00 | Stable release | Yes, full release |

## Tag naming

Use annotated tags with a `v` prefix:

```text
v0.01
v0.02
v0.10
v1.00
```

Do not create releases for the inherited upstream `2.3.x` tags. Leave those
tags untouched.

## Release checklist

### 1. Preflight

- Confirm the version is the next value in the table.
- Confirm `versionCode` is greater than every published code.
- Confirm the tag and GitHub Release do not already exist.
- Confirm the repository Actions secrets exist. Required names:
  - `APP_KEYSTORE_BASE64`
  - `APP_KEYSTORE_PASSWORD`
  - `APP_KEYSTORE_ALIAS`
  - `APP_KEY_PASSWORD`
  - `GPG_PRIVATE_KEY`
- Confirm the selected commit has a green `Build APK` run.
- Check whether `master` is protected. If direct pushes are blocked, use a
  release pull request and tag after it merges.

Never commit a keystore, signing password, GPG private key or token.

### 2. Version commit

On `master` (or the release pull request), change only the release metadata:

```kotlin
versionCode = <major * 100 + minor>
versionName = "<major.minor>"
```

Add or update `docs/releases/v<version>.md`, then commit:

```text
chore(release): v<version>
```

### 3. Tag

```bash
git tag -a v0.01 -m "RoamTV v0.01 - First public test build / 首个公开测试版"
git push origin v0.01
```

### 4. GitHub Release

Create the release before uploading assets so the bilingual notes are the
release body:

```bash
gh release create v0.01 \
  --title "RoamTV v0.01 - First public test build / 首个公开测试版" \
  --notes-file docs/releases/v0.01.md \
  --prerelease
```

Use `--prerelease` for `0.x` test builds. Omit it for `1.00`.

### 5. Signed APK assets

Dispatch the workflow on the tag, not on a moving branch:

```bash
gh workflow run build.yml \
  --ref v0.01 \
  -f release_tag=v0.01 \
  -f prerelease=true
```

The workflow builds and signs:

- `RoamTV_<version>_arm64-v8a.apk`
- `RoamTV_<version>_armeabi-v7a.apk`
- `RoamTV_<version>_x86.apk`
- `RoamTV_<version>_universal.apk`
- one `.sig` per APK
- `RoamTV-public-key.asc`

For a stable release, pass `-f prerelease=false`.

### 6. Verify

- `gh release view v<version>` shows the correct title, notes and pre-release
  state.
- Every APK, `.sig` and the public key are present.
- Download the universal APK and install it on an API 26+ emulator.
- Check `versionName` and `versionCode` with `aapt2 dump badging` or
  `apkanalyzer manifest print`.
- Verify a detached signature with `gpg --verify <apk>.sig <apk>`.
- Check the download links from the public release page.

If the workflow uploads only part of the assets, rerun the failed job. For a
manual recovery, use `gh release upload v<version> <file> --clobber`.

## Release notes template

`docs/releases/v<version>.md` is bilingual. Keep Chinese first, English second.

```markdown
# RoamTV v0.01 / 首个公开测试版

## 中文

### 主要更新
### 验证情况
### 已知限制
### 安装说明

## English

### Highlights
### Verification
### Known limitations
### Installation
```

Include the Android version range, the signing key fingerprint and any
breaking change. Do not include credentials, private URLs or personal data.

## Branching after a release

- Merge the release into `master` before tagging.
- Keep the release commit and tag on `master`.
- Start the next feature area from `master` on a short-lived branch.
- Tag-only releases may be committed directly on `master` when branch
  protection allows it.
