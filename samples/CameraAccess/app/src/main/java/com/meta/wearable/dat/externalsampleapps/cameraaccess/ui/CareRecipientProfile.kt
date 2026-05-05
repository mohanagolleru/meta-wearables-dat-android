/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// ── Profile Screen ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareRecipientProfile(
    modifier: Modifier = Modifier,
    careRecipient: CareRecipient = DemoData.careRecipient,
    medications: List<Medication> = DemoData.medications,
    conditions: List<Condition> = DemoData.conditions,
    allergies: List<Allergen> = DemoData.allergies,
    emergencyContacts: List<EmergencyContact> = DemoData.emergencyContacts,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // ── Header ──────────────────────────────────────────────────

        Spacer(Modifier.height(statusBarPadding.calculateTopPadding() + 32.dp))

        // Avatar — subtle radial gradient (cream center, soft sage at the edge).
        // Calm anchor that echoes the page atmosphere; reads as a portrait stand-in,
        // not a UI accent. Color stops hold cream across most of the circle, with
        // a thin sage ring at the perimeter for soft depth.
        val avatarGradient = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to AppColor.Surface,
                0.70f to AppColor.Surface,
                1.0f to AppColor.SageSurface,
            ),
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(avatarGradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = extractInitials(careRecipient.fullName),
                style = MaterialTheme.typography.displayMedium,
                color = AppColor.TextPrimary,
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            // First name only — feels personal; full name reads clinical for "Mom."
            // The data class still holds lastName for future paperwork/export.
            text = careRecipient.firstName,
            style = MaterialTheme.typography.displayMedium,
            color = AppColor.TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Age ${careRecipient.age} \u00B7 ${careRecipient.relationship}",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColor.TextSecondary,
        )

        // ── Medications ─────────────────────────────────────────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                Text(
                    text = "Medications (${medications.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColor.TextPrimary,
                    modifier = Modifier.padding(24.dp),
                )

                medications.forEachIndexed { index, med ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Brand pill icon — visual scanning anchor for the row.
                        PillIcon(
                            color = med.pillColor,
                            kind = med.pillKind,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        // Two-line med info (Pillory MedRow pattern)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = med.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                ),
                                color = AppColor.TextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${med.category} \u00B7 ${med.schedule}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColor.TextSecondary,
                            )
                        }
                        // (No trailing schedule chip — moved into the subline above
                        //  so the row stays compact and the right edge is clean.)
                    }
                    if (index < medications.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = AppColor.Secondary.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        // ── Conditions ──────────────────────────────────────────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColor.TextPrimary,
                )
                Spacer(Modifier.height(14.dp))
                // Pillory benefit-chips pattern: 2-up FlowRow of pill-shape chips
                // with emoji + label. Far more scannable than a flat text strip.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    conditions.forEach { c ->
                        ConditionChip(emoji = c.emoji, label = c.name)
                    }
                }
            }
        }

        // ── Allergies (amber left border + warning chips) ───────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Amber left border bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(120.dp)
                        .background(
                            color = AppColor.Warning,
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                        ),
                )

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Allergies",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColor.TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allergies.forEach { a ->
                            AllergenChip(emoji = a.emoji, label = a.name)
                        }
                    }
                }
            }
        }

        // ── Emergency Contacts ──────────────────────────────────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Emergency",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColor.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                emergencyContacts.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColor.TextPrimary,
                        )
                        Text(
                            text = contact.phone,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColor.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ── Avatar initials helper ──────────────────────────────────────────

/**
 * Extracts up to 2 initials from a person's name.
 *
 * TODO (user contribution): refine the rules. Real names are messy:
 *   - "Rosa Martinez"      → "RM"
 *   - "Dr. Sam Kartner"    → "SK"? or "DS"? (titles usually stripped)
 *   - "Mary Anne Smith"    → "MS" (first + last)? or "MA" (first two)?
 *   - "Madonna"            → "M" (single name)
 *   - leading/trailing whitespace, hyphenated names like "Anne-Marie", etc.
 *
 * Naive baseline: take the first character of each whitespace-separated word, up to 2.
 * Replace with whatever rules feel right for the caregiver context.
 */
private fun extractInitials(name: String): String {
    return name.trim()
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
}

// ── Chip composables (Pillory benefit-chips pattern) ──────────────────────

/** Cream pill-shape chip with emoji + label — Pillory's "Boost immunity" style. */
@Composable
private fun ConditionChip(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppColor.Background.copy(alpha = 0.20f)) // soft sage tint on cream card
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            ),
            color = AppColor.TextPrimary,
        )
    }
}

/** Warning-tinted chip — same shape as Condition chip but amber-flavored. */
@Composable
private fun AllergenChip(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppColor.Warning.copy(alpha = 0.15f)) // soft amber wash
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            ),
            color = AppColor.Warning,
        )
    }
}
