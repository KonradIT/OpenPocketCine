# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Shared Swift protocol core for DJI Osmo Pocket: DUML framing, BLE discovery, SoftAP join, and
  HEVC/AVC live-view depacketizing.
- iOS SwiftUI shell with saved cameras, live monitor chrome, and GPU assists (LUT import, peaking,
  zebra, false color, waveform, histogram, guides).
- Android Jetpack Compose shell in `Apps/Android/` consuming the same Swift core over JNI.
- Public repository hygiene: `just check`, secret scan, landing page at openpocketcine.app.
