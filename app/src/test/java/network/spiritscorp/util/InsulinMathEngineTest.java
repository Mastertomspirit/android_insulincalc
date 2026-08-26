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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure Java unit tests for {@link InsulinMathEngine}.
 */
public class InsulinMathEngineTest {

    private static final double DELTA = 0.001;

    @Test
    public void testConvertToGrams() {
        assertEquals(0.0, InsulinMathEngine.convertToGrams(0.0, "g KH"), DELTA);
        assertEquals(0.0, InsulinMathEngine.convertToGrams(-5.0, "BE"), DELTA);

        // Grams
        assertEquals(55.0, InsulinMathEngine.convertToGrams(55.0, "g KH"), DELTA);
        assertEquals(55.0, InsulinMathEngine.convertToGrams(55.0, "g"), DELTA);

        // BE (12g)
        assertEquals(12.0, InsulinMathEngine.convertToGrams(1.0, "BE"), DELTA);
        assertEquals(48.0, InsulinMathEngine.convertToGrams(4.0, "BE"), DELTA);
        assertEquals(30.0, InsulinMathEngine.convertToGrams(2.5, "be"), DELTA);

        // KE (10g)
        assertEquals(10.0, InsulinMathEngine.convertToGrams(1.0, "KE"), DELTA);
        assertEquals(35.0, InsulinMathEngine.convertToGrams(3.5, "ke"), DELTA);
    }

    @Test
    public void testKeAndBeCalculation() {
        assertEquals(0.0, InsulinMathEngine.calculateKe(0.0), DELTA);
        assertEquals(0.0, InsulinMathEngine.calculateBe(0.0), DELTA);

        // 60g KH -> 6.0 KE, 5.0 BE
        assertEquals(6.0, InsulinMathEngine.calculateKe(60.0), DELTA);
        assertEquals(5.0, InsulinMathEngine.calculateBe(60.0), DELTA);

        // 24g KH -> 2.4 KE, 2.0 BE
        assertEquals(2.4, InsulinMathEngine.calculateKe(24.0), DELTA);
        assertEquals(2.0, InsulinMathEngine.calculateBe(24.0), DELTA);
    }

    @Test
    public void testMealInsulinCalculation() {
        // Zero / negative inputs
        assertEquals(0.0, InsulinMathEngine.calculateMealInsulin(0.0, "BE", 0.0, 1.5), DELTA);
        assertEquals(0.0, InsulinMathEngine.calculateMealInsulin(4.0, "BE", 48.0, 0.0), DELTA);

        // 4 BE * 1.5 factor = 6.0 IE
        assertEquals(6.0, InsulinMathEngine.calculateMealInsulin(4.0, "BE", 48.0, 1.5), DELTA);

        // 3 KE * 1.2 factor = 3.6 IE
        assertEquals(3.6, InsulinMathEngine.calculateMealInsulin(3.0, "KE", 30.0, 1.2), DELTA);

        // 48g KH with Grams input (48 / 12 = 4 units * 1.5 factor = 6.0 IE)
        assertEquals(6.0, InsulinMathEngine.calculateMealInsulin(48.0, "g KH", 48.0, 1.5), DELTA);
    }

    @Test
    public void testCorrectionInsulinCalculation() {
        // Correction disabled
        assertEquals(0.0, InsulinMathEngine.calculateCorrectionInsulin(false, 180.0, 100.0, 40.0), DELTA);

        // Null parameters
        assertEquals(0.0, InsulinMathEngine.calculateCorrectionInsulin(true, null, 100.0, 40.0), DELTA);
        assertEquals(0.0, InsulinMathEngine.calculateCorrectionInsulin(true, 180.0, null, 40.0), DELTA);
        assertEquals(0.0, InsulinMathEngine.calculateCorrectionInsulin(true, 180.0, 100.0, null), DELTA);
        assertEquals(0.0, InsulinMathEngine.calculateCorrectionInsulin(true, 180.0, 100.0, 0.0), DELTA);

        // Positive correction: Current BG = 180, Target = 100, CorrFactor = 40 -> +2.0 IE
        double posCorrection = InsulinMathEngine.calculateCorrectionInsulin(true, 180.0, 100.0, 40.0);
        assertEquals(2.0, posCorrection, DELTA);

        // Negative correction (below target): Current BG = 70, Target = 110, CorrFactor = 40 -> -(40/40) = -1.0 IE
        double negCorrection = InsulinMathEngine.calculateCorrectionInsulin(true, 70.0, 110.0, 40.0);
        assertEquals(-1.0, negCorrection, DELTA);

        // On target
        assertEquals(0.0, InsulinMathEngine.calculateCorrectionInsulin(true, 120.0, 120.0, 40.0), DELTA);
    }

    @Test
    public void testHypoglycemiaDetection() {
        assertFalse(InsulinMathEngine.isHypoglycemia(null, false));
        assertFalse(InsulinMathEngine.isHypoglycemia(null, true));

        // mg/dL mode (< 70 mg/dL is hypo)
        assertTrue(InsulinMathEngine.isHypoglycemia(69.0, false));
        assertTrue(InsulinMathEngine.isHypoglycemia(55.0, false));
        assertFalse(InsulinMathEngine.isHypoglycemia(70.0, false));
        assertFalse(InsulinMathEngine.isHypoglycemia(110.0, false));

        // mmol/L mode (< 3.9 mmol/L is hypo)
        assertTrue(InsulinMathEngine.isHypoglycemia(3.8, true));
        assertTrue(InsulinMathEngine.isHypoglycemia(2.9, true));
        assertFalse(InsulinMathEngine.isHypoglycemia(3.9, true));
        assertFalse(InsulinMathEngine.isHypoglycemia(6.5, true));
    }

    @Test
    public void testRoundingSteps() {
        // Step 0.5 (Insulin Pen)
        assertEquals(3.5, InsulinMathEngine.roundToStep(3.6, 0.5), DELTA);
        assertEquals(4.0, InsulinMathEngine.roundToStep(3.8, 0.5), DELTA);
        assertEquals(3.5, InsulinMathEngine.roundToStep(3.3, 0.5), DELTA);
        assertEquals(3.0, InsulinMathEngine.roundToStep(3.1, 0.5), DELTA);

        // Step 0.1 (Insulin Pump)
        assertEquals(4.3, InsulinMathEngine.roundToStep(4.28, 0.1), DELTA);
        assertEquals(4.2, InsulinMathEngine.roundToStep(4.24, 0.1), DELTA);

        // Step 1.0 (Whole units)
        assertEquals(5.0, InsulinMathEngine.roundToStep(4.6, 1.0), DELTA);
        assertEquals(4.0, InsulinMathEngine.roundToStep(4.4, 1.0), DELTA);

        // Invalid or zero step defaults to 2 decimals
        assertEquals(3.46, InsulinMathEngine.roundToStep(3.456, 0.0), DELTA);
    }

    @Test
    public void testRoundToDecimals() {
        assertEquals(0.0, InsulinMathEngine.roundToDecimals(Double.NaN, 2), DELTA);
        assertEquals(0.0, InsulinMathEngine.roundToDecimals(Double.POSITIVE_INFINITY, 2), DELTA);
        assertEquals(3.14, InsulinMathEngine.roundToDecimals(3.14159, 2), DELTA);
        assertEquals(3.142, InsulinMathEngine.roundToDecimals(3.14159, 3), DELTA);
        assertEquals(3.1, InsulinMathEngine.roundToDecimals(3.14159, 1), DELTA);
    }

    @Test
    public void testMgDlAndMmolConversion() {
        // 180 mg/dL ~= 10.0 mmol/L
        assertEquals(10.0, InsulinMathEngine.convertMgDlToMmol(180.182), 0.1);
        // 10.0 mmol/L ~= 180 mg/dL
        assertEquals(180.0, InsulinMathEngine.convertMmolToMgDl(10.0), 1.0);
    }
}
