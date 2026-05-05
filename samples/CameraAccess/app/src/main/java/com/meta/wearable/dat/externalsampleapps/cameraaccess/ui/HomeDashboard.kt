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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun HomeDashboard(
    onStartRound: () -> Unit,
    modifier: Modifier = Modifier,
    careRecipient: CareRecipient = DemoData.careRecipient,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
    ) {
        // ── Greeting section ──────────────────────────────────────────
        // Avatar + greeting in a row — Pillory's pattern where the page header
        // visually says "this is [Rosa]'s screen" not "this is the app's screen."
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 24.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mini avatar — same gradient as the Profile avatar, smaller.
            val miniAvatarGradient = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to AppColor.Surface,
                    0.70f to AppColor.Surface,
                    1.0f to AppColor.SageSurface,
                ),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(miniAvatarGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = careRecipient.firstName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = AppColor.TextPrimary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                // "Good morning, Rosa" — name bolded, greeting slightly muted
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = AppColor.TextSecondary)) {
                            append("Good morning, ")
                        }
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = AppColor.TextPrimary,
                        )) {
                            append(careRecipient.firstName)
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(4.dp))
                // Bold-accent status narrative below the greeting
                Text(
                    text = composeStatusNarrative(careRecipient.firstName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColor.TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── "Today" hero card — cream on sage (Pillory Timeline card pattern) ─
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColor.TextPrimary,
                )
                Spacer(Modifier.height(18.dp))

                ScheduleRow(
                    label = "Morning (3)",
                    statusText = "Done",
                    statusColor = AppColor.Safe,
                    iconDone = true,
                )
                Spacer(Modifier.height(14.dp))

                ScheduleRow(
                    label = "Afternoon (2)",
                    statusText = "Due at 2 PM",
                    statusColor = AppColor.PrimaryAccent, // dot stays orange (active "now" indicator)
                    statusTextColor = AppColor.TextSecondary, // text demoted — quiet, the dot speaks
                    iconDone = false,
                )
                Spacer(Modifier.height(14.dp))

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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        )

        // ── Recent activity section ───────────────────────────────────
        Text(
            text = "Recent",
            style = MaterialTheme.typography.titleLarge,
            color = AppColor.TextPrimary,
            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Each row uses a stylized PillIcon in the medication's brand color —
                // visual richness over a generic check. Color choices echo the Pillory
                // pill palette (green = vitamins, pink = antibiotics, yellow = supplements).
                ActivityRow(
                    icon = { PillIcon(color = AppColor.PillGreen, kind = PillKind.Tablet, modifier = Modifier.size(22.dp)) },
                    text = "Metformin 500mg",
                    time = "7:35 AM",
                )
                HorizontalDivider(color = AppColor.Secondary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))
                ActivityRow(
                    icon = { PillIcon(color = AppColor.PillPink, kind = PillKind.Tablet, modifier = Modifier.size(22.dp)) },
                    text = "Lisinopril 10mg",
                    time = "7:36 AM",
                )
                HorizontalDivider(color = AppColor.Secondary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))
                // Warning stays an actual warning icon — it's an alert, not a med-taken event.
                ActivityRow(
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AppColor.Warning, modifier = Modifier.size(22.dp)) },
                    text = "Naproxen interaction",
                    time = "9:15 AM",
                )
            }
        }
    }
}

// ── Status narrative ──────────────────────────────────────────────────
/**
 * Drishti's "voice" — the single sentence under the greeting that answers
 * the caregiver's first question: "How is [name] doing right now?"
 *
 * TODO (user contribution): Replace this fallback with real schedule-aware logic.
 * Decisions to make (these shape how the app *feels*):
 *   - Tone: warm/casual ("Rosa is doing great this morning") vs. clinical
 *     ("Rosa: 3 of 6 doses logged, next at 14:00")?
 *   - Punctuation style: bullet ("On track · Next dose at 2 PM") vs. sentence?
 *   - Overdue framing: alarming ("MISSED — 1 hr overdue") vs. gentle
 *     ("Rosa is 1 hour past her 2 PM dose")?
 *   - End-of-day: "All done today" vs. "All done · See you tomorrow at 8 AM"?
 *
 * For now this returns the legacy text so the screen still renders.
 */
@Suppress("UNUSED_PARAMETER")
private fun composeStatusNarrative(name: String): AnnotatedString = buildAnnotatedString {
    // Pillory-style bold-accent typography: emphasis on the *answer* phrases
    // (positive state, next-dose time). The greeting line above carries the name,
    // so this line just delivers status. Connective tissue stays default weight.
    val bold = SpanStyle(fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)

    withStyle(bold) { append("On track") }
    append(" this morning · Next dose at ")
    withStyle(bold) { append("2 PM") }
}

// ── Helper composables ────────────────────────────────────────────────

@Composable
private fun ScheduleRow(
    label: String,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color,
    iconDone: Boolean,
    // When unset, status text takes the same color as the dot/icon.
    // Pass a separate color to "demote" loud text (e.g., orange dot + gray text)
    // so the dot is the only loud signal and the text reads quietly.
    statusTextColor: androidx.compose.ui.graphics.Color = statusColor,
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
                modifier = Modifier.size(22.dp),
            )
        } else {
            // Filled status dot (coral / gray) drawn via Canvas
            androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                drawCircle(color = statusColor, radius = 6.dp.toPx())
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColor.TextPrimary,
            modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusTextColor,
        )
    }
}

@Composable
private fun ActivityRow(
    icon: @Composable () -> Unit,
    text: String,
    time: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColor.TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = AppColor.TextSecondary,
        )
    }
}
