#!/usr/bin/env bash
# RoamTV -- in-project toolchain environment.
# Usage:  source scripts/env.sh
#
# Rationale: the user requires that no file outside the project directory is
# created, modified or deleted. Java/Gradle/sdkmanager are native Windows
# processes, so they receive Windows-style paths; POSIX tools (mktemp, cp,
# git) receive MSYS-style paths. Both are derived from one root.
# System Android SDK / NDK are referenced read-only, never written to.
# This script is session-scoped only: it never writes system or user-level
# environment variables, the registry, or any global configuration.

R_POSIX="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
R_WIN="$(cygpath -m "$R_POSIX" 2>/dev/null || echo "$R_POSIX")"
SYSTEM_SDK_WIN="D:/Android-SDK"
SYSTEM_SDK_POSIX="/d/Android-SDK"
NDK_VERSION="30.0.16248370"

# --- native Windows-process variables (Java, Gradle, sdkmanager, aapt, adb) ---
export JAVA_HOME="$R_WIN/.tooling/jdk17"
export GRADLE_USER_HOME="$R_WIN/.tooling/gradle"
# AGP 9.x rejects multiple variables pointing at the same location with
# AndroidLocationsException; only ANDROID_HOME and ANDROID_USER_HOME are set.
export ANDROID_HOME="$R_WIN/.tooling/android-sdk"
export ANDROID_USER_HOME="$R_WIN/.tooling/android-user"
export ANDROID_AVD_HOME="$R_WIN/.tooling/avd"
export ADB_VENDOR_KEYS="$R_WIN/.tooling/avd"
export ANDROID_NDK_HOME="$SYSTEM_SDK_WIN/ndk/$NDK_VERSION"
export NDK_HOME="$SYSTEM_SDK_POSIX/ndk/$NDK_VERSION"
# ndk-build resolves its bundled make as prebuilt/<host-tag>/bin/make (no .exe)
# which does not exist on Windows, then falls back to a system `make` that is
# absent in Git Bash. Point GNUMAKE at the NDK's own Windows build explicitly.
export GNUMAKE="$NDK_HOME/prebuilt/windows-x86_64/bin/make.exe"
export TEMP="$R_WIN/.tooling/tmp"
export TMP="$TEMP"

# --- POSIX variables (mktemp, shell scripts, git) ---
export TMPDIR="$R_POSIX/.tooling/tmp"
export PROJECT_ROOT="$R_POSIX"
export PROJECT_ROOT_WIN="$R_WIN"

mkdir -p "$R_POSIX/.tooling/tmp" "$R_POSIX/.tooling/gradle" \
         "$R_POSIX/.tooling/android-user" "$R_POSIX/.tooling/avd" \
         "$R_POSIX/.tooling/verify"

export PATH="$R_POSIX/.tooling/jdk17/bin:$SYSTEM_SDK_POSIX/platform-tools:$SYSTEM_SDK_POSIX/cmdline-tools/latest/bin:$PATH"

# Resolve the outbound proxy from the current session environment instead of
# hard-coding a port. A previously configured port (10407) is no longer
# listening, which silently broke Gradle dependency resolution. If no proxy is
# present, or the resolved endpoint is unreachable, fall back to direct
# connections. All values are session-scoped; nothing is written to disk or to
# the system configuration.
_proxy_url="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
# Gradle's default 30s socket timeout causes bulk dependency resolution to
# fail with "Read timed out" on a proxied connection even though single
# requests succeed. These are project-scoped JVM options only.
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=180000"

if [[ -n "$_proxy_url" ]]; then
    _proxy_authority="${_proxy_url#*://}"
    _proxy_authority="${_proxy_authority%%/*}"
    _proxy_authority="${_proxy_authority##*@}"
    _proxy_host="${_proxy_authority%%:*}"
    _proxy_port="${_proxy_authority##*:}"
    [[ "$_proxy_host" == "$_proxy_port" ]] && _proxy_port=80
    if timeout 2 bash -c "exec 3<>/dev/tcp/$_proxy_host/$_proxy_port" 2>/dev/null; then
        export GRADLE_OPTS="${GRADLE_OPTS:-} -Dhttp.proxyHost=$_proxy_host -Dhttp.proxyPort=$_proxy_port -Dhttps.proxyHost=$_proxy_host -Dhttps.proxyPort=$_proxy_port"
        export ROAMTV_PROXY_MODE="proxy:$_proxy_host:$_proxy_port"
    else
        export ROAMTV_PROXY_MODE="direct:unreachable-proxy"
    fi
else
    export ROAMTV_PROXY_MODE="direct:no-proxy"
fi
unset _proxy_url _proxy_authority _proxy_host _proxy_port

# Never let GitHub credentials touch disk. Callers that push should inject the
# token through in-memory GIT_CONFIG_COUNT / GIT_CONFIG_KEY_* variables.
