package com.opencapture.openpocketcine.session

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.opencapture.openpocketcine.bridge.SwiftCore
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Best-effort HEVC decode via MediaCodec. Access units come from the Swift
 * depacketizer (Annex-B, DJI marker already stripped). iOS uses VideoToolbox.
 */
class HevcDecoder {
    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    private var configured = false
    private var outputThread: Thread? = null
    @Volatile private var running = false
    @Volatile var hasFormat = false
        private set
    var nalTypesSeen = ""
        private set
    var lastKeyframeAt: Long? = null
        private set
    val decoderErrors = AtomicInteger(0)
    val framesEnqueued = AtomicInteger(0)

    fun attachSurface(next: Surface?) {
        if (surface == next) return
        reset()
        surface = next
    }

    fun decode(accessUnit: ByteArray): Boolean {
        if (!SwiftCore.isAvailable) return false
        val types = SwiftCore.hevcNalTypes(accessUnit).orEmpty()
        if (types.isNotEmpty()) nalTypesSeen = mergeTypes(nalTypesSeen, types)
        if (SwiftCore.hevcIsKeyframe(accessUnit)) lastKeyframeAt = System.currentTimeMillis()
        if (!configured) {
            val csd = SwiftCore.hevcCsd(accessUnit) ?: return false
            return configure(csd) && queue(accessUnit)
        }
        return queue(accessUnit)
    }

    fun reset() {
        running = false
        outputThread?.interrupt()
        outputThread = null
        configured = false
        hasFormat = false
        nalTypesSeen = ""
        lastKeyframeAt = null
        framesEnqueued.set(0)
        decoderErrors.set(0)
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
    }

    private fun configure(csd: ByteArray): Boolean {
        val target = surface ?: return false
        return try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 1920, 1080)
            format.setByteBuffer("csd-0", ByteBuffer.wrap(csd))
            val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            decoder.configure(format, target, null, 0)
            decoder.start()
            codec = decoder
            configured = true
            running = true
            outputThread =
                Thread(
                    {
                        val info = MediaCodec.BufferInfo()
                        while (running) {
                            val index =
                                try {
                                    decoder.dequeueOutputBuffer(info, 10_000)
                                } catch (_: Exception) {
                                    break
                                }
                            when {
                                index >= 0 -> runCatching { decoder.releaseOutputBuffer(index, true) }
                                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> hasFormat = true
                            }
                        }
                    },
                    "opc.hevc.out",
                ).also { it.isDaemon = true; it.start() }
            true
        } catch (e: Exception) {
            decoderErrors.incrementAndGet()
            Log.w(TAG, "HEVC configure failed", e)
            false
        }
    }

    private fun queue(accessUnit: ByteArray): Boolean {
        val decoder = codec ?: return false
        return try {
            // Drop-newest when the decoder is behind — do not block the datalink thread 10 ms.
            val index = decoder.dequeueInputBuffer(0)
            if (index < 0) return false
            val buffer = decoder.getInputBuffer(index) ?: return false
            buffer.clear()
            buffer.put(accessUnit)
            decoder.queueInputBuffer(index, 0, accessUnit.size, System.nanoTime() / 1000, 0)
            framesEnqueued.incrementAndGet()
            true
        } catch (e: Exception) {
            decoderErrors.incrementAndGet()
            Log.w(TAG, "HEVC queue failed", e)
            false
        }
    }

    private fun mergeTypes(existing: String, incoming: String): String {
        val set = existing.split(',').filter { it.isNotBlank() }.toMutableSet()
        set.addAll(incoming.split(',').filter { it.isNotBlank() })
        return set.sorted().joinToString(",")
    }

    companion object {
        private const val TAG = "HevcDecoder"
    }
}
