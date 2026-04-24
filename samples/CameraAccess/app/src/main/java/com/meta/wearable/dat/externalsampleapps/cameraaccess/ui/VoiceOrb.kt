package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium audio-reactive animated orb for MedSpect voice assistant.
 *
 * Renders a multi-layered coral orb with radial gradients, a shadow layer,
 * a specular highlight, and smooth spring-based audio reactivity.
 *
 * @param audioLevel normalised amplitude in 0 .. 1 (0 = silence, 1 = max)
 * @param modifier   optional [Modifier] — default size is 150 dp.
 */
@Composable
fun VoiceOrb(
    audioLevel: Float,
    modifier: Modifier = Modifier,
) {
    val coral: Color = AppColor.PrimaryAccent
    val coralLight: Color = AppColor.PrimaryAccentLight
    val coralDark: Color = AppColor.PrimaryAccentDark

    // ── Idle breathing pulse ───────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingPulse",
    )

    // ── Audio-reactive scale ───────────────────────────────────────
    val audioScale by animateFloatAsState(
        targetValue = 1.0f + audioLevel.coerceIn(0f, 1f) * 0.4f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 250f),
        label = "audioScale",
    )

    // Final scale: use the breathing pulse when idle, audio scale when active.
    val scale = maxOf(breathingPulse, audioScale)

    // ── Draw ───────────────────────────────────────────────────────
    Canvas(modifier = modifier.size(150.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2f

        // 1. Shadow layer — coral circle offset downward for depth/lift
        drawCircle(
            color = coral.copy(alpha = 0.06f),
            radius = baseRadius * scale,
            center = Offset(center.x, center.y + 6.dp.toPx()),
        )

        // 2. Outer pulse aura — very faint, expands the most
        drawCircle(
            color = coral.copy(alpha = 0.04f),
            radius = baseRadius * scale * 1.8f,
            center = center,
        )

        // 3. Middle glow — radial gradient from coral to transparent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coral.copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                center = center,
                radius = baseRadius * scale * 1.3f,
            ),
            radius = baseRadius * scale * 1.3f,
            center = center,
        )

        // 4. Inner core — radial gradient from light coral to dark coral
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coralLight, coralDark),
                center = center,
                radius = baseRadius * scale,
            ),
            radius = baseRadius * scale,
            center = center,
            alpha = 0.90f,
        )

        // 5. Bright center dot — white specular highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = baseRadius * scale * 0.3f,
            center = center,
        )
    }
}
