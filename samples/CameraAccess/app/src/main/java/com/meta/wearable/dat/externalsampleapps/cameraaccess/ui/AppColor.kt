/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.ui.graphics.Color

object AppColor {
  // Drishti palette — warm cream/coral (Claude & Function Health inspired)
  val Background = Color(0xFFF4F3EE)          // warm cream
  val Surface = Color(0xFFFFFFFF)             // white cards
  val PrimaryAccent = Color(0xFFD4725C)       // warm coral (buttons, orb)
  val PrimaryAccentLight = Color(0xFFE8896F)  // lighter coral for gradient center
  val PrimaryAccentDark = Color(0xFFC4604A)   // deeper coral for gradient edge
  val Secondary = Color(0xFFB1ADA1)           // warm gray (borders, inactive)
  val TextPrimary = Color(0xFF1A1A1A)         // near-black
  val TextSecondary = Color(0xFF8A8580)        // warm gray captions
  val Safe = Color(0xFF5D9B76)               // sage green (confirmed)
  val Warning = Color(0xFFD4943A)            // warm amber (caution)
  val Critical = Color(0xFFC9504B)           // soft red (danger only)

  // Legacy aliases — keep for compatibility with existing code
  val Green = Safe
  val Red = Critical
  val Yellow = Warning
  val DeepBlue = PrimaryAccent
  val DestructiveBackground = Color(0xFFFED7D5)
  val DestructiveForeground = Critical
}
