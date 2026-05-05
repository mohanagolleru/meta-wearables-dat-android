/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R

/**
 * Care Log — adherence chart + chronological event list.
 *
 * Top of screen: AdherenceCard answers "How is Mom doing this week?" at a glance
 * (big % + 7-bar chart + sentence).
 * Below: events grouped by day, captured by Drishti's tools (`log_observation`,
 * `log_medication` from the Mac side; sample data for now).
 *
 * Replace [sampleGroups] and [sampleAdherence] with real CSV-backed data when
 * the on-device persistence lands per Drishti v2.
 */
private enum class EventKind {
    INTAKE,        // medication taken (PillIcon in brand color)
    WARNING,       // drug interaction or alert (amber triangle)
    OBSERVATION,   // Drishti's glasses saw and identified something (smart-glasses icon)
}

private data class CareEvent(
    val kind: EventKind,
    val title: String,
    val detail: String,
    val time: String,
    /** For INTAKE events, the brand color of the pill (drives PillIcon). Ignored for non-INTAKE. */
    val pillColor: Color = AppColor.PillGreen,
)

private data class EventGroup(val label: String, val events: List<CareEvent>)

/** Per-day adherence: doses [taken] of [scheduled]. */
private data class DailyAdherence(val day: String, val taken: Int, val scheduled: Int) {
    val percent: Float get() = if (scheduled == 0) 0f else taken.toFloat() / scheduled.toFloat()
    val hasData: Boolean get() = scheduled > 0
}

// ── Sample data — replace with real CSV-backed feed ─────────────────

private val sampleAdherence = listOf(
    DailyAdherence("Mon", taken = 6, scheduled = 6),
    DailyAdherence("Tue", taken = 4, scheduled = 6),
    DailyAdherence("Wed", taken = 6, scheduled = 6),
    DailyAdherence("Thu", taken = 5, scheduled = 6),
    DailyAdherence("Fri", taken = 6, scheduled = 6),
    DailyAdherence("Sat", taken = 6, scheduled = 6),
    DailyAdherence("Sun", taken = 3, scheduled = 5), // today — partial
)

private val sampleGroups = listOf(
    EventGroup(
        label = "Today",
        events = listOf(
            CareEvent(EventKind.INTAKE, "Metformin 500mg", "Morning dose", "7:35 AM", AppColor.PillGreen),
            CareEvent(EventKind.INTAKE, "Lisinopril 10mg", "Morning dose", "7:36 AM", AppColor.PillPink),
            CareEvent(EventKind.INTAKE, "Calcium 600mg", "Morning dose", "7:38 AM", AppColor.PillYellow),
            CareEvent(EventKind.WARNING, "Naproxen interaction", "Avoid with Lisinopril", "9:15 AM"),
            CareEvent(EventKind.OBSERVATION, "Bottle confirmed", "Vitamin D 1000IU label scanned", "9:42 AM"),
        ),
    ),
    EventGroup(
        label = "Yesterday",
        events = listOf(
            CareEvent(EventKind.INTAKE, "Metformin 500mg", "Morning dose", "7:30 AM", AppColor.PillGreen),
            CareEvent(EventKind.INTAKE, "Aspirin 81mg", "Evening dose", "8:05 PM", AppColor.PillPink),
            CareEvent(EventKind.INTAKE, "Atorvastatin 20mg", "Evening dose", "8:06 PM", AppColor.PillYellow),
            CareEvent(EventKind.OBSERVATION, "Pill organizer refilled", "Weekly stock photo logged", "6:20 PM"),
        ),
    ),
)

// ── Screen ──────────────────────────────────────────────────────────

@Composable
fun CareLogScreen(
    modifier: Modifier = Modifier,
    careRecipient: CareRecipient = DemoData.careRecipient,
) {
    val weeklyAdherence = sampleAdherence // wire to real CSV-backed source later
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        // Header
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 28.dp)
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = "Care Log",
                style = MaterialTheme.typography.headlineLarge,
                color = AppColor.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Adherence at a glance, plus everything Drishti has captured.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColor.TextSecondary,
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Adherence chart card ───────────────────────────────────────
        AdherenceCard(
            careRecipientName = careRecipient.firstName,
            data = weeklyAdherence,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(28.dp))

        // ── Day-grouped events ─────────────────────────────────────────
        sampleGroups.forEach { group ->
            Text(
                text = group.label,
                style = MaterialTheme.typography.titleLarge,
                color = AppColor.TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    group.events.forEachIndexed { index, event ->
                        EventRow(event)
                        if (index < group.events.lastIndex) {
                            HorizontalDivider(
                                color = AppColor.Secondary.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Adherence chart ──────────────────────────────────────────────────

@Composable
private fun AdherenceCard(
    careRecipientName: String,
    data: List<DailyAdherence>,
    modifier: Modifier = Modifier,
) {
    val totalTaken = data.sumOf { it.taken }
    val totalScheduled = data.sumOf { it.scheduled }
    val weeklyPercent = if (totalScheduled == 0) 0 else (totalTaken * 100) / totalScheduled

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Headline: big % + label
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$weeklyPercent%",
                    style = MaterialTheme.typography.displayMedium,
                    color = AppColor.PrimaryAccent, // the *answer* — focal warmth
                )
                Spacer(Modifier.size(width = 8.dp, height = 1.dp))
                Text(
                    text = "this week",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColor.TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 7-bar chart
            BarChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Day-of-week labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                data.forEach { day ->
                    Text(
                        text = day.day,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColor.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Footer sentence
            Text(
                text = "$careRecipientName took $totalTaken of $totalScheduled scheduled doses.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColor.TextSecondary,
            )
        }
    }
}

@Composable
private fun BarChart(
    data: List<DailyAdherence>,
    modifier: Modifier = Modifier,
) {
    val safeColor = AppColor.Safe
    val activeColor = AppColor.PrimaryAccent
    val emptyColor = AppColor.Secondary.copy(alpha = 0.3f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (data.isEmpty()) return@Canvas

            val barCount = data.size
            val gap = 10.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())

            data.forEachIndexed { i, day ->
                val x = i * (barWidth + gap)
                val fillHeight = if (day.hasData) size.height * day.percent.coerceIn(0.05f, 1f) else size.height * 0.08f
                val y = size.height - fillHeight

                // Background track (very faint, full-height) — the "scheduled" baseline
                drawRoundRect(
                    color = AppColor.Secondary.copy(alpha = 0.15f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = cornerRadius,
                )

                // Filled bar — semantic color by status
                val color = when {
                    !day.hasData -> emptyColor
                    day.percent >= 0.9f -> safeColor              // ≥90% = great → sage
                    day.percent >= 0.5f -> safeColor.copy(alpha = 0.7f) // partial good → faded sage
                    else -> activeColor                            // miss / very low → orange (active warning)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, fillHeight),
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}

// ── Event row ────────────────────────────────────────────────────────

@Composable
private fun EventRow(event: CareEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Render INTAKE events with the medication's pill icon (visual richness),
        // and non-INTAKE (warning, photo) with material icons (semantic clarity).
        when (event.kind) {
            EventKind.INTAKE -> PillIcon(
                color = event.pillColor,
                kind = PillKind.Tablet,
                modifier = Modifier.size(22.dp),
            )
            EventKind.WARNING -> Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = AppColor.Warning,
                modifier = Modifier.size(22.dp),
            )
            // Drishti's glasses saw + identified — smart-glasses icon, NOT a camera.
            // Drishti is a wearable; the user looks, the glasses observe.
            EventKind.OBSERVATION -> Icon(
                painter = painterResource(id = R.drawable.smart_glasses_icon),
                contentDescription = null,
                tint = AppColor.PrimaryAccent,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColor.TextPrimary,
            )
            Text(
                text = event.detail,
                style = MaterialTheme.typography.bodySmall,
                color = AppColor.TextSecondary,
            )
        }
        Text(
            text = event.time,
            style = MaterialTheme.typography.bodySmall,
            color = AppColor.TextSecondary,
        )
    }
}

