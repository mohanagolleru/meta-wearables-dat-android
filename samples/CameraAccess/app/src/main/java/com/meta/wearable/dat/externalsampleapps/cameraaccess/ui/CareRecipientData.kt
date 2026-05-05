/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

/**
 * Drishti's domain model for the care recipient.
 *
 * The app is *about* a person, not a screen — name, age, medications, allergies
 * all belong to one [CareRecipient]. Drishti screens (HomeDashboard, Profile,
 * CareLog) all read from a single instance held at the scaffold root. When real
 * persistence lands (Room/SQLite per Drishti v2), only the root changes; every
 * downstream screen keeps the same signature.
 */
data class CareRecipient(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val relationship: String,
    val photoUri: String? = null,
) {
    val fullName: String get() = "$firstName $lastName"
}

/** A scheduled or as-needed medication. */
data class Medication(
    val name: String,
    val schedule: String,
    /** Brand color for the PillIcon — drives visual scanning. */
    val pillColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF9BCB7E), // PillGreen default
    /** Visual shape — most meds are tablets; capsules for antibiotics, supplements. */
    val pillKind: PillKind = PillKind.Tablet,
    /** Therapeutic category (Diabetes, Cardio, etc.) — shown on Profile rows. */
    val category: String = "General",
)

/** A health condition with an emoji glyph for scannable chip display. */
data class Condition(val name: String, val emoji: String)

/** An allergen with severity hint. */
data class Allergen(val name: String, val emoji: String = "\u26A0\uFE0F")

/** A care-team contact (doctor, primary caregiver, etc.). */
data class EmergencyContact(
    val name: String,
    val phone: String,
)

/**
 * Demo data — the single source of truth for the care recipient and her
 * regimen until persistence lands. Update one [DemoData] to change every
 * screen at once.
 */
object DemoData {

    val careRecipient: CareRecipient = CareRecipient(
        firstName = "Rosa",
        lastName = "Martinez",
        age = 78,
        relationship = "Mother",
    )

    val medications: List<Medication> = listOf(
        // Brand colors echo the Pillory pill palette; categories map clinically.
        Medication("Metformin 500mg",   "2x daily",  AppColor.PillGreen,  PillKind.Tablet,  "Diabetes"),
        Medication("Lisinopril 10mg",   "Morning",   AppColor.PillPink,   PillKind.Tablet,  "Blood pressure"),
        Medication("Amlodipine 5mg",    "Afternoon", AppColor.PillPink,   PillKind.Tablet,  "Blood pressure"),
        Medication("Omeprazole 20mg",   "Afternoon", AppColor.PillYellow, PillKind.Capsule, "Stomach"),
        Medication("Calcium 600mg",     "Morning",   AppColor.PillYellow, PillKind.Tablet,  "Bone health"),
        Medication("Vitamin D 1000IU",  "Morning",   AppColor.PillYellow, PillKind.Capsule, "Supplement"),
        Medication("Aspirin 81mg",      "Evening",   AppColor.PillPink,   PillKind.Tablet,  "Cardio protect"),
        Medication("Atorvastatin 20mg", "Evening",   AppColor.PillGreen,  PillKind.Tablet,  "Cholesterol"),
    )

    val conditions: List<Condition> = listOf(
        Condition("Type 2 Diabetes", "\uD83E\uDE78"),     // 🩸 drop of blood
        Condition("Hypertension",    "\uD83D\uDC93"),     // 💓 beating heart
        Condition("High Cholesterol","\uD83D\uDCCA"),     // 📊 chart
        Condition("Osteoporosis",    "\uD83E\uDDB4"),     // 🦴 bone
    )

    val allergies: List<Allergen> = listOf(
        Allergen("Penicillin"),
        Allergen("Sulfa drugs"),
    )

    val emergencyContacts: List<EmergencyContact> = listOf(
        EmergencyContact("Dr. Patel (Primary)", "555-0123"),
        EmergencyContact("Maria (Caregiver)", "555-0456"),
    )
}
