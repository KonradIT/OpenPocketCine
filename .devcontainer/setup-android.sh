#!/usr/bin/env bash
# Provision a Codespaces container for Android builds.
#
# The base image is the official swift:6.3.3-noble, so the host Swift toolchain
# already matches what the Swift Android SDK requires. This adds the Android
# SDK/NDK, then hands off to the same scripts/ci-install-swift-android.sh that
# GitHub Actions runs, so a Codespace and CI cross-compile the core identically.
set -euo pipefail

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

echo "==> Base packages"
as_root apt-get update -qq
as_root apt-get install -y -qq --no-install-recommends unzip zip curl file

echo "==> Android command-line tools -> $sdk_root"
if [[ ! -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]]; then
  as_root mkdir -p "$sdk_root/cmdline-tools"
  as_root chown -R "$(id -u):$(id -g)" "$sdk_root"
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
# The install script treats ANDROID_NDK_ROOT as an NDK override and then fails to
# find the Swift runtime; it unsets it itself, but keep the env clean regardless.
unset ANDROID_NDK_ROOT
ANDROID_NDK_HOME="$sdk_root/ndk/$NDK_VERSION" \
  "$repository_root/scripts/ci-install-swift-android.sh"

echo "==> Ready. Build with:"
echo "    cd Apps/Android && ./gradlew assembleDebug"
