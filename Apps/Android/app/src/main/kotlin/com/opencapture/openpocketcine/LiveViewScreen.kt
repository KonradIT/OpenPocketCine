package com.opencapture.openpocketcine

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.opencapture.openpocketcine.session.CameraStatus
import kotlin.math.hypot
import kotlinx.coroutines.delay

private enum class LiveAssist(val label: String, val renderable: Boolean) {
    LUT("LUT", false),
    PEAK("PEAK", false),
    FALSE("FALSE", false),
    ZEBRA("ZEBRA", false),
    WAVE("WAVE", false),
    HISTO("HISTO", false),
    GUIDES("GUIDES", true),
    GRID("GRID", true),
    CROSS("CROSS", true),
}

@Composable
fun LiveViewScreen(model: AppModel) {
    val status by model.session.status.collectAsState()
    val controlNote by model.session.controlNote.collectAsState()
    val controlBusy by model.session.controlBusy.collectAsState()
    val focusPoint by model.session.focusPoint.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    var clean by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<LiveSheet?>(null) }
    var assistOn by remember { mutableStateOf(setOf<LiveAssist>()) }
    var guideIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        model.refreshPhoneBattery()
        while (true) {
            delay(1_000)
            tick += 1
            model.refreshPhoneBattery()
        }
    }
    val locked = model.uiLocked
    val enabled = !locked && !controlBusy
    val bars =
        when {
            model.session.hasVideoFormat -> 4
            model.session.videoPackets > 0 -> 2
            else -> 1
        }
    tick

    Box(Modifier.fillMaxSize().background(LiveDesign.background)) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                model.session.attachSurface(holder.surface)
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {}

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                model.session.attachSurface(null)
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        FeedGuides(
            grid = LiveAssist.GRID in assistOn,
            crosshair = LiveAssist.CROSS in assistOn,
            guides = LiveAssist.GUIDES in assistOn,
            guideRatio = GUIDE_RATIOS[guideIndex].second,
            focus = focusPoint,
        )
        if (status.isRecording) {
            Box(
                Modifier.fillMaxSize()
                    .border(3.dp, LiveDesign.rec.copy(alpha = 0.72f))
            )
        }
        if (!model.session.hasVideoFormat) {
            Text(
                "Waiting for HEVC…",
                color = LiveDesign.muted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (!clean) {
            Box(
                Modifier.fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.verticalGradient(listOf(LiveDesign.background.copy(alpha = 0.72f), Color.Transparent))
                    )
            )
        }

        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            TopDeck(
                status = status,
                clean = clean,
                locked = locked,
                enabled = enabled,
                bars = bars,
                active = sheet,
                onLock = {
                    model.uiLocked = !model.uiLocked
                    if (model.uiLocked) model.endGimbalStick()
                },
                onSettings = { model.homePanel = AppPanel.SETTINGS },
                onOpen = { if (enabled) sheet = it },
                onDisconnect = model::disconnect,
            )
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (!clean) {
                    BatteryPair(
                        phonePercent = model.phoneBatteryPercent,
                        phoneCharging = model.phoneCharging,
                        cameraPercent = status.batteryPercent,
                        cameraCharging = status.charging,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(
                    Modifier.weight(1f).fillMaxHeight().pointerInput(locked, clean) {
                        detectTapGestures { offset ->
                            if (locked || clean) return@detectTapGestures
                            val x = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val y = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            model.tapFocus(x, y)
                        }
                    },
                )
                if (!clean) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        AuxCircleButton(onClick = { model.homePanel = AppPanel.MEDIA }) { MediaGlyph(it) }
                        RecordButton(
                            recording = status.isRecording,
                            enabled = enabled,
                            onClick = model::pressRecord,
                        )
                    }
                }
            }

            if (!clean) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DispButton(
                        clean = false,
                        onClick = { clean = true },
                        onLongClick = { showDebug = !showDebug },
                    )
                }
                controlNote?.takeIf { it.isNotEmpty() }?.let { note ->
                    Text(note, color = LiveDesign.accent, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                sheet?.let { open ->
                    LiveControlSheet(open, model, status, locked) { sheet = null }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AssistStrip(
                        on = assistOn,
                        enabled = !locked,
                        modifier = Modifier.weight(1f),
                    ) { tool ->
                        if (tool == LiveAssist.GUIDES) {
                            if (LiveAssist.GUIDES in assistOn) {
                                val next = guideIndex + 1
                                if (next >= GUIDE_RATIOS.size) {
                                    assistOn = assistOn - LiveAssist.GUIDES
                                    guideIndex = 0
                                } else {
                                    guideIndex = next
                                }
                            } else {
                                assistOn = assistOn + LiveAssist.GUIDES
                            }
                        } else {
                            assistOn = if (tool in assistOn) assistOn - tool else assistOn + tool
                        }
                    }
                    CaptureStrip(
                        status = status,
                        active = sheet,
                        enabled = enabled,
                        modifier = Modifier.weight(2f),
                        onOpen = { sheet = it },
                    )
                }
                if (showDebug) {
                    PipelineHud(model)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RecChip(status.isRecording)
                    Spacer(Modifier.weight(1f))
                    TimecodeReadout(status.timecode)
                    Spacer(Modifier.width(8.dp))
                    DispButton(clean = true, onClick = { clean = false })
                }
            }
        }

        GimbalJoystick(
            enabled = !locked && sheet == null,
            onMove = model::updateGimbalStick,
            onRelease = model::endGimbalStick,
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (clean) 28.dp else 118.dp),
        )
    }
}

@Composable
private fun GimbalJoystick(
    enabled: Boolean,
    onMove: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = 88.dp
    val knob = 36.dp
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            knobOffset = Offset.Zero
            onRelease()
        }
    }
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = "Gimbal stick" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val travel = (size.toPx() - knob.toPx()) / 2f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = knobOffset + dragAmount
                        val mag = hypot(next.x, next.y)
                        val limited =
                            if (mag > travel && mag > 0f) next * (travel / mag) else next
                        knobOffset = limited
                        val denom = if (travel > 0f) travel else 1f
                        onMove(limited.x / denom, -limited.y / denom)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onRelease()
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onRelease()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = size.toPx() / 2f - stroke / 2f,
                style = Stroke(width = stroke),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = knob.toPx() / 2f,
                center = center + knobOffset,
            )
        }
    }
}

@Composable
private fun TopDeck(
    status: CameraStatus,
    clean: Boolean,
    locked: Boolean,
    enabled: Boolean,
    bars: Int,
    active: LiveSheet?,
    onLock: () -> Unit,
    onSettings: () -> Unit,
    onOpen: (LiveSheet) -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LockButton(locked, onClick = onLock)
        Spacer(Modifier.width(10.dp))
        if (clean) {
            RecChip(status.isRecording)
            Spacer(Modifier.weight(1f))
            TimecodeReadout(status.timecode)
        } else {
            Row(
                Modifier.weight(1f).monitorGlass().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimecodeReadout(status.timecode)
                ReadoutPill(
                    status.recFormatLabel,
                    active = active == LiveSheet.FORMAT,
                    enabled = enabled,
                    onClick = { onOpen(LiveSheet.FORMAT) },
                ) { VideoGlyph(it) }
                ReadoutPill(
                    status.colorLabel,
                    active = active == LiveSheet.COLOR,
                    enabled = enabled,
                    onClick = { onOpen(LiveSheet.COLOR) },
                ) { ColorGlyph(it) }
                ReadoutPill(status.storageLabel) { SdCardGlyph(it) }
                ConnectionPill(bars, onLongClick = onDisconnect)
            }
        }
        if (!clean) {
            Spacer(Modifier.width(10.dp))
            AuxCircleButton(onClick = onSettings) { GearGlyph(it) }
        }
    }
}

@Composable
private fun AssistStrip(
    on: Set<LiveAssist>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (LiveAssist) -> Unit,
) {
    Row(
        modifier
            .height(LiveDesign.CONTROL_HEIGHT_DP.dp)
            .monitorGlass()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LiveAssist.entries.forEach { tool ->
            AssistToolChip(
                label = tool.label,
                on = tool in on,
                enabled = enabled,
                stub = !tool.renderable,
                onClick = { onToggle(tool) },
            )
        }
    }
}

@Composable
private fun CaptureStrip(
    status: CameraStatus,
    active: LiveSheet?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onOpen: (LiveSheet) -> Unit,
) {
    Row(
        modifier
            .height(LiveDesign.CONTROL_HEIGHT_DP.dp)
            .monitorGlass()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CaptureSettingCell("ISO", status.isoLabel, "25600", active == LiveSheet.ISO, enabled) { onOpen(LiveSheet.ISO) }
        CaptureSettingCell("SHUTTER", status.shutterLabel, "1/16000", active == LiveSheet.SHUTTER, enabled) {
            onOpen(LiveSheet.SHUTTER)
        }
        CaptureSettingCell("WB", status.wbLabel, "10000K", active == LiveSheet.WB, enabled) { onOpen(LiveSheet.WB) }
        CaptureSettingCell("FOCUS", status.focusLabel, "Single", active == LiveSheet.FOCUS, enabled) {
            onOpen(LiveSheet.FOCUS)
        }
        CaptureSettingCell("EXPO", status.expoLabel, "Manual", active == LiveSheet.EXPO, enabled) {
            onOpen(LiveSheet.EXPO)
        }
        CaptureSettingCell("AUDIO", status.audioLabel, "Spatial", active == LiveSheet.AUDIO, enabled) {
            onOpen(LiveSheet.AUDIO)
        }
    }
}

@Composable
private fun PipelineHud(model: AppModel) {
    Column(
        Modifier.fillMaxWidth()
            .padding(top = 8.dp)
            .monitorGlass()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HudRow("Video packets", "${model.session.videoPackets}")
        HudRow("Access units", "${model.session.accessUnits}")
        HudRow("Dropped incomplete", "${model.session.droppedIncomplete}")
        HudRow("NAL types", model.session.nalTypes.ifEmpty { "—" })
        HudRow("Format (VPS/SPS/PPS)", if (model.session.hasVideoFormat) "ready" else "waiting…")
        HudRow("Last keyframe", model.session.lastKeyframeAge)
        HudRow("Frames enqueued", "${model.session.framesEnqueued}")
        HudRow("Decoder errors", "${model.session.decoderErrors}")
    }
}

@Composable
private fun HudRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = LiveDesign.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = LiveDesign.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun FeedGuides(
    grid: Boolean,
    crosshair: Boolean,
    guides: Boolean,
    guideRatio: Float,
    focus: Pair<Float, Float>?,
) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = 1.dp.toPx()
        if (guides && guideRatio > 0f && w > 0f && h > 0f) {
            val view = w / h
            val frame =
                if (view > guideRatio) {
                    val fw = h * guideRatio
                    androidx.compose.ui.geometry.Rect((w - fw) / 2f, 0f, (w + fw) / 2f, h)
                } else {
                    val fh = w / guideRatio
                    androidx.compose.ui.geometry.Rect(0f, (h - fh) / 2f, w, (h + fh) / 2f)
                }
            val mask = Color.Black.copy(alpha = 0.45f)
            drawRect(mask, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(w, frame.top))
            drawRect(mask, topLeft = Offset(0f, frame.bottom), size = androidx.compose.ui.geometry.Size(w, h - frame.bottom))
            drawRect(mask, topLeft = Offset(0f, frame.top), size = androidx.compose.ui.geometry.Size(frame.left, frame.height))
            drawRect(
                mask,
                topLeft = Offset(frame.right, frame.top),
                size = androidx.compose.ui.geometry.Size(w - frame.right, frame.height),
            )
            drawRect(
                Color.White.copy(alpha = 0.55f),
                topLeft = Offset(frame.left, frame.top),
                size = androidx.compose.ui.geometry.Size(frame.width, frame.height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            )
        }
        if (grid) {
            val color = Color.White.copy(alpha = 0.22f)
            drawLine(color, Offset(w / 3f, 0f), Offset(w / 3f, h), stroke)
            drawLine(color, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), stroke)
            drawLine(color, Offset(0f, h / 3f), Offset(w, h / 3f), stroke)
            drawLine(color, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), stroke)
        }
        if (crosshair) {
            val color = Color.White.copy(alpha = 0.65f)
            val cx = w / 2f
            val cy = h / 2f
            val arm = 20.dp.toPx()
            drawLine(color, Offset(cx, cy - arm), Offset(cx, cy + arm), 1.4.dp.toPx())
            drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), 1.4.dp.toPx())
        }
        if (focus != null) {
            val side = minOf(w, h) * 0.14f
            val cx = focus.first * w
            val cy = focus.second * h
            drawRect(
                LiveDesign.accent,
                topLeft = Offset(cx - side / 2f, cy - side / 2f),
                size = androidx.compose.ui.geometry.Size(side, side),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}

private val GUIDE_RATIOS =
    listOf(
        "2.39:1" to 2.39f,
        "1.85:1" to 1.85f,
        "16:9" to (16f / 9f),
        "1:1" to 1f,
        "4:5" to (4f / 5f),
        "9:16" to (9f / 16f),
    )
