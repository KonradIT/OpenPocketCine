import SwiftUI

/// OpenZCine `MonitorLiveViewModuleLayout` / `MonitorSideRailControlLayout` / chrome insets.
enum LiveChromeMetrics {
    static let lockButtonSize: CGFloat = 40
    static let lockBatteryGap: CGFloat = 4
    static let auxiliaryButtonSize: CGFloat = 63.25
    static let recordButtonSize: CGFloat = 82.8
    static let displayButtonWidth: CGFloat = 73.6
    static let displayButtonHeight: CGFloat = 43.7
    static let topInfoDeckHeight: CGFloat = 46
    static let topInfoDeckSideInset: CGFloat = 10
    static let topInfoDeckControlGap: CGFloat = 12
    static let bottomBarBottomInset: CGFloat = 14
    static let bottomModuleSpacing: CGFloat = 12
    static let railWidth: CGFloat = 82.8
    static let chromeTop: CGFloat = 14
    static let chromeLeading: CGFloat = 16
    static let chromeBottom: CGFloat = 12
    static let chromeTrailing: CGFloat = 18
    static let feedAspect: CGFloat = 16 / 9
    static let cutoutMinimum: CGFloat = 50
    static let classicNotchRailwardShift: CGFloat = 10
    static let batteryIndicatorWidth: CGFloat = 38
    static let batteryPillWidth: CGFloat = 48
    static let batteryPillHeight: CGFloat = 40
    static let batteryPillLeading: CGFloat = 8
    static let batteryPillGap: CGFloat = 6
    static let batteryInlineGap: CGFloat = 12
    static let batteryInlineWidth: CGFloat = 52
    static let zoomChipInset: CGFloat = 10
    static let zoomButtonSize: CGFloat = 44
    static let gimbalStickSize: CGFloat = 88
    static let gimbalKnobSize: CGFloat = 36
    static let gimbalStickInset: CGFloat = 16
    static let gimbalStickGap: CGFloat = 8
    /// On-feed stick. Light on dark picture, dark on bright picture.
    static let gimbalStickOpacity: CGFloat = 0.55
    static let focusResetSize: CGFloat = 40
    static let focusResetGap: CGFloat = 24
    static let popupGap: CGFloat = 10
    static let topPickerGap: CGFloat = 8
    static let topPickerWidth: CGFloat = 340
    static let capturePickerMaxWidth: CGFloat = 420
    /// OpenZCine `PickerPanel` + `GlassPanel` (16+16 pad, 34 close header, 14 gap, 176 drum).
    static let drumPickerHeight: CGFloat = 256
    /// Extra hug when a mode-tab row sits under the drum (ISO / WB / resolution / audio).
    static let pickerModeBarHeight: CGFloat = 51
}

private struct InterfaceLockedKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var interfaceLocked: Bool {
        get { self[InterfaceLockedKey.self] }
        set { self[InterfaceLockedKey.self] = newValue }
    }
}

/// OpenZCine `MonitorSystemCluster.lockButton` (`MonitorUnified.swift` ~1068).
struct LiveLockButton: View {
    @Binding var locked: Bool

    var body: some View {
        Button {
            locked.toggle()
        } label: {
            Image(systemName: locked ? "lock.fill" : "lock")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(locked ? LiveDesign.accent : LiveDesign.text.opacity(0.86))
                .frame(width: LiveChromeMetrics.lockButtonSize, height: LiveChromeMetrics.lockButtonSize)
                .liveChromeGlass(
                    in: RoundedRectangle(cornerRadius: LiveDesign.cornerRadius, style: .continuous)
                )
                .overlay {
                    if locked {
                        RoundedRectangle(cornerRadius: LiveDesign.cornerRadius, style: .continuous)
                            .stroke(LiveDesign.accent.opacity(0.75), lineWidth: 1.5)
                    }
                }
        }
        .buttonStyle(.zcTapTarget)
        .sensoryFeedback(.impact(weight: .medium), trigger: locked)
        .accessibilityLabel(locked ? "Unlock monitor controls" : "Lock monitor controls")
        .accessibilityHint("Prevents accidental camera and View Assist changes")
        .accessibilityIdentifier("monitor.system.lock")
    }
}
