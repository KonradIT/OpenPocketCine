package com.opencapture.openpocketcine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencapture.openpocketcine.session.CameraCommands
import com.opencapture.openpocketcine.session.CameraStatus

enum class LiveSheet {
    ISO,
    SHUTTER,
    WB,
    FOCUS,
    EXPO,
    AUDIO,
    COLOR,
    FORMAT,
}

@Composable
fun LiveControlSheet(
    sheet: LiveSheet,
    model: AppModel,
    status: CameraStatus,
    locked: Boolean,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(sheet) {
        if (sheet == LiveSheet.AUDIO) model.refreshAudio()
    }
    Column(
        Modifier.fillMaxWidth()
            .monitorGlass()
            .border(1.dp, LiveDesign.hairline, ChromeShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(sheetTitle(sheet), color = LiveDesign.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                "Done",
                color = LiveDesign.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.chromeClickable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        when (sheet) {
            LiveSheet.ISO -> IsoSheet(model, status, locked)
            LiveSheet.SHUTTER -> ShutterSheet(model, status, locked)
            LiveSheet.WB -> WbSheet(model, status, locked)
            LiveSheet.FOCUS -> FocusSheet(model, status, locked)
            LiveSheet.EXPO -> ExpoSheet(model, status, locked)
            LiveSheet.AUDIO -> AudioSheet(model, status, locked)
            LiveSheet.COLOR -> ColorSheet(model, status, locked)
            LiveSheet.FORMAT -> FormatSheet(model, status, locked)
        }
    }
}

@Composable
private fun IsoSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    Text("Current  ${status.isoLabel}", color = LiveDesign.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    ChipRow {
        val choices =
            if (status.availableIsoIndices.isNotEmpty()) {
                CameraCommands.isoChoices(status.colorMode).filter { it.first in status.availableIsoIndices }
                    .ifEmpty { status.availableIsoIndices.map { it to CameraCommands.isoLabel(it) } }
            } else {
                CameraCommands.isoChoices(status.colorMode)
            }
        choices.forEach { (index, label) ->
            ChoiceChip(
                CameraCommands.isoChipLabel(label, status.colorMode),
                status.isoIndex == index,
                !locked,
            ) { model.setIsoIndex(index) }
        }
    }
}

@Composable
private fun ShutterSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    Text("Current  ${status.shutterLabel}", color = LiveDesign.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    ChipRow {
        CameraCommands.shutterWheelDenoms(status.availableShutterDenoms, status.shutterDenom).forEach { denom ->
            ChoiceChip("1/$denom", status.shutterDenom == denom, !locked) { model.setShutterDenom(denom) }
        }
    }
}

@Composable
private fun WbSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    Text("Current  ${status.wbLabel}", color = LiveDesign.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    ChipRow {
        ChoiceChip("Auto", status.wbMode == CameraCommands.WB_AUTO, !locked) { model.setWhiteBalanceAuto() }
        CameraCommands.kelvinPresets.forEach { k ->
            ChoiceChip("${k}K", status.wbMode == CameraCommands.WB_CUSTOM && status.wbKelvin == k, !locked) {
                model.setWhiteBalance(k, status.wbTint)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tint", color = LiveDesign.faint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        ChoiceChip("−", false, !locked && status.wbMode != CameraCommands.WB_AUTO) {
            val k = if (status.wbKelvin > 0) status.wbKelvin else 5600
            model.setWhiteBalance(k, (status.wbTint - 5).coerceIn(-100, 100))
        }
        Text(
            "${status.wbTint}",
            color = LiveDesign.text,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
        )
        ChoiceChip("+", false, !locked && status.wbMode != CameraCommands.WB_AUTO) {
            val k = if (status.wbKelvin > 0) status.wbKelvin else 5600
            model.setWhiteBalance(k, (status.wbTint + 5).coerceIn(-100, 100))
        }
    }
}

@Composable
private fun FocusSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    ChipRow {
        ChoiceChip("Single", status.focusMode == CameraCommands.FOCUS_SINGLE, !locked) {
            model.setFocusMode(false)
        }
        ChoiceChip("Continuous", status.focusMode == CameraCommands.FOCUS_CONTINUOUS, !locked) {
            model.setFocusMode(true)
        }
    }
}

@Composable
private fun ExpoSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    ChipRow {
        ChoiceChip("Auto", status.expoMode == CameraCommands.EXPO_AUTO, !locked) { model.setExpoMode(false) }
        ChoiceChip("Manual", status.expoMode == CameraCommands.EXPO_MANUAL, !locked) { model.setExpoMode(true) }
    }
}

@Composable
private fun AudioSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    SectionLabel("Channel")
    ChipRow {
        ChoiceChip("Stereo", status.audioChannel == CameraCommands.AUDIO_STEREO, !locked) {
            model.setAudioChannel(CameraCommands.AUDIO_STEREO)
        }
        ChoiceChip("Mono", status.audioChannel == CameraCommands.AUDIO_MONO, !locked) {
            model.setAudioChannel(CameraCommands.AUDIO_MONO)
        }
        ChoiceChip("Spatial", status.audioChannel == CameraCommands.AUDIO_SPATIAL, !locked) {
            model.setAudioChannel(CameraCommands.AUDIO_SPATIAL)
        }
    }
    SectionLabel("Wind NR")
    ChipRow {
        ChoiceChip("On", status.windNr == 1, !locked) { model.setWindNr(true) }
        ChoiceChip("Off", status.windNr == 0, !locked) { model.setWindNr(false) }
    }
    SectionLabel("Directional")
    ChipRow {
        ChoiceChip("All", status.directionalAudio == 0, !locked) { model.setDirectionalAudio(0) }
        ChoiceChip("Front", status.directionalAudio == 1, !locked) { model.setDirectionalAudio(1) }
        ChoiceChip("Front + back", status.directionalAudio == 2, !locked) { model.setDirectionalAudio(2) }
    }
    SectionLabel("Vocal Boost")
    ChipRow {
        ChoiceChip("On", status.vocalBoost == 1, !locked) { model.setVocalBoost(true) }
        ChoiceChip("Off", status.vocalBoost == 0, !locked) { model.setVocalBoost(false) }
    }
}

@Composable
private fun ColorSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    ChipRow {
        listOf(
            CameraCommands.COLOR_NORMAL to "Normal",
            CameraCommands.COLOR_HDR to "HDR",
            CameraCommands.COLOR_DLOG to "D-Log",
            CameraCommands.COLOR_DLOG2 to "D-Log2",
        ).forEach { (mode, label) ->
            ChoiceChip(label, status.colorMode == mode, !locked) { model.setColorMode(mode) }
        }
    }
}

@Composable
private fun FormatSheet(model: AppModel, status: CameraStatus, locked: Boolean) {
    val res = if (status.resolutionCode == CameraCommands.RES_4K) CameraCommands.RES_4K else CameraCommands.RES_1080
    val fpsIdx = if (status.fpsIndex > 0) status.fpsIndex else 1
    SectionLabel("Resolution")
    ChipRow {
        ChoiceChip("1080p", res == CameraCommands.RES_1080, !locked) {
            model.setResolutionFps(CameraCommands.RES_1080, fpsIdx)
        }
        ChoiceChip("4K", res == CameraCommands.RES_4K, !locked) {
            model.setResolutionFps(CameraCommands.RES_4K, fpsIdx)
        }
    }
    SectionLabel("Frame rate")
    ChipRow {
        listOf(1 to 24, 2 to 25, 3 to 30, 4 to 48, 5 to 50, 6 to 60).forEach { (idx, fps) ->
            ChoiceChip("$fps", status.fpsIndex == idx || (status.fps == fps && status.fpsIndex <= 0), !locked) {
                model.setResolutionFps(res, idx)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = LiveDesign.faint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) LiveDesign.accent else LiveDesign.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier.clip(ChromeShape)
                .background(if (selected) LiveDesign.accentDim else LiveDesign.surface)
                .chromeClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun sheetTitle(sheet: LiveSheet): String =
    when (sheet) {
        LiveSheet.ISO -> "ISO"
        LiveSheet.SHUTTER -> "Shutter"
        LiveSheet.WB -> "White balance"
        LiveSheet.FOCUS -> "Focus"
        LiveSheet.EXPO -> "Exposure"
        LiveSheet.AUDIO -> "Audio"
        LiveSheet.COLOR -> "Color"
        LiveSheet.FORMAT -> "Rec"
    }
