package com.opencapture.openpocketcine.bridge

/**
 * JNI binding to `libOpenPocketCineAndroid.so` — OpenPocketViewCore plus
 * `OpenPocketCineAndroidFacade`. Staged by `:app:stageSwiftCore` / `just android-core`.
 */
object SwiftCore {
    val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("OpenPocketCineAndroid")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    const val CMD_SESSION_WAKE = 1
    const val CMD_SESSION_KEEPALIVE = 2
    const val CMD_SET_PAIRING_PIN = 3
    const val CMD_PAIR_APPROVAL_ACK = 4
    const val CMD_SESSION_5310 = 5
    const val CMD_GET_WIFI_SSID = 6
    const val CMD_GET_WIFI_PASSWORD = 7
    const val CMD_APP_DEVICE_INFO = 8
    const val CMD_APP_PRESENCE = 9
    const val CMD_GIMBAL_INIT = 10
    const val CMD_SUBSCRIBE = 11
    const val CMD_ENTER_PLAYBACK = 12
    const val CMD_LIVE_VIEW_ENABLE = 13
    const val CMD_RECORD_START = 14
    const val CMD_RECORD_STOP = 15
    const val CMD_SET_EXPO_MODE = 16
    const val CMD_SET_SHUTTER = 17
    const val CMD_SET_ISO_INDEX = 18
    const val CMD_SET_COLOR_MODE = 19
    const val CMD_SET_FOCUS_MODE = 20
    const val CMD_SET_WB_AUTO = 21
    const val CMD_SET_WB_CUSTOM = 22
    const val CMD_GET_AUDIO_CHANNEL = 23
    const val CMD_SET_AUDIO_CHANNEL = 24
    const val CMD_GET_VOCAL_BOOST = 25
    const val CMD_SET_VOCAL_BOOST = 26
    const val CMD_AUDIO_DSP_GET = 27
    const val CMD_AUDIO_DSP_SET = 28
    const val CMD_AUDIO_DSP_PATCH_WIND = 29
    const val CMD_AUDIO_DSP_PATCH_DIRECTIONAL = 30
    const val CMD_SET_VIDEO_FORMAT = 31
    const val CMD_TAP_FOCUS_PREPARE = 32
    const val CMD_TAP_FOCUS_POINT = 33
    const val CMD_TAP_FOCUS_HINT = 34
    const val CMD_TAP_FOCUS_COMMIT = 35

    /** DUML set/cmd key the camera ACKs for [kind]. */
    fun waitKey(kind: Int): Int =
        when (kind) {
            CMD_RECORD_START, CMD_RECORD_STOP -> 0x0202
            CMD_SET_EXPO_MODE -> 0x021E
            CMD_SET_SHUTTER -> 0x0228
            CMD_SET_ISO_INDEX -> 0x022A
            CMD_SET_COLOR_MODE -> 0x0242
            CMD_SET_FOCUS_MODE -> 0x0224
            CMD_SET_WB_AUTO, CMD_SET_WB_CUSTOM -> 0x022C
            CMD_GET_AUDIO_CHANNEL, CMD_SET_AUDIO_CHANNEL,
            CMD_GET_VOCAL_BOOST, CMD_SET_VOCAL_BOOST,
            -> 0x028E
            CMD_AUDIO_DSP_GET -> 0x02A0
            CMD_AUDIO_DSP_SET, CMD_AUDIO_DSP_PATCH_WIND, CMD_AUDIO_DSP_PATCH_DIRECTIONAL -> 0x029F
            CMD_SET_VIDEO_FORMAT -> 0x0218
            CMD_TAP_FOCUS_PREPARE -> 0x0222
            CMD_TAP_FOCUS_POINT -> 0x0230
            CMD_TAP_FOCUS_HINT -> 0x0268
            CMD_TAP_FOCUS_COMMIT -> 0x0232
            else -> 0
        }

    const val FLAG_REQUEST = 0x40
    const val FLAG_RESPONSE = 0xC0
    const val FLAG_NOTIFY = 0x00
    const val SENDER_APP = 0x02
    const val RX_GIMBAL = 0x04

    external fun coreVersion(): String

    external fun encodeDuml(
        sender: Int,
        receiver: Int,
        seq: Int,
        flags: Int,
        cmdSet: Int,
        cmdId: Int,
        payload: ByteArray?,
    ): ByteArray?

    external fun scanDuml(data: ByteArray): ByteArray?

    external fun unpackStatusString(payload: ByteArray): String?

    external fun encodeCommand(kind: Int, seq: Int, extra: String?): ByteArray?

    external fun bleAdvertModelId(payload: ByteArray): Int

    external fun resolveCameraModel(modelId: Int, name: String?): String?

    external fun transportHeader(pktType: Int, payloadLen: Int, sessionId: Int, seq: Int): ByteArray?

    external fun routingHeader(seq: Int, cmdCounter: Int, drone: Boolean): ByteArray?

    external fun handshakePayload(baseSeq: Int): ByteArray?

    external fun ackPayload(peerCursor: Int, baseSeq: Int): ByteArray?

    external fun transportSeq(datagram: ByteArray): Int

    external fun applyStatus(cmdSet: Int, cmdId: Int, payload: ByteArray, previousJSON: String): String?

    external fun hevcCsd(annexB: ByteArray): ByteArray?

    external fun hevcNalTypes(annexB: ByteArray): String?

    external fun hevcIsKeyframe(annexB: ByteArray): Boolean

    external fun depacketizerCreate(): Long

    external fun depacketizerFeed(handle: Long, payload: ByteArray): ByteArray?

    external fun depacketizerDropped(handle: Long): Int

    external fun depacketizerReset(handle: Long)

    external fun depacketizerDestroy(handle: Long)

    fun command(kind: Int, seq: Int = 0, extra: String? = null): ByteArray {
        check(isAvailable) { "Swift core is not loaded" }
        return encodeCommand(kind, seq and 0xFFFF, extra)
            ?: error("encodeCommand($kind) returned null")
    }

    fun subscribe(key: String, subId: Long, seq: Int = 0): ByteArray =
        command(CMD_SUBSCRIBE, seq, "$key\u001f$subId")
}
