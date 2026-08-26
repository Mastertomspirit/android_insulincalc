package network.spiritscorp.util;

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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Production-ready unit tests for insulin and carbohydrate calculations (BE / KE / g KH)
 * and rounding logic delegating to {@link InsulinMathEngine}.
 */
public class InsulinCalculatorCalculationTest {

    private static final double DELTA = 0.001;

    @Test
    public void testCarbUnitConversionAndGrams() {
        // BE: 1 BE = 12g
        double beInput = 4.0; // 4 BE
        double beGrams = InsulinMathEngine.convertToGrams(beInput, "BE");
        assertEquals(48.0, beGrams, DELTA);
        assertEquals(4.8, InsulinMathEngine.calculateKe(beGrams), DELTA); // KE equivalent
        assertEquals(4.0, InsulinMathEngine.calculateBe(beGrams), DELTA); // BE equivalent

        // KE: 1 KE = 10g
        double keInput = 5.0; // 5 KE
        double keGrams = InsulinMathEngine.convertToGrams(keInput, "KE");
        assertEquals(50.0, keGrams, DELTA);

        // Grams
        double gramsInput = 60.0;
        assertEquals(60.0, InsulinMathEngine.convertToGrams(gramsInput, "g KH"), DELTA);
        assertEquals(5.0, InsulinMathEngine.calculateBe(gramsInput), DELTA); // 60g / 12 = 5 BE
    }

    @Test
    public void testInsulinMealCalculationWithBE() {
        // 4 BE with factor 1.5 IE/BE -> 4 * 1.5 = 6.0 IE
        double mealInsulin = InsulinMathEngine.calculateMealInsulin(4.0, "BE", 48.0, 1.5);
        assertEquals(6.0, mealInsulin, DELTA);
    }

    @Test
    public void testCorrectionBolusPositive() {
        // Current BG = 180, Target = 100, Correction Factor = 40
        // (180 - 100) / 40 = 80 / 40 = 2.0 IE correction
        double correction = InsulinMathEngine.calculateCorrectionInsulin(true, 180.0, 100.0, 40.0);
        assertEquals(2.0, correction, DELTA);
    }

    @Test
    public void testCorrectionBolusNegativeHypoRisk() {
        // Current BG = 60, Target = 100, Correction Factor = 40
        // (100 - 60) / 40 = -1.0 IE reduction
        double correction = InsulinMathEngine.calculateCorrectionInsulin(true, 60.0, 100.0, 40.0);
        assertEquals(-1.0, correction, DELTA);
    }

    @Test
    public void testRoundingSteps() {
        // Step 0.5 (Standard for Pens)
        assertEquals(6.0, InsulinMathEngine.roundToStep(5.8, 0.5), DELTA);
        assertEquals(6.0, InsulinMathEngine.roundToStep(6.2, 0.5), DELTA);
        assertEquals(5.5, InsulinMathEngine.roundToStep(5.3, 0.5), DELTA);

        // Step 0.1 (Pump)
        assertEquals(5.8, InsulinMathEngine.roundToStep(5.75, 0.1), DELTA); // half up -> 5.8
        assertEquals(5.8, InsulinMathEngine.roundToStep(5.76, 0.1), DELTA);

        // Step 1.0
        assertEquals(6.0, InsulinMathEngine.roundToStep(5.6, 1.0), DELTA);
        assertEquals(5.0, InsulinMathEngine.roundToStep(5.4, 1.0), DELTA);
    }
}
