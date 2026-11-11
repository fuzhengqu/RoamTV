#!/bin/bash
set -o errexit
set -o pipefail
set -o nounset
# Set magic variables for current file & dir
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
__file="${__dir}/$(basename "${BASH_SOURCE[0]}")"
__base="$(basename ${__file} .sh)"
if [[ ! -d $NDK_HOME ]]; then
  echo "Android NDK: NDK_HOME not found. please set env \$NDK_HOME"
  exit 1
fi
TMPDIR=$(mktemp -d)
clear_tmp () {
  # Best-effort cleanup: the workspace may disallow -rf on temporary trees.
  # All temp content lives inside the project's own .tooling/tmp (gitignored).
  rm -rf "$TMPDIR" 2>/dev/null || true
}
trap 'echo -e "Aborted, error $? in command: $BASH_COMMAND"; trap ERR; clear_tmp; exit 1' ERR INT

ABIS="armeabi-v7a arm64-v8a x86 x86_64"

# Windows NDK distributions expose the POSIX wrapper at build/ndk-build while
# the root only carries ndk-build.cmd. Select whichever exists so the same
# script works from Git Bash on Windows and on POSIX CI runners.
if [[ -f "$NDK_HOME/ndk-build" ]]; then
    NDK_BUILD="$NDK_HOME/ndk-build"
else
    NDK_BUILD="$NDK_HOME/build/ndk-build"
fi
if [[ ! -f "$NDK_BUILD" ]]; then
    echo "Android NDK: ndk-build wrapper not found under $NDK_HOME"
    exit 1
fi

# On Windows the NDK makefiles are written for cmd.exe (they use `rem`, `if
# exist`, etc.) and require the Windows-native make. The POSIX wrapper would
# otherwise classify the host as Cygwin, reject the Windows make, and fall back
# to /usr/bin/sh. Force the Windows host classification and shell here only;
# GNUMAKE is supplied by scripts/env.sh.
NDK_BUILD_SHELL_ARGS=()
case "${OSTYPE:-}" in
    cygwin|msys)
        export OSTYPE=msys
        NDK_BUILD_SHELL_ARGS+=(SHELL=cmd)
        ;;
esac

mkdir -p "$TMPDIR/jni"
pushd "$TMPDIR"

# Work on a private copy. A symlink into the submodule would make the fixups
# below mutate the checked-out submodule worktree.
cp -r "$__dir/hev-socks5-tunnel" jni/hev-socks5-tunnel

# Git for Windows checks out symlinks as plain text files holding the link
# target when core.symlinks=false (the default without developer mode). The C
# compiler then sees e.g. "../src/lib/object/hev-object-atomic.h" as source and
# fails. Restore each recorded 120000 entry as a real copy of its target inside
# the temporary tree only. On POSIX CI runners these are already real links and
# are simply overwritten with identical content, so the step is idempotent.
restore_symlinks() {
    local copy_root="$TMPDIR/jni/hev-socks5-tunnel"
    local entry sub gitdir base rel target resolved
    local mapping=(
        "|$__dir/hev-socks5-tunnel"
        "src/core|$__dir/hev-socks5-tunnel/src/core"
        "third-part/hev-task-system|$__dir/hev-socks5-tunnel/third-part/hev-task-system"
        "third-part/yaml|$__dir/hev-socks5-tunnel/third-part/yaml"
    )
    for entry in "${mapping[@]}"; do
        sub="${entry%%|*}"; gitdir="${entry#*|}"
        base="$copy_root/$sub"
        while IFS= read -r rel; do
            [[ -n "$rel" ]] || continue
            target=$(git -C "$gitdir" cat-file -p ":$rel")
            resolved="$(dirname "$base/$rel")/$target"
            if [[ -f "$resolved" ]]; then
                cp -f "$resolved" "$base/$rel"
            else
                echo "WARN: symlink target missing: $resolved" >&2
            fi
        done < <(git -C "$gitdir" ls-files -s | awk '$1=="120000"{print $4}')
    done
}
restore_symlinks

# 1) JNI shared library (libhev-socks5-tunnel.so) — loaded in-process by
#    tv.roam.client.service.TProxyService for the VpnService hev tun mode.
echo 'include $(call all-subdir-makefiles)' > jni/Android.mk

"$NDK_BUILD" \
    NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=jni/Android.mk \
    "APP_ABI=$ABIS" \
    APP_PLATFORM=android-26 \
    NDK_LIBS_OUT="$TMPDIR/libs" \
    NDK_OUT="$TMPDIR/obj" \
    "APP_CFLAGS=-O3 -DPKGNAME=tv/roam/client/service" \
    "APP_LDFLAGS=-Wl,--build-id=none -Wl,--hash-style=gnu" \
    "${NDK_BUILD_SHELL_ARGS[@]}" \

# 2) Standalone executable (libhevsockstun.so) — run as a separate root
#    process by tv.roam.client.core.root for the Root run mode. Same hev source,
#    no -DENABLE_LIBRARY so hev-main.c's main() is built, and BUILD_EXECUTABLE
#    instead of a shared library. It creates its own tun and reads a YAML config.
cat > jni/exec.mk <<'EXECMK'
TOP_PATH := $(call my-dir)/hev-socks5-tunnel

ifeq ($(filter $(modules-get-list),yaml),)
    include $(TOP_PATH)/third-part/yaml/Android.mk
endif
ifeq ($(filter $(modules-get-list),lwip),)
    include $(TOP_PATH)/third-part/lwip/Android.mk
endif
ifeq ($(filter $(modules-get-list),hev-task-system),)
    include $(TOP_PATH)/third-part/hev-task-system/Android.mk
endif

LOCAL_PATH := $(TOP_PATH)
SRCDIR := $(LOCAL_PATH)/src

include $(CLEAR_VARS)
include $(LOCAL_PATH)/build.mk
LOCAL_MODULE    := hevsockstun
LOCAL_SRC_FILES := $(patsubst $(SRCDIR)/%,src/%,$(SRCFILES))
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/src \
	$(LOCAL_PATH)/src/misc \
	$(LOCAL_PATH)/src/core/include \
	$(LOCAL_PATH)/third-part/yaml/include \
	$(LOCAL_PATH)/third-part/lwip/src/include \
	$(LOCAL_PATH)/third-part/lwip/src/ports/include \
	$(LOCAL_PATH)/third-part/hev-task-system/include
LOCAL_CFLAGS += -DFD_SET_DEFINED -DSOCKLEN_T_DEFINED
LOCAL_CFLAGS += $(VERSION_CFLAGS)
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -mfpu=neon
endif
LOCAL_STATIC_LIBRARIES := yaml lwip hev-task-system
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
LOCAL_LDFLAGS += -Wl,-z,common-page-size=16384
include $(BUILD_EXECUTABLE)
EXECMK

"$NDK_BUILD" \
    NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=jni/exec.mk \
    "APP_ABI=$ABIS" \
    APP_PLATFORM=android-26 \
    NDK_LIBS_OUT="$TMPDIR/libs-exec" \
    NDK_OUT="$TMPDIR/obj-exec" \
    "APP_CFLAGS=-O3" \
    "APP_LDFLAGS=-Wl,--build-id=none -Wl,--hash-style=gnu" \
    "${NDK_BUILD_SHELL_ARGS[@]}" \

# Stage both artifacts under libs/<abi>/. The executable is renamed to
# lib*.so so the APK installer extracts it into nativeLibraryDir as an
# executable file (filename distinct from the JNI library above).
mkdir -p "$__dir/libs"
cp -r "$TMPDIR/libs/"* "$__dir/libs/"
for abi in $ABIS; do
  cp "$TMPDIR/libs-exec/$abi/hevsockstun" "$__dir/libs/$abi/libhevsockstun.so"
done

popd
clear_tmp
