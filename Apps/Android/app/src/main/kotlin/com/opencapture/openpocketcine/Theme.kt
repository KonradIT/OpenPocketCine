package com.opencapture.openpocketcine

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BrandColors {
    val accent: Color = Color(0xFF00A3E0)
    val background: Color = Color(20 / 255f, 20 / 255f, 20 / 255f)
    val backgroundDeep: Color = Color(14 / 255f, 14 / 255f, 14 / 255f)
    val surface: Color = Color(28 / 255f, 28 / 255f, 28 / 255f)
    val ink: Color = Color.White
    val muted: Color = Color(160 / 255f, 165 / 255f, 165 / 255f)
}

object LiveDesign {
    val background = Color(20 / 255f, 20 / 255f, 20 / 255f)
    val surface = Color(28 / 255f, 28 / 255f, 28 / 255f)
    val glass = Color(20 / 255f, 20 / 255f, 20 / 255f, 0.64f)
    val glassOpaque = Color(20 / 255f, 20 / 255f, 20 / 255f, 0.90f)
    val text = Color.White
    val muted = Color(160 / 255f, 165 / 255f, 165 / 255f)
    val faint = Color(94 / 255f, 98 / 255f, 98 / 255f)
    val accent = Color(0f, 163 / 255f, 230 / 255f)
    val good = Color(0.18f, 0.78f, 0.42f)
    val rec = Color(0.82f, 0.20f, 0.23f)
    val info = Color(0.10f, 0.58f, 0.98f)
    val accentDim = Color(0f, 163 / 255f, 230 / 255f, 0.16f)
    val hairlineStrong = Color(94 / 255f, 98 / 255f, 98 / 255f, 0.70f)
    val hairline = Color(94 / 255f, 98 / 255f, 98 / 255f, 0.45f)
    const val CORNER_RADIUS_DP = 16f
    const val CONTROL_HEIGHT_DP = 58f
    const val LOCK_SIZE_DP = 44f
    const val RECORD_SIZE_DP = 64f
    const val AUX_SIZE_DP = 44f
}

@Composable
fun OpenPocketCineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = BrandColors.accent,
                background = BrandColors.backgroundDeep,
                surface = BrandColors.backgroundDeep,
                onBackground = BrandColors.ink,
                onSurface = BrandColors.ink,
            ),
        content = content,
    )
}
