/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeDashboard(
    onStartRound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
    ) {
        // ── Greeting section ──────────────────────────────────────────
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(top = 20.dp)
                    .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Good morning",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Janani has 3 medications due",
                fontSize = 15.sp,
                color = AppColor.TextSecondary,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Schedule card ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextPrimary,
                )
                Spacer(Modifier.height(12.dp))

                // Morning row
                ScheduleRow(
                    label = "Morning (3)",
                    statusText = "Done",
                    statusColor = AppColor.Safe,
                    iconDone = true,
                )
                Spacer(Modifier.height(10.dp))

                // Afternoon row
                ScheduleRow(
                    label = "Afternoon (2)",
                    statusText = "Due at 2 PM",
                    statusColor = AppColor.PrimaryAccent,
                    iconDone = false,
                )
                Spacer(Modifier.height(10.dp))

                // Evening row
                ScheduleRow(
                    label = "Evening (3)",
                    statusText = "Due at 8 PM",
                    statusColor = AppColor.Secondary,
                    iconDone = false,
                )
            }
        }

        // ── Start button ──────────────────────────────────────────────
        SwitchButton(
            label = "Start Next Round",
            onClick = onStartRound,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )

        // ── Recent activity section ───────────────────────────────────
        Text(
            text = "Recent",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextPrimary,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Entry 1
                ActivityRow(
                    icon = { Icon(Icons.Default.Check, contentDescription = null, tint = AppColor.Safe, modifier = Modifier.size(20.dp)) },
                    text = "Metformin 500mg",
                    time = "7:35 AM",
                    textColor = AppColor.TextPrimary,
                )

                HorizontalDivider(color = AppColor.Secondary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Entry 2
                ActivityRow(
                    icon = { Icon(Icons.Default.Check, contentDescription = null, tint = AppColor.Safe, modifier = Modifier.size(20.dp)) },
                    text = "Lisinopril 10mg",
                    time = "7:36 AM",
                    textColor = AppColor.TextPrimary,
                )

                HorizontalDivider(color = AppColor.Secondary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Entry 3 — warning
                ActivityRow(
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AppColor.Warning, modifier = Modifier.size(20.dp)) },
                    text = "Naproxen interaction",
                    time = "9:15 AM",
                    textColor = AppColor.TextPrimary,
                )
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────

@Composable
private fun ScheduleRow(
    label: String,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color,
    iconDone: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconDone) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = AppColor.Safe,
                modifier = Modifier.size(20.dp),
            )
        } else {
            // Coral / gray dot via a small filled circle
            androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                drawCircle(color = statusColor, radius = 5.dp.toPx())
            }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            color = AppColor.TextPrimary,
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = statusText,
            fontSize = 13.sp,
            color = statusColor,
        )
    }
}

@Composable
private fun ActivityRow(
    icon: @Composable () -> Unit,
    text: String,
    time: String,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = time,
            fontSize = 13.sp,
            color = AppColor.TextSecondary,
        )
    }
}
