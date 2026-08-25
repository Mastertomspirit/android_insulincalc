package network.spiritscorp

import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests verifying unit conversions, blood glucose calculations,
 * and default user settings integrity.
 */
class InsulinUnitConversionTest {

    @Test
    fun testCarbUnitConversionFormulas() {
        // Grams
        assertEquals(50.0, CarbUnit.GRAMS.toGrams(50.0), 0.001)
        assertEquals(50.0, CarbUnit.GRAMS.fromGrams(50.0), 0.001)

        // KE (1 KE = 10g)
        assertEquals(40.0, CarbUnit.KE.toGrams(4.0), 0.001)
        assertEquals(4.5, CarbUnit.KE.fromGrams(45.0), 0.001)

        // BE (1 BE = 12g)
        assertEquals(36.0, CarbUnit.BE.toGrams(3.0), 0.001)
        assertEquals(3.0, CarbUnit.BE.fromGrams(36.0), 0.001)
    }

    @Test
    fun testGlucoseUnitConversion() {
        // 100 mg/dl in mmol/l: 100 / 18.0182 ≈ 5.55 mmol/l
        val mmol = GlucoseUnit.MMOL_L.fromMgDl(100.0)
        assertEquals(5.55, mmol, 0.01)

        // 5.55 mmol/l in mg/dl: 5.55 * 18.0182 ≈ 100 mg/dl
        val mgDl = GlucoseUnit.MMOL_L.toMgDl(5.55)
        assertEquals(100.0, mgDl, 0.1)

        // 120 mg/dl in mmol/l: 120 / 18.0182 ≈ 6.66 mmol/l
        val mmol120 = GlucoseUnit.MMOL_L.fromMgDl(120.0)
        assertEquals(6.66, mmol120, 0.01)

        // String parsing
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromString("mmol/l"))
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromString("MMOL/L"))
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromString("mmol"))
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromString("mg/dl"))
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromString("mgdl"))
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromString(null))
    }

    @Test
    fun testTimeOfDayDetection() {
        assertNotNull(TimeOfDay.fromHour(8))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(8))
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(12))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(18))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(23))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(3))
    }

    @Test
    fun testDefaultUserSettingsIntegrity() {
        val settings = UserSettings()
        assertEquals(1.50, settings.morningFactor, 0.001)
        assertEquals(1.00, settings.noonFactor, 0.001)
        assertEquals(1.20, settings.eveningFactor, 0.001)
        assertEquals(0.80, settings.nightFactor, 0.001)
        assertEquals(120.0, settings.targetGlucoseMgDl, 0.001)
        assertEquals(50.0, settings.correctionFactorMgDl, 0.001)
        assertEquals(0.5, settings.roundingStep, 0.001)
    }
}
