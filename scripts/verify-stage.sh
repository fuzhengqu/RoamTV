#!/usr/bin/env bash
# RoamTV -- per-stage build/lint/test verification baseline (P-V).
# Usage:  source scripts/env.sh && bash scripts/verify-stage.sh <stage-number>
# All logs land in .tooling/verify/ (gitignored). No file outside the project
# directory is created, modified or deleted.
set -o pipefail

STAGE="${1:-0}"
R_POSIX="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY_DIR="$R_POSIX/.tooling/verify"
mkdir -p "$VERIFY_DIR"

cd "$R_POSIX/V2rayNG" || exit 1

echo "=== P-V stage $STAGE: Gradle ==="
./gradlew :app:assemblePlaystoreDebug \
          :app:lintPlaystoreDebug \
          :app:testPlaystoreDebugUnitTest \
          :app:processPlaystoreDebugMainManifest \
          --console=plain --max-workers=2 --no-parallel 2>&1 | tee "$VERIFY_DIR/stage-$STAGE-gradle.log"
GRADLE_EXIT=${PIPESTATUS[0]}
echo "GRADLE_EXIT=$GRADLE_EXIT"

APK=$(find "$R_POSIX/V2rayNG/app/build/outputs/apk/playstore/debug" -name "*.apk" 2>/dev/null | head -1)
AAPT2="$R_POSIX/.tooling/android-sdk/build-tools/37.0.0/aapt2.exe"
[[ -f "$AAPT2" ]] || AAPT2="D:/Android-SDK/build-tools/37.0.0/aapt2.exe"

if [[ -n "$APK" && -f "$AAPT2" ]]; then
    echo "=== P-V stage $STAGE: aapt2 badging ==="
    "$AAPT2" dump badging "$APK" > "$VERIFY_DIR/stage-$STAGE-badging.txt" 2>&1
    grep -E "package:|sdkVersion:|targetSdkVersion:|application-label:|nativeLibrary" \
        "$VERIFY_DIR/stage-$STAGE-badging.txt" | head -20

    echo "=== P-V stage $STAGE: aapt2 manifest ==="
    "$AAPT2" dump xmltree --file AndroidManifest.xml "$APK" > "$VERIFY_DIR/stage-$STAGE-manifest.txt" 2>&1
    MANIFEST_LINES=$(wc -l < "$VERIFY_DIR/stage-$STAGE-manifest.txt")
    echo "manifest lines: $MANIFEST_LINES"
    if [[ "$MANIFEST_LINES" -lt 100 ]]; then
        echo "ERROR: manifest dump looks truncated; aapt2 flags need review" >&2
    fi
else
    echo "APK or aapt2 not available; aapt2 checks = Not run"
fi

echo "=== P-V stage $STAGE: git diff --check ==="
git -C "$R_POSIX" diff --check
DIFF_EXIT=$?
echo "DIFF_EXIT=$DIFF_EXIT"

echo "=== P-V stage $STAGE: summary ==="
echo "GRADLE_EXIT=$GRADLE_EXIT DIFF_EXIT=$DIFF_EXIT"
if [[ "$GRADLE_EXIT" -eq 0 && "$DIFF_EXIT" -eq 0 ]]; then
    echo "P-V stage $STAGE: PASS"
    exit 0
fi
echo "P-V stage $STAGE: FAIL"
exit 1
