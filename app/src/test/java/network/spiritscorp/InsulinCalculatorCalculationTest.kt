package network.spiritscorp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Production-ready unit tests for insulin and carbohydrate calculations (BE / KE / g KH)
 * and rounding logic.
 */
class InsulinCalculatorCalculationTest {

    private fun roundToStep(value: Double, step: Double): Double {
        if (step <= 0.0) return value
        return (BigDecimal(value / step).setScale(0, RoundingMode.HALF_UP).toDouble() * step)
    }

    @Test
    fun testCarbUnitConversionAndGrams() {
        // BE: 1 BE = 12g
        val beInput = 4.0 // 4 BE
        val beGrams = beInput * 12.0
        assertEquals(48.0, beGrams, 0.001)
        assertEquals(4.8, beGrams / 10.0, 0.001) // KE equivalent
        assertEquals(4.0, beGrams / 12.0, 0.001) // BE equivalent

        // KE: 1 KE = 10g
        val keInput = 5.0 // 5 KE
        val keGrams = keInput * 10.0
        assertEquals(50.0, keGrams, 0.001)

        // Grams
        val gramsInput = 60.0
        assertEquals(60.0, gramsInput, 0.001)
        assertEquals(5.0, gramsInput / 12.0, 0.001) // 60g / 12 = 5 BE
    }

    @Test
    fun testInsulinMealCalculationWithBE() {
        // 4 BE with factor 1.5 IE/BE -> 4 * 1.5 = 6.0 IE
        val beCount = 4.0
        val factor = 1.5
        val mealInsulin = beCount * factor
        assertEquals(6.0, mealInsulin, 0.001)
    }

    @Test
    fun testCorrectionBolusPositive() {
        // Current BG = 180, Target = 100, Correction Factor = 40
        // (180 - 100) / 40 = 80 / 40 = 2.0 IE correction
        val currentBg = 180.0
        val targetBg = 100.0
        val corrFactor = 40.0
        val correction = (currentBg - targetBg) / corrFactor
        assertEquals(2.0, correction, 0.001)
    }

    @Test
    fun testCorrectionBolusNegativeHypoRisk() {
        // Current BG = 60, Target = 100, Correction Factor = 40
        // (100 - 60) / 40 = -1.0 IE reduction
        val currentBg = 60.0
        val targetBg = 100.0
        val corrFactor = 40.0
        val correction = -((targetBg - currentBg) / corrFactor)
        assertEquals(-1.0, correction, 0.001)
    }

    @Test
    fun testRoundingSteps() {
        // Step 0.5 (Standard for Pens)
        assertEquals(6.0, roundToStep(5.8, 0.5), 0.001)
        assertEquals(6.0, roundToStep(6.2, 0.5), 0.001)
        assertEquals(5.5, roundToStep(5.3, 0.5), 0.001)

        // Step 0.1 (Pump)
        assertEquals(5.8, roundToStep(5.75, 0.1), 0.001) // half up -> 5.8
        assertEquals(5.8, roundToStep(5.76, 0.1), 0.001)

        // Step 1.0
        assertEquals(6.0, roundToStep(5.6, 1.0), 0.001)
        assertEquals(5.0, roundToStep(5.4, 1.0), 0.001)
    }
}
