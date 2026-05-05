/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Drishti's medication pill icon — replaces flat status dots with proper
 * 3D-feeling pill visuals, ported from the Pillory prototype's Pill component.
 *
 * Two shapes:
 *  - [PillKind.Tablet]  — round tablet with radial gradient (brighter top-left,
 *                         shadow bottom-right) and a faint center pill-line
 *  - [PillKind.Capsule] — oblong capsule, color on left half, white on right half
 *
 * "Taken" state fades the icon to 40% so it reads as completed without removing it.
 */
enum class PillKind { Tablet, Capsule }

@Composable
fun PillIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    kind: PillKind = PillKind.Tablet,
    taken: Boolean = false,
) {
    val highlightColor = remember(color) { lerp(color, Color.White, 0.35f) }
    val shadowColor = remember(color) { lerp(color, Color.Black, 0.18f) }
    val alpha = if (taken) 0.4f else 1f

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            when (kind) {
                PillKind.Tablet -> drawTablet(highlightColor, color, shadowColor, alpha)
                PillKind.Capsule -> drawCapsule(color, alpha)
            }
        }
    }
}

/** Round tablet: radial gradient + faint center seam line. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTablet(
    highlight: Color,
    base: Color,
    shadow: Color,
    alpha: Float,
) {
    val radius = this.size.minDimension / 2f
    val center = Offset(this.size.width / 2f, this.size.height / 2f)

    // Radial gradient: highlight at upper-left, base mid, shadow lower-right
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(highlight, base, shadow),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
        alpha = alpha,
    )

    // Center seam — the line where two halves of a tablet meet
    drawLine(
        color = Color.Black.copy(alpha = 0.18f * alpha),
        start = Offset(center.x - radius * 0.65f, center.y),
        end = Offset(center.x + radius * 0.65f, center.y),
        strokeWidth = 1.5f,
    )
}

/** Oblong capsule: color left half, white right half, with seam. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCapsule(
    color: Color,
    alpha: Float,
) {
    val w = this.size.width
    val h = this.size.height * 0.62f
    val r = h / 2f
    val topY = (this.size.height - h) / 2f

    // Left half (color)
    drawArc(
        color = color.copy(alpha = alpha),
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(0f, topY),
        size = Size(h, h),
    )
    drawRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(r, topY),
        size = Size(w / 2f - r, h),
    )

    // Right half (white-ish)
    drawRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(w / 2f, topY),
        size = Size(w / 2f - r, h),
    )
    drawArc(
        color = Color.White.copy(alpha = alpha),
        startAngle = 270f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(w - h, topY),
        size = Size(h, h),
    )

    // Subtle outline
    drawArc(
        color = Color.Black.copy(alpha = 0.15f * alpha),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(0f, topY),
        size = Size(w, h),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
    )
}
