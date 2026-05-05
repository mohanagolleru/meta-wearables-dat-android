/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Drishti's primary CTA — a warm-gradient pill that feels like a moment.
 *
 * Why a custom Box instead of [androidx.compose.material3.Button]:
 * Material 3 Button accepts `containerColor: Color`, not `Brush`. The orb-family
 * gradient (peach top → orange bottom) requires a brush, so we wrap our own.
 *
 * Visual language:
 *  - Vertical gradient mirrors the orb's light-on-top / deeper-at-base feel.
 *  - 4 dp shadow gives the button physical lift against the sage page.
 *  - 60 dp height + 18 dp radius = generous, premium touch target.
 *
 * Destructive variant uses a flat soft-red bg (no gradient — destructive
 * actions should not look "premium" or inviting).
 */
@Composable
fun SwitchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(18.dp)

    val backgroundModifier = when {
        !enabled -> Modifier.background(AppColor.Secondary.copy(alpha = 0.3f), shape)
        isDestructive -> Modifier.background(AppColor.DestructiveBackground, shape)
        else -> Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(AppColor.PrimaryAccentLight, AppColor.PrimaryAccent),
            ),
            shape = shape,
        )
    }

    val contentColor = when {
        !enabled -> AppColor.TextSecondary
        isDestructive -> AppColor.DestructiveForeground
        else -> Color.White
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = shape, clip = false)
            .clip(shape)
            .then(backgroundModifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
