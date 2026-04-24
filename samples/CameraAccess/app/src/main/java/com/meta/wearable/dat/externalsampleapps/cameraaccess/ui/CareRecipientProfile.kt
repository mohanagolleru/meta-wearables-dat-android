/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Hardcoded demo data ─────────────────────────────────────────────

private data class Medication(val name: String, val schedule: String)

private val medications = listOf(
    Medication("Metformin 500mg", "2x daily"),
    Medication("Lisinopril 10mg", "Morning"),
    Medication("Amlodipine 5mg", "Afternoon"),
    Medication("Omeprazole 20mg", "Afternoon"),
    Medication("Calcium 600mg", "Morning"),
    Medication("Vitamin D 1000IU", "Morning"),
    Medication("Aspirin 81mg", "Evening"),
    Medication("Atorvastatin 20mg", "Evening"),
)

private data class EmergencyContact(val name: String, val phone: String)

private val emergencyContacts = listOf(
    EmergencyContact("Dr. Patel (Primary)", "555-0123"),
    EmergencyContact("Maria (Caregiver)", "555-0456"),
)

// ── Profile Screen ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareRecipientProfile(
    modifier: Modifier = Modifier,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // ── Header ──────────────────────────────────────────────────

        Spacer(Modifier.height(statusBarPadding.calculateTopPadding() + 20.dp))

        // Avatar circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppColor.PrimaryAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                tint = AppColor.PrimaryAccent,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Janani",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextPrimary,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Age 78 \u00B7 Mother",
            fontSize = 14.sp,
            color = AppColor.TextSecondary,
        )

        // ── Medications ─────────────────────────────────────────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                Text(
                    text = "Medications (${medications.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                    modifier = Modifier.padding(16.dp),
                )

                medications.forEachIndexed { index, med ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = med.name,
                            fontSize = 15.sp,
                            color = AppColor.TextPrimary,
                        )
                        Text(
                            text = med.schedule,
                            fontSize = 14.sp,
                            color = AppColor.TextSecondary,
                        )
                    }
                    if (index < medications.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Conditions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                )

                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Type 2 Diabetes \u00B7 Hypertension \u00B7 High Cholesterol \u00B7 Osteoporosis",
                        fontSize = 15.sp,
                        color = AppColor.TextPrimary,
                    )
                }
            }
        }

        // ── Allergies (amber left border) ───────────────────────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .border(
                    width = 0.dp,
                    color = AppColor.Surface,
                    shape = cardShape,
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Amber left border
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .background(
                            color = AppColor.Warning,
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                        ),
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Allergies",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColor.TextSecondary,
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Allergy warning",
                            tint = AppColor.Warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Penicillin \u00B7 Sulfa drugs",
                            fontSize = 15.sp,
                            color = AppColor.Warning,
                        )
                    }
                }
            }
        }

        // ── Emergency Contacts ──────────────────────────────────────

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Emergency",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                )

                Spacer(Modifier.height(8.dp))

                emergencyContacts.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = contact.name,
                            fontSize = 15.sp,
                            color = AppColor.TextPrimary,
                        )
                        Text(
                            text = contact.phone,
                            fontSize = 15.sp,
                            color = AppColor.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
