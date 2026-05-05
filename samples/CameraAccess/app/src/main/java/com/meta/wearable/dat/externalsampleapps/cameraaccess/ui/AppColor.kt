/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.ui.graphics.Color

/**
 * Drishti palette — Pillory-style sage atmosphere with warm-orb focal points.
 *
 * Identity:
 *  - Page atmosphere: SAGE GREEN. Calm, medical, trustworthy. The "field."
 *  - Cards: warm CREAM. Float on sage; legible. The "content."
 *  - Orb + CTAs: WARM ORANGE. The "focal point" that pops against sage.
 *  - Hero accents: soft PEACH (orb-tinted). Echoes the orb across the UI.
 *  - Safe sage: tiny ✓ marks only. Semantic green for "done."
 *  - Warning amber + Critical red: alerts only, nothing else.
 *
 * The references this is modeled on:
 *  - Pillory (Refs 1, 2, 5): sage page + cream cards + orange "Taken" CTA
 *  - Amoxicillin detail: sage hero + 3D warm pill + orange "Taken" — the orb
 *    is to Drishti what the 3D pill is to Pillory: the warm focal hero on sage.
 *
 * Token names are semantic — change hex values to re-skin. New code prefers
 * `Background`, `Surface`, `PrimaryAccent` etc. over Material role names.
 */
object AppColor {
  // ── Atmosphere (Pillory prototype values — deeper, more characterful) ───
  val Background = Color(0xFFA6AE7E)            // deeper olive page bg (Pillory THEMES.olive.bg)
  val Surface = Color(0xFFF5EFE0)               // warm cream card (Pillory THEMES.olive.card)

  // ── Warm orb family — Drishti's focal warmth ────────────────────────────
  val PrimaryAccent = Color(0xFFE85D2F)         // punchier red-orange (Pillory THEMES.olive.accent)
  val PrimaryAccentLight = Color(0xFFF5A86F)    // soft peach (Pillory accentSoft)
  val PrimaryAccentDark = Color(0xFFB94720)     // deeper red-orange (orb depth)

  // Soft warm surfaces — only for avatar / portrait stand-ins
  val WarmSurface = Color(0xFFF7E2CE)           // soft peach (used sparingly)
  val WarmAmbient = Color(0xFFF2C4A5)           // medium peach (avatar bg)

  // ── Pill colors (medication visualization, Pillory-style) ──────────────
  val PillGreen = Color(0xFF9BCB7E)             // sage-green tablet/capsule (vitamins, supplements)
  val PillPink = Color(0xFFF2B6C0)              // rose-pink tablet (antibiotics)
  val PillYellow = Color(0xFFF2C94C)            // amber tablet (Omega-3, oils)

  // ── Sage family (semantic green only — small accents) ───────────────────
  val Safe = Color(0xFF5D9B76)                  // ✓ check icons, "Done" status text
  val SageAmbient = Color(0xFF8E9A66)           // deeper olive (chip/striped surfaces)
  val SageSurface = Color(0xFFD4DDC0)           // soft sage tint (rarely used)

  // ── Neutrals ────────────────────────────────────────────────────────────
  val Secondary = Color(0xFF5C6443)             // chip-dim text / borders (Pillory chipDimText)
  val TextPrimary = Color(0xFF1A1A1A)           // primary text — near-black
  val TextSecondary = Color(0xFF3D3D2E)         // captions: dark olive-gray (Pillory textSoft)

  // ── Status colors ───────────────────────────────────────────────────────
  val Warning = Color(0xFFC8862F)               // drug interaction warnings
  val Critical = Color(0xFFB84B47)              // errors only

  // ── Destructive snackbar pair ───────────────────────────────────────────
  val DestructiveBackground = Color(0xFFF5DAD7)
  val DestructiveForeground = Critical

  // ── Legacy aliases — keep so existing code compiles ─────────────────────
  val Green = Safe
  val Red = Critical
  val Yellow = Warning
  val DeepBlue = PrimaryAccent
}
