/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Drishti audio-reactive orb — the warm focal point that pops against sage.
 *
 * Pure warm gradient (no sage in the orb itself — sage is the page bg, the orb
 * is the warm hero on top). Audio amplitude only pulls the gradient brighter:
 *   - Idle (audioLevel ≈ 0): soft peach core → mid-orange edge. Calm, warm.
 *   - Peak (audioLevel = 1): bright orange core → deep coral edge. Punchy.
 *
 * Note: [audioLevel] is the *captured* mic amplitude
 * ([com.meta.wearable.dat.externalsampleapps.cameraaccess.stream.BluetoothAudioBridge.currentRmsLevel]).
 * It rises when the user speaks; it does NOT measure Gemini's TTS playback
 * coming back through the glasses speakers.
 *
 * Motion is unchanged from the legacy orb:
 *   - Continuous breathing pulse (0.94 → 1.0) when idle
 *   - Audio-reactive scale (1.0 → 1.4) driven by RMS amplitude
 *   - Multi-layered draw: shadow + outer aura + middle glow + inner core + specular highlight
 *
 * @param audioLevel normalised amplitude in 0..1 (0 = silence, 1 = max)
 * @param modifier   optional [Modifier] — default size is 150 dp
 */
@Composable
fun VoiceOrb(
    audioLevel: Float,
    modifier: Modifier = Modifier,
) {
    // ── Warm-on-warm intensity blend ───────────────────────────────
    // At rest the orb is soft peach; audio peaks brighten it toward saturated orange.
    val blend = audioLevel.coerceIn(0f, 1f)

    val coreColor: Color = lerp(AppColor.PrimaryAccentLight, AppColor.PrimaryAccent, blend)
    val coreLightColor: Color = lerp(
        start = AppColor.WarmSurface,
        stop = AppColor.PrimaryAccentLight,
        fraction = blend,
    )
    val coreDarkColor: Color = lerp(
        start = AppColor.PrimaryAccent,
        stop = AppColor.PrimaryAccentDark,
        fraction = blend,
    )

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

        // 1. Shadow layer — faint circle offset downward for depth/lift
        drawCircle(
            color = coreColor.copy(alpha = 0.06f),
            radius = baseRadius * scale,
            center = Offset(center.x, center.y + 6.dp.toPx()),
        )

        // 2. Outer pulse aura — very faint, expands the most
        drawCircle(
            color = coreColor.copy(alpha = 0.04f),
            radius = baseRadius * scale * 1.8f,
            center = center,
        )

        // 3. Middle glow — radial gradient from core color to transparent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                center = center,
                radius = baseRadius * scale * 1.3f,
            ),
            radius = baseRadius * scale * 1.3f,
            center = center,
        )

        // 4. Inner core — radial gradient from light → dark
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreLightColor, coreDarkColor),
                center = center,
                radius = baseRadius * scale,
            ),
            radius = baseRadius * scale,
            center = center,
            alpha = 0.92f,
        )

        // 5. Bright center dot — white specular highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = baseRadius * scale * 0.3f,
            center = center,
        )
    }
}
