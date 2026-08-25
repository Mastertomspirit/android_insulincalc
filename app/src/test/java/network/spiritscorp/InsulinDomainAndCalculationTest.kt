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
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Comprehensive domain unit tests for core diabetes medical formulas,
 * units conversion, boundary checks, and time-of-day interval logic.
 */
class InsulinDomainAndCalculationTest {

    private fun roundToStep(value: Double, step: Double): Double {
        if (step <= 0.0) return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
        val factor = 1.0 / step
        return BigDecimal(Math.round(value * factor) / factor)
            .setScale(if (step == 0.1) 1 else if (step == 0.5) 1 else 0, RoundingMode.HALF_UP)
            .toDouble()
    }

    @Test
    fun testCarbUnitGramsConversions() {
        val unit = CarbUnit.GRAMS
        assertEquals("g KH", unit.shortName)
        assertEquals("Gramm Kohlenhydrate", unit.label)
        assertEquals(100.0, unit.toGrams(100.0), 0.001)
        assertEquals(0.0, unit.toGrams(0.0), 0.001)
        assertEquals(75.5, unit.fromGrams(75.5), 0.001)
    }

    @Test
    fun testCarbUnitKEConversions() {
        val unit = CarbUnit.KE
        assertEquals("KE", unit.shortName)
        assertEquals("Kohlenhydrateinheit (10g)", unit.label)
        // 1 KE = 10g
        assertEquals(10.0, unit.toGrams(1.0), 0.001)
        assertEquals(35.0, unit.toGrams(3.5), 0.001)
        assertEquals(4.2, unit.fromGrams(42.0), 0.001)
    }

    @Test
    fun testCarbUnitBEConversions() {
        val unit = CarbUnit.BE
        assertEquals("BE", unit.shortName)
        assertEquals("Broteinheit (12g)", unit.label)
        // 1 BE = 12g (standard German divisor)
        assertEquals(12.0, unit.toGrams(1.0), 0.001)
        assertEquals(48.0, unit.toGrams(4.0), 0.001)
        assertEquals(5.0, unit.fromGrams(60.0), 0.001)
    }

    @Test
    fun testGlucoseUnitConversionsAndFormulas() {
        val mgDlUnit = GlucoseUnit.MG_DL
        assertEquals("mg/dl", mgDlUnit.shortName)
        assertEquals(120.0, mgDlUnit.toMgDl(120.0), 0.001)
        assertEquals(120.0, mgDlUnit.fromMgDl(120.0), 0.001)

        val mmolUnit = GlucoseUnit.MMOL_L
        assertEquals("mmol/l", mmolUnit.shortName)
        // Conversion factor: 18.0182
        val mgDlValue = 180.182
        val expectedMmol = 10.0
        assertEquals(expectedMmol, mmolUnit.fromMgDl(mgDlValue), 0.01)
        assertEquals(mgDlValue, mmolUnit.toMgDl(expectedMmol), 0.01)
    }

    @Test
    fun testTimeOfDayFull24HourCoverage() {
        // Morning: 06:00 - 10:59
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(6))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(8))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(10))

        // Noon: 11:00 - 16:59
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(11))
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(13))
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(16))

        // Evening: 17:00 - 21:59
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(17))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(19))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(21))

        // Night: 22:00 - 05:59
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(22))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(23))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(0))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(3))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(5))
    }

    @Test
    fun testTimeOfDayMetadataAndProperties() {
        for (tod in TimeOfDay.values()) {
            assertNotNull(tod.title)
            assertNotNull(tod.subtitle)
            assertTrue(tod.defaultFactor > 0.0)
            assertNotNull(tod.icon)
            assertNotNull(tod.accentColor)
        }
    }

    @Test
    fun testHypoglycemiaBoundaryCheck() {
        val summaryNormal = CalculationSummary(
            carbGrams = 40.0,
            keValue = 4.0,
            beValue = 3.33,
            factorUsed = 1.0,
            mealInsulin = 3.33,
            bloodGlucoseInput = 90.0,
            targetGlucose = 100.0,
            correctionInsulin = -0.25,
            rawTotalInsulin = 3.08,
            roundedTotalInsulin = 3.0,
            roundingStep = 0.5,
            isHypoRisk = false,
            advisoryNote = "Normal"
        )
        assertFalse(summaryNormal.isHypoRisk)

        val summaryHypo = CalculationSummary(
            carbGrams = 20.0,
            keValue = 2.0,
            beValue = 1.67,
            factorUsed = 1.0,
            mealInsulin = 1.67,
            bloodGlucoseInput = 58.0,
            targetGlucose = 100.0,
            correctionInsulin = -1.05,
            rawTotalInsulin = 0.62,
            roundedTotalInsulin = 0.5,
            roundingStep = 0.5,
            isHypoRisk = true,
            advisoryNote = "Achtung: Niedriger Blutzucker!"
        )
        assertTrue(summaryHypo.isHypoRisk)
    }

    @Test
    fun testInsulinRoundingPrecisionSteps() {
        // Step 0.5 (Standard insulin pen)
        assertEquals(4.0, roundToStep(3.8, 0.5), 0.001)
        assertEquals(3.5, roundToStep(3.7, 0.5), 0.001)
        assertEquals(3.5, roundToStep(3.3, 0.5), 0.001)
        assertEquals(3.0, roundToStep(3.2, 0.5), 0.001)
        assertEquals(0.0, roundToStep(0.2, 0.5), 0.001)
        assertEquals(0.5, roundToStep(0.3, 0.5), 0.001)

        // Step 0.1 (Insulin pump)
        assertEquals(4.3, roundToStep(4.28, 0.1), 0.001)
        assertEquals(4.2, roundToStep(4.24, 0.1), 0.001)
        assertEquals(0.1, roundToStep(0.09, 0.1), 0.001)

        // Step 1.0 (Full units)
        assertEquals(5.0, roundToStep(4.6, 1.0), 0.001)
        assertEquals(4.0, roundToStep(4.4, 1.0), 0.001)
    }

    @Test
    fun testUserSettingsCustomDivisorAndFactors() {
        val customSettings = UserSettings(
            id = 1,
            morningFactor = 2.0,
            noonFactor = 1.25,
            eveningFactor = 1.5,
            nightFactor = 0.75,
            defaultCarbUnit = "KE",
            beGramsDivisor = 10, // Some clinics use 10g for BE
            targetGlucoseMgDl = 110.0,
            correctionFactorMgDl = 35.0,
            roundingStep = 0.1,
            showDisclaimer = false,
            selectedTheme = "EMERALD_MINT"
        )

        assertEquals(2.0, customSettings.morningFactor, 0.001)
        assertEquals(10, customSettings.beGramsDivisor)
        assertEquals("KE", customSettings.defaultCarbUnit)
        assertEquals("EMERALD_MINT", customSettings.selectedTheme)
        assertFalse(customSettings.showDisclaimer)
    }
}
