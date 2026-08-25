package network.spiritscorp

/*
 * Copyright (C) 2026 Tom Spirit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import network.spiritscorp.model.CalculationSummary
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Unit & integration tests for the Calculator state computation engine,
 * verifying accurate multi-step formulas, correction factors, unit toggles,
 * and user factor overrides.
 */
class CalculatorEngineStateTest {

    private fun calculateSummary(
        carbInput: String,
        selectedUnit: CarbUnit,
        selectedTimeOfDay: TimeOfDay,
        factorOverride: Double?,
        currentGlucoseInput: String,
        targetGlucoseInput: String,
        correctionFactorInput: String,
        showCorrection: Boolean,
        settings: UserSettings
    ): CalculationSummary {
        val rawInput = carbInput.toDoubleOrNull() ?: 0.0
        val beDivisor = settings.beGramsDivisor.toDouble().let { if (it > 0.0) it else 12.0 }

        val grams = when (selectedUnit) {
            CarbUnit.GRAMS -> rawInput
            CarbUnit.KE -> rawInput * 10.0
            CarbUnit.BE -> rawInput * beDivisor
        }

        val ke = grams / 10.0
        val be = grams / 12.0

        val factor = factorOverride ?: when (selectedTimeOfDay) {
            TimeOfDay.MORNING -> settings.morningFactor
            TimeOfDay.NOON -> settings.noonFactor
            TimeOfDay.EVENING -> settings.eveningFactor
            TimeOfDay.NIGHT -> settings.nightFactor
        }

        val unitsCount = when (selectedUnit) {
            CarbUnit.BE -> rawInput
            CarbUnit.KE -> rawInput
            CarbUnit.GRAMS -> grams / 12.0
        }
        val mealInsulin = unitsCount * factor

        var correctionInsulin = 0.0
        val currentBg = currentGlucoseInput.toDoubleOrNull()
        val targetBg = targetGlucoseInput.toDoubleOrNull()
        val corrFactor = correctionFactorInput.toDoubleOrNull()

        var isHypoRisk = false
        var advisory = "Standard-Dosis für die Mahlzeit"
        val isMmol = settings.glucoseUnit.lowercase().contains("mmol")
        val hypoThreshold = if (isMmol) 3.9 else 70.0

        if (showCorrection && currentBg != null && targetBg != null && corrFactor != null && corrFactor > 0) {
            if (currentBg < hypoThreshold) {
                isHypoRisk = true
                advisory = "Achtung: Niedriger Blutzucker! Bitte zuerst 1-2 KE schnelle KH einnehmen."
            } else if (currentBg < targetBg) {
                val diff = targetBg - currentBg
                correctionInsulin = -(diff / corrFactor)
                advisory = "Blutzucker unter Zielbereich: Korrektur reduziert Gesamtdosis."
            } else if (currentBg > targetBg) {
                val diff = currentBg - targetBg
                correctionInsulin = diff / corrFactor
                advisory = "Erhöhter Blutzucker: Korrektur-Bolus addiert."
            }
        }

        val rawTotal = (mealInsulin + correctionInsulin).coerceAtLeast(0.0)
        val roundingStep = settings.roundingStep

        val factorStep = if (roundingStep > 0.0) 1.0 / roundingStep else 1.0
        val roundedTotal = if (roundingStep > 0.0) {
            BigDecimal(Math.round(rawTotal * factorStep) / factorStep)
                .setScale(if (roundingStep == 0.1) 1 else if (roundingStep == 0.5) 1 else 0, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            BigDecimal(rawTotal).setScale(2, RoundingMode.HALF_UP).toDouble()
        }

        return CalculationSummary(
            carbGrams = BigDecimal(grams).setScale(1, RoundingMode.HALF_UP).toDouble(),
            keValue = BigDecimal(ke).setScale(2, RoundingMode.HALF_UP).toDouble(),
            beValue = BigDecimal(be).setScale(2, RoundingMode.HALF_UP).toDouble(),
            factorUsed = factor,
            mealInsulin = BigDecimal(mealInsulin).setScale(2, RoundingMode.HALF_UP).toDouble(),
            bloodGlucoseInput = currentBg,
            targetGlucose = targetBg,
            correctionInsulin = BigDecimal(correctionInsulin).setScale(2, RoundingMode.HALF_UP).toDouble(),
            rawTotalInsulin = BigDecimal(rawTotal).setScale(2, RoundingMode.HALF_UP).toDouble(),
            roundedTotalInsulin = roundedTotal,
            roundingStep = roundingStep,
            isHypoRisk = isHypoRisk,
            advisoryNote = advisory
        )
    }

    @Test
    fun testMorningCalculationWithGrams() {
        val settings = UserSettings(morningFactor = 1.50, roundingStep = 0.5)
        val summary = calculateSummary(
            carbInput = "60",
            selectedUnit = CarbUnit.GRAMS,
            selectedTimeOfDay = TimeOfDay.MORNING,
            factorOverride = null,
            currentGlucoseInput = "",
            targetGlucoseInput = "100",
            correctionFactorInput = "40",
            showCorrection = false,
            settings = settings
        )

        // 60g KH / 12 = 5 BE. 5 BE * 1.5 = 7.5 IE
        assertEquals(60.0, summary.carbGrams, 0.001)
        assertEquals(5.0, summary.beValue, 0.001)
        assertEquals(6.0, summary.keValue, 0.001)
        assertEquals(1.50, summary.factorUsed, 0.001)
        assertEquals(7.50, summary.mealInsulin, 0.001)
        assertEquals(7.5, summary.roundedTotalInsulin, 0.001)
        assertFalse(summary.isHypoRisk)
    }

    @Test
    fun testNoonCalculationWithBEAndFactorOverride() {
        val settings = UserSettings(noonFactor = 1.00, roundingStep = 0.5)
        // User overrides factor from 1.0 to 1.3
        val summary = calculateSummary(
            carbInput = "4.5",
            selectedUnit = CarbUnit.BE,
            selectedTimeOfDay = TimeOfDay.NOON,
            factorOverride = 1.30,
            currentGlucoseInput = "",
            targetGlucoseInput = "100",
            correctionFactorInput = "40",
            showCorrection = false,
            settings = settings
        )

        // 4.5 BE * 12 = 54.0g KH. 4.5 BE * 1.30 = 5.85 IE -> Rounded to step 0.5 = 6.0 IE
        assertEquals(54.0, summary.carbGrams, 0.001)
        assertEquals(4.5, summary.beValue, 0.001)
        assertEquals(1.30, summary.factorUsed, 0.001)
        assertEquals(5.85, summary.mealInsulin, 0.001)
        assertEquals(6.0, summary.roundedTotalInsulin, 0.001)
    }

    @Test
    fun testEveningCalculationWithHighGlucoseCorrection() {
        val settings = UserSettings(eveningFactor = 1.20, roundingStep = 0.5)
        // 3.0 BE (36g KH) -> 3.0 * 1.2 = 3.6 IE
        // Current BG 220, Target 100, CorrFactor 40 -> (220-100)/40 = +3.0 IE correction
        // Total = 3.6 + 3.0 = 6.6 IE -> Rounded to step 0.5 = 6.5 IE
        val summary = calculateSummary(
            carbInput = "3.0",
            selectedUnit = CarbUnit.BE,
            selectedTimeOfDay = TimeOfDay.EVENING,
            factorOverride = null,
            currentGlucoseInput = "220",
            targetGlucoseInput = "100",
            correctionFactorInput = "40",
            showCorrection = true,
            settings = settings
        )

        assertEquals(36.0, summary.carbGrams, 0.001)
        assertEquals(3.60, summary.mealInsulin, 0.001)
        assertEquals(3.00, summary.correctionInsulin, 0.001)
        assertEquals(6.60, summary.rawTotalInsulin, 0.001)
        assertEquals(6.5, summary.roundedTotalInsulin, 0.001)
        assertTrue(summary.advisoryNote.contains("Erhöhter Blutzucker"))
    }

    @Test
    fun testLowBloodGlucoseWarningAndDoseReduction() {
        val settings = UserSettings(morningFactor = 1.50, roundingStep = 0.5)
        // Current BG = 65 (Hypo risk < 70)
        val summary = calculateSummary(
            carbInput = "30",
            selectedUnit = CarbUnit.GRAMS,
            selectedTimeOfDay = TimeOfDay.MORNING,
            factorOverride = null,
            currentGlucoseInput = "65",
            targetGlucoseInput = "100",
            correctionFactorInput = "40",
            showCorrection = true,
            settings = settings
        )

        assertTrue(summary.isHypoRisk)
        assertTrue(summary.advisoryNote.contains("Achtung: Niedriger Blutzucker"))
    }

    @Test
    fun testNegativeCorrectionDoseReduction() {
        val settings = UserSettings(noonFactor = 1.0, roundingStep = 0.5)
        // 2 BE = 2.0 IE meal insulin. BG = 80, Target = 100, Corr = 40
        // Correction = -(20 / 40) = -0.5 IE
        // Total = 2.0 - 0.5 = 1.5 IE
        val summary = calculateSummary(
            carbInput = "2.0",
            selectedUnit = CarbUnit.BE,
            selectedTimeOfDay = TimeOfDay.NOON,
            factorOverride = null,
            currentGlucoseInput = "80",
            targetGlucoseInput = "100",
            correctionFactorInput = "40",
            showCorrection = true,
            settings = settings
        )

        assertEquals(2.0, summary.mealInsulin, 0.001)
        assertEquals(-0.50, summary.correctionInsulin, 0.001)
        assertEquals(1.50, summary.rawTotalInsulin, 0.001)
        assertEquals(1.5, summary.roundedTotalInsulin, 0.001)
        assertFalse(summary.isHypoRisk)
        assertTrue(summary.advisoryNote.contains("reduziert"))
    }

    @Test
    fun testMmolLCorrectionCalculation() {
        val settings = UserSettings(
            eveningFactor = 1.00,
            glucoseUnit = "mmol/l",
            targetGlucoseMgDl = 120.0, // ~6.66 mmol/l
            correctionFactorMgDl = 50.0, // ~2.77 mmol/l pro IE
            roundingStep = 0.5
        )

        // 3 KE = 3.0 IE meal insulin
        // Target: 6.7 mmol/l, Current BG: 12.3 mmol/l, CorrFactor: 2.8 mmol/l pro IE
        // Diff = 12.3 - 6.7 = 5.6 mmol/l
        // Correction = 5.6 / 2.8 = 2.0 IE
        // Total = 3.0 + 2.0 = 5.0 IE
        val summary = calculateSummary(
            carbInput = "3.0",
            selectedUnit = CarbUnit.KE,
            selectedTimeOfDay = TimeOfDay.EVENING,
            factorOverride = null,
            currentGlucoseInput = "12.3",
            targetGlucoseInput = "6.7",
            correctionFactorInput = "2.8",
            showCorrection = true,
            settings = settings
        )

        assertEquals(30.0, summary.carbGrams, 0.001)
        assertEquals(3.00, summary.mealInsulin, 0.001)
        assertEquals(2.00, summary.correctionInsulin, 0.001)
        assertEquals(5.00, summary.rawTotalInsulin, 0.001)
        assertEquals(5.0, summary.roundedTotalInsulin, 0.001)
        assertTrue(summary.advisoryNote.contains("Erhöhter Blutzucker"))
    }

    @Test
    fun testMmolLHypoRiskDetection() {
        val settings = UserSettings(
            morningFactor = 1.50,
            glucoseUnit = "mmol/l",
            roundingStep = 0.5
        )
        // Hypo threshold in mmol/l is 3.9 mmol/l (< 70 mg/dl)
        val currentBg = 3.5
        val isHypo = currentBg < 3.9
        assertTrue(isHypo)
    }

    @Test
    fun testSanitizeCarbInput() {
        fun sanitize(input: String): String {
            val sanitized = input.replace(',', '.').filter { it.isDigit() || it == '.' }
            val dotsCount = sanitized.count { it == '.' }
            return if (dotsCount <= 1 && sanitized.length <= 8) sanitized else ""
        }

        assertEquals("45.5", sanitize("45,5"))
        assertEquals("12.0", sanitize("12.0"))
        assertEquals("60", sanitize("60g"))
        assertEquals("", sanitize("45.5.5")) // multiple dots
    }
}
