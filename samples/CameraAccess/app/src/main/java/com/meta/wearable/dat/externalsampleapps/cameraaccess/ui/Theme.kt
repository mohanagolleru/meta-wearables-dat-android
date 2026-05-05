/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Drishti's Material 3 theme wrapper.
 *
 * Maps [AppColor] tokens onto Material's [androidx.compose.material3.ColorScheme] roles
 * (primary, surface, onSurface, etc.) so any Material 3 component the SDK renders
 * (Button, NavigationBar, Snackbar, etc.) automatically picks up our palette.
 *
 * We keep [AppColor] as the canonical source of truth — Drishti screens prefer
 * `AppColor.Foo` over `MaterialTheme.colorScheme.foo` because semantic names
 * (Safe, Warning) read better than Material role names (tertiary, error).
 *
 * Wrap the root composable in [DrishtiTheme] inside MainActivity.setContent { ... }.
 */
@Composable
fun DrishtiTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = AppColor.PrimaryAccent,
        onPrimary = AppColor.Surface,
        primaryContainer = AppColor.PrimaryAccent.copy(alpha = 0.12f),
        onPrimaryContainer = AppColor.PrimaryAccent,

        secondary = AppColor.Safe,
        onSecondary = AppColor.TextPrimary, // true black on sage passes WCAG AA
        secondaryContainer = AppColor.SageSurface,
        onSecondaryContainer = AppColor.Safe,

        tertiary = AppColor.SageAmbient,
        onTertiary = AppColor.TextPrimary,

        background = AppColor.Background,
        onBackground = AppColor.TextPrimary,

        surface = AppColor.Surface,
        onSurface = AppColor.TextPrimary,
        surfaceVariant = AppColor.Background,
        onSurfaceVariant = AppColor.TextSecondary,

        error = AppColor.Critical,
        onError = AppColor.Surface,
        errorContainer = AppColor.DestructiveBackground,
        onErrorContainer = AppColor.Critical,

        outline = AppColor.Secondary,
        outlineVariant = AppColor.Secondary.copy(alpha = 0.4f),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DrishtiTypography,
        content = content,
    )
}
