package com.opencapture.openpocketcine.core

/**
 * Operator-visible stage of a Pocket connection. Mirrors Swift
 * `ConnectionPhase` in OpenPocketViewCore — BLE → Wi-Fi → datalink.
 */
public enum class ConnectionPhase {
    IDLE,
    SCANNING,
    CONNECTING_GATT,
    PAIRING,
    AWAITING_APPROVAL,
    READING_WIFI_CREDS,
    JOINING_WIFI,
    OPENING_DATALINK,
    LIVE,
    FAILED,
}

/**
 * Thin camera-session seam. The Compose app implements this over BLE / Wi-Fi /
 * UDP plus the Swift JNI facade — Kotlin does not pack DUML bytes.
 */
public interface CameraSession {
    public val phase: ConnectionPhase
    public fun startScan()
    public fun disconnect()
}
