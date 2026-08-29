#!/usr/bin/env bash
# Provision a container for OpenPocketCine Android builds.
#
# The devcontainer image (swift:6.3.3-noble) already ships the Swift toolchain the
# Swift Android SDK requires. This script installs it when absent, so it also works
# in a plain Ubuntu 24.04 Codespace that predates the devcontainer. It then adds the
# Android SDK/NDK and hands off to the same scripts/ci-install-swift-android.sh that
# GitHub Actions runs, so a Codespace and CI cross-compile the core identically.
set -euo pipefail

readonly SWIFT_VERSION="6.3.3"
readonly SWIFT_ROOT="/opt/swift"
readonly SWIFT_TARBALL_URL="https://download.swift.org/swift-6.3.3-release/ubuntu2404/swift-6.3.3-RELEASE/swift-6.3.3-RELEASE-ubuntu24.04.tar.gz"
readonly NDK_VERSION="27.2.12479018"
readonly BUILD_TOOLS="36.0.0"
readonly PLATFORM="android-36"
readonly CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"

sdk_root="${ANDROID_HOME:-/opt/android-sdk}"
repository_root="$(cd "$(dirname "$0")/.." && pwd -P)"

fail() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

as_root() {
  if [[ "$(id -u)" -eq 0 ]]; then "$@"; else sudo "$@"; fi
}

own() {
  as_root chown -R "$(id -u):$(id -g)" "$1"
}

echo "==> Base packages"
as_root apt-get update -qq
as_root apt-get install -y -qq --no-install-recommends unzip zip curl file

if command -v swift >/dev/null 2>&1 && swift --version 2>/dev/null | grep -q "$SWIFT_VERSION"; then
  echo "==> Swift $SWIFT_VERSION already present"
else
  echo "==> Swift $SWIFT_VERSION -> $SWIFT_ROOT"
  as_root apt-get install -y -qq --no-install-recommends \
    binutils libc6-dev libcurl4-openssl-dev libedit2 libncurses-dev \
    libpython3-dev libsqlite3-0 libxml2-dev libz3-dev pkg-config tzdata zlib1g-dev
  as_root mkdir -p "$SWIFT_ROOT"
  own "$SWIFT_ROOT"
  curl -fsSL "$SWIFT_TARBALL_URL" | tar xz --strip-components=1 -C "$SWIFT_ROOT"
  # Put the real toolchain bin on PATH rather than symlinking into /usr/local/bin:
  # ci-install-swift-android.sh needs llvm-objcopy beside the resolved swift binary.
  if ! grep -qF "$SWIFT_ROOT/usr/bin" "$HOME/.bashrc" 2>/dev/null; then
    printf 'export PATH="%s/usr/bin:$PATH"\n' "$SWIFT_ROOT" >> "$HOME/.bashrc"
  fi
fi
[[ -d "$SWIFT_ROOT/usr/bin" ]] && export PATH="$SWIFT_ROOT/usr/bin:$PATH"
command -v swift >/dev/null 2>&1 || fail "swift is not on PATH after install"

echo "==> Android command-line tools -> $sdk_root"
if [[ ! -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]]; then
  as_root mkdir -p "$sdk_root/cmdline-tools"
  own "$sdk_root"
  tools_zip="$(mktemp -d)/cmdline-tools.zip"
  curl -fsSL "$CMDLINE_TOOLS_URL" -o "$tools_zip"
  unzip -q "$tools_zip" -d "$sdk_root/cmdline-tools"
  # The archive unpacks as cmdline-tools/; sdkmanager requires the `latest` name.
  mv "$sdk_root/cmdline-tools/cmdline-tools" "$sdk_root/cmdline-tools/latest"
  rm -f "$tools_zip"
fi

sdkmanager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
[[ -x "$sdkmanager" ]] || fail "sdkmanager is missing at $sdkmanager"

echo "==> Android platform $PLATFORM, build-tools $BUILD_TOOLS, NDK $NDK_VERSION"
yes | "$sdkmanager" --licenses >/dev/null 2>&1 || true
"$sdkmanager" --install \
  "platform-tools" \
  "platforms;$PLATFORM" \
  "build-tools;$BUILD_TOOLS" \
  "ndk;$NDK_VERSION" >/dev/null
[[ -d "$sdk_root/ndk/$NDK_VERSION" ]] || fail "NDK $NDK_VERSION did not install"

echo "==> Swift Android SDK"
# Swift 6.1+ Android SDKs treat ANDROID_NDK_ROOT as an NDK override and then fail
# to find the Swift runtime libraries; keep it out of the environment.
unset ANDROID_NDK_ROOT
ANDROID_NDK_HOME="$sdk_root/ndk/$NDK_VERSION" \
  "$repository_root/scripts/ci-install-swift-android.sh"

echo "==> Ready. Build with:"
echo "    cd Apps/Android && ANDROID_HOME=$sdk_root ./gradlew assembleDebug"
