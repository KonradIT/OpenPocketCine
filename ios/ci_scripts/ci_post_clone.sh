#!/bin/sh
# Xcode Cloud: generate the Xcode project and inject Frame.io PKCE values.
# The xcodeproj is gitignored and produced by XcodeGen; this script must run
# before xcodebuild. Frameio.local.xcconfig is gitignored; without this the
# archive ships with an empty FrameioClientID. Empty-safe: missing vars
# reproduce the default (Frame.io login disabled, non-fatal).
set -eu

cd "$CI_PRIMARY_REPOSITORY_PATH"

if ! command -v xcodegen >/dev/null 2>&1; then
  brew install xcodegen
fi
(cd ios && xcodegen generate)

cat > ios/OpenPocketCine/Frameio.local.xcconfig <<EOF
FRAMEIO_CLIENT_ID = ${FRAMEIO_CLIENT_ID:-}
FRAMEIO_REDIRECT_URI = ${FRAMEIO_REDIRECT_URI:-}
FRAMEIO_URL_SCHEME = ${FRAMEIO_URL_SCHEME:-}
EOF

if [ -z "${FRAMEIO_CLIENT_ID:-}" ]; then
  echo "warning: FRAMEIO_CLIENT_ID not set — Frame.io login will be disabled in this build."
fi
