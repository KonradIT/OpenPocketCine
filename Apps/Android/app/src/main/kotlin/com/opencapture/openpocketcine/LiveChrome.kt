package com.opencapture.openpocketcine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ChromeShape = RoundedCornerShape(LiveDesign.CORNER_RADIUS_DP.dp)

fun Modifier.monitorGlass(shape: RoundedCornerShape = ChromeShape): Modifier =
    clip(shape).background(LiveDesign.glass)

@Composable
fun Modifier.chromeClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return if (onLongClick != null) {
        combinedClickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
    }
}

@Composable
fun LockButton(locked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = if (locked) LiveDesign.accent else LiveDesign.text.copy(alpha = 0.86f)
    Box(
        modifier
            .size(LiveDesign.LOCK_SIZE_DP.dp)
            .monitorGlass()
            .then(
                if (locked) Modifier.border(1.5.dp, LiveDesign.accent.copy(alpha = 0.75f), ChromeShape)
                else Modifier
            )
            .chromeClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PadlockGlyph(tint = tint, filled = locked, modifier = Modifier.size(13.dp, 17.dp))
    }
}

@Composable
fun DispButton(clean: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        modifier
            .width(52.dp)
            .height(44.dp)
            .monitorGlass()
            .chromeClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(
            "DISP",
            color = if (clean) LiveDesign.text else LiveDesign.info,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                Modifier.size(width = 14.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(if (!clean) LiveDesign.info else LiveDesign.hairlineStrong),
            )
            Box(
                Modifier.size(width = 14.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(if (clean) LiveDesign.info else LiveDesign.hairlineStrong),
            )
        }
    }
}

@Composable
fun AuxCircleButton(modifier: Modifier = Modifier, onClick: () -> Unit, glyph: @Composable (Color) -> Unit) {
    Box(
        modifier
            .size(LiveDesign.AUX_SIZE_DP.dp)
            .monitorGlass(CircleShape)
            .chromeClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        glyph(LiveDesign.text.copy(alpha = 0.86f))
    }
}

@Composable
fun RecordButton(recording: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Canvas(
        modifier
            .size(LiveDesign.RECORD_SIZE_DP.dp)
            .chromeClickable(enabled = enabled, onClick = onClick),
    ) {
        val d = size.minDimension
        val center = Offset(size.width / 2, size.height / 2)
        if (recording) {
            drawCircle(LiveDesign.rec.copy(alpha = 0.28f), radius = d * 0.56f, center = center)
        }
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0.88f, 0.28f, 0.30f), LiveDesign.rec),
                    center = Offset(center.x, center.y - d / 2),
                    radius = d * (48f / 72f),
                ),
            radius = d / 2,
            center = center,
        )
        drawCircle(
            Color.White.copy(alpha = 0.17f),
            radius = d / 2 - 1.5.dp.toPx(),
            center = center,
            style = Stroke(width = 3.dp.toPx()),
        )
        if (recording) {
            val side = d * (25f / 72f)
            drawRoundRect(
                LiveDesign.rec,
                topLeft = Offset(center.x - side / 2, center.y - side / 2),
                size = Size(side, side),
                cornerRadius = CornerRadius(7.dp.toPx()),
            )
        } else {
            drawCircle(LiveDesign.rec, radius = d * (58f / 72f) / 2, center = center)
        }
    }
}

@Composable
fun BatteryPair(
    phonePercent: Int,
    phoneCharging: Boolean,
    cameraPercent: Int,
    cameraCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BatteryOutlineRow(percent = phonePercent, charging = phoneCharging, camera = false)
        BatteryOutlineRow(percent = cameraPercent, charging = cameraCharging, camera = true)
    }
}

@Composable
private fun BatteryOutlineRow(percent: Int, charging: Boolean, camera: Boolean) {
    val tint =
        when {
            percent < 0 -> LiveDesign.faint
            percent <= 15 -> LiveDesign.rec
            camera -> Color(0xFF6BCC87)
            else -> LiveDesign.text.copy(alpha = 0.85f)
        }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp), contentAlignment = Alignment.Center) {
            if (camera) CameraGlyph(LiveDesign.muted, Modifier.size(12.dp, 10.dp))
            else PhoneGlyph(LiveDesign.muted, Modifier.size(7.dp, 11.dp))
        }
        Box(Modifier.size(28.dp, 16.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 1.2.dp.toPx()
                val bodyWidth = size.width - 3.dp.toPx()
                drawRoundRect(
                    tint.copy(alpha = 0.85f),
                    topLeft = Offset.Zero,
                    size = Size(bodyWidth, size.height),
                    cornerRadius = CornerRadius(3.5.dp.toPx()),
                    style = Stroke(stroke),
                )
                drawRoundRect(
                    tint.copy(alpha = 0.85f),
                    topLeft = Offset(bodyWidth, size.height * 0.28f),
                    size = Size(2.4.dp.toPx(), size.height * 0.44f),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
            Text(
                if (percent < 0) "—" else "$percent",
                color = tint,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        if (charging) {
            Text("⚡", color = tint, fontSize = 8.sp)
        }
    }
}

@Composable
fun TimecodeReadout(timecode: String?, modifier: Modifier = Modifier) {
    val raw = timecode ?: "--:--:--:--"
    val parts = raw.split(":")
    val main = if (parts.size >= 4) parts.take(3).joinToString(":") else raw
    val frames = if (parts.size >= 4) ":${parts[3]}" else ""
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = LiveDesign.text)) { append(main) }
            withStyle(SpanStyle(color = LiveDesign.accent)) { append(frames) }
        },
        fontSize = 20.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun RecChip(recording: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.clip(CircleShape).background(LiveDesign.glass).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (recording) LiveDesign.rec else LiveDesign.faint))
        Text(
            if (recording) "REC" else "STBY",
            color = if (recording) LiveDesign.text else LiveDesign.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun ReadoutPill(
    value: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable (Color) -> Unit,
) {
    val surface =
        if (active) {
            Modifier.background(LiveDesign.accentDim, CircleShape).border(1.dp, LiveDesign.accentDim, CircleShape)
        } else {
            Modifier.clip(CircleShape).background(LiveDesign.glass)
        }
    Row(
        modifier =
            surface
                .then(
                    if (onClick != null) {
                        Modifier.chromeClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(if (active) LiveDesign.accent else LiveDesign.muted)
        Text(
            value.replace(" · ", "·"),
            color = if (active) LiveDesign.accent else LiveDesign.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

@Composable
fun ConnectionPill(bars: Int, onLongClick: () -> Unit) {
    val tint =
        when {
            bars >= 3 -> LiveDesign.good
            bars == 2 -> LiveDesign.accent
            bars == 1 -> LiveDesign.rec
            else -> LiveDesign.faint
        }
    Row(
        modifier =
            Modifier.clip(CircleShape)
                .background(LiveDesign.glass)
                .chromeClickable(onClick = {}, onLongClick = onLongClick)
                .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignalBarsGlyph(bars = bars, tint = tint)
        Text("LINK", color = LiveDesign.faint, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CaptureSettingCell(
    label: String,
    value: String,
    widest: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val labelColor = if (active) LiveDesign.accent.copy(alpha = 0.85f) else LiveDesign.faint
    val valueColor = if (active) LiveDesign.accent else LiveDesign.text
    Column(
        modifier =
            Modifier.clip(ChromeShape)
                .background(if (active) LiveDesign.accentDim else Color.Transparent)
                .chromeClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Box(contentAlignment = Alignment.Center) {
            Text(widest, color = Color.Transparent, fontSize = 19.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(value, color = valueColor, fontSize = 19.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
fun AssistToolChip(label: String, on: Boolean, enabled: Boolean, stub: Boolean, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier.clip(ChromeShape)
                .background(if (on) LiveDesign.accentDim else Color.Transparent)
                .chromeClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            label,
            color = if (on) LiveDesign.accent else LiveDesign.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
        if (stub && on) {
            Text("local", color = LiveDesign.faint, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun PadlockGlyph(tint: Color, filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.08f,
                    top = h * 0.42f,
                    right = w * 0.92f,
                    bottom = h * 0.96f,
                    radiusX = 2.dp.toPx(),
                    radiusY = 2.dp.toPx(),
                )
            )
        }
        if (filled) drawPath(body, tint)
        else drawPath(body, tint, style = Stroke(1.6.dp.toPx()))
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.04f),
            size = Size(w * 0.56f, h * 0.48f),
            style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
fun PhoneGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRoundRect(tint, style = Stroke(1.3.dp.toPx()), cornerRadius = CornerRadius(1.6.dp.toPx()))
    }
}

@Composable
fun CameraGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRoundRect(tint, style = Stroke(1.3.dp.toPx()), cornerRadius = CornerRadius(1.8.dp.toPx()))
        drawCircle(tint, radius = size.minDimension * 0.22f, style = Stroke(1.2.dp.toPx()))
    }
}

@Composable
fun GearGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.28f
        drawCircle(tint, radius = r, center = c, style = Stroke(1.6.dp.toPx()))
        for (i in 0 until 6) {
            val a = Math.toRadians((i * 60).toDouble())
            val inner = r + 1.dp.toPx()
            val outer = size.minDimension * 0.46f
            drawLine(
                tint,
                Offset(c.x + (inner * kotlin.math.cos(a)).toFloat(), c.y + (inner * kotlin.math.sin(a)).toFloat()),
                Offset(c.x + (outer * kotlin.math.cos(a)).toFloat(), c.y + (outer * kotlin.math.sin(a)).toFloat()),
                2.dp.toPx(),
                StrokeCap.Round,
            )
        }
    }
}

@Composable
fun MediaGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.76f, h * 0.56f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(1.5.dp.toPx()),
        )
        drawLine(tint, Offset(w * 0.28f, h * 0.22f), Offset(w * 0.38f, h * 0.08f), 1.5.dp.toPx(), StrokeCap.Round)
        drawLine(tint, Offset(w * 0.38f, h * 0.08f), Offset(w * 0.72f, h * 0.08f), 1.5.dp.toPx(), StrokeCap.Round)
        drawLine(tint, Offset(w * 0.72f, h * 0.08f), Offset(w * 0.82f, h * 0.22f), 1.5.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun VideoGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp, 11.dp)) {
        drawRoundRect(tint, style = Stroke(1.4.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
        val path =
            Path().apply {
                moveTo(size.width * 0.42f, size.height * 0.32f)
                lineTo(size.width * 0.68f, size.height * 0.5f)
                lineTo(size.width * 0.42f, size.height * 0.68f)
                close()
            }
        drawPath(path, tint)
    }
}

@Composable
fun ColorGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp)) {
        drawCircle(tint, radius = size.minDimension / 2, style = Stroke(1.4.dp.toPx()))
        drawCircle(tint, radius = size.minDimension * 0.18f)
    }
}

@Composable
fun SdCardGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(11.dp, 14.dp)) {
        val path =
            Path().apply {
                moveTo(size.width * 0.15f, 0f)
                lineTo(size.width * 0.62f, 0f)
                lineTo(size.width, size.height * 0.22f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                lineTo(0f, size.height * 0.18f)
                close()
            }
        drawPath(path, tint, style = Stroke(1.3.dp.toPx()))
    }
}

@Composable
fun SignalBarsGlyph(bars: Int, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp, 10.dp)) {
        val gap = 1.6.dp.toPx()
        val w = (size.width - gap * 3) / 4f
        for (i in 0 until 4) {
            val h = size.height * (0.35f + i * 0.22f)
            val color = if (i < bars) tint else tint.copy(alpha = 0.22f)
            drawRoundRect(
                color,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(0.8.dp.toPx()),
            )
        }
    }
}
