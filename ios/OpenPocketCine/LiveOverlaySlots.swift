import SwiftUI
import OpenPocketViewCore

/// OpenZCine `MonitorSystemCluster.settingsButton` (`MonitorUnified.swift` ~1098).
struct LiveSettingsButton: View {
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            LiveRailCircle(systemName: "gearshape")
        }
        .buttonStyle(.zcTapTarget)
        .accessibilityLabel("Open Operator Setup")
        .accessibilityIdentifier("monitor.system.settings")
    }
}

/// OpenZCine `MonitorSystemCluster.mediaButton` (`MonitorUnified.swift` ~1114).
struct LiveMediaButton: View {
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            LiveRailCircle(systemName: "rectangle.stack")
        }
        .buttonStyle(.zcTapTarget)
        .accessibilityLabel("Open Media")
        .accessibilityIdentifier("monitor.system.media")
    }
}

/// Mimo's green (x) on the locked subject box.
struct LiveTrackingCancelButton: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        Button {
            model.session.cancelSubjectTracking()
        } label: {
            Image(systemName: "xmark")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(LiveDesign.background)
                .frame(
                    width: LiveTrackingChrome.cancelSize,
                    height: LiveTrackingChrome.cancelSize
                )
                .background(LiveDesign.good, in: Circle())
        }
        .buttonStyle(.zcTapTarget)
        .accessibilityLabel("Stop subject tracking")
        .accessibilityIdentifier("monitor.system.trackCancel")
    }
}

/// OpenZCine `dot.viewfinder` recenter — AF to centre and end tracking.
struct LiveFocusResetButton: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        Button {
            model.session.resetFocusPoint()
        } label: {
            Image(systemName: "dot.viewfinder")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(LiveDesign.text)
                .frame(
                    width: LiveChromeMetrics.focusResetSize,
                    height: LiveChromeMetrics.focusResetSize
                )
                .background(.black.opacity(0.55), in: Circle())
                .overlay(Circle().strokeBorder(LiveDesign.hairline, lineWidth: 1))
        }
        .buttonStyle(.zcTapTarget)
        .accessibilityLabel("Recenter focus")
        .accessibilityHint("Moves the focus box back to the center and ends subject tracking")
        .accessibilityIdentifier("monitor.system.focusReset")
    }
}

/// OpenZCine `AssetCircleButton` with an SF Symbol stand-in (no copyrighted rail assets).
private struct LiveRailCircle: View {
    let systemName: String

    var body: some View {
        let size = LiveChromeMetrics.auxiliaryButtonSize
        Image(systemName: systemName)
            .font(.system(size: size * 0.36, weight: .medium))
            .foregroundStyle(LiveDesign.text.opacity(0.86))
            .frame(width: size, height: size)
            .liveChromeCircle()
    }
}
