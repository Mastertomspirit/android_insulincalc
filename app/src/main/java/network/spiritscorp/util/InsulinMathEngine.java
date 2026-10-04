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

import java.math.BigDecimal;
import java.math.RoundingMode;
import network.spiritscorp.model.CarbUnit;

/**
 * Pure Java calculation engine for diabetic insulin dosages, carbohydrate conversions,
 * correction bolus math, and hypo risk detection.
 */
public final class InsulinMathEngine {

    private static final double HYPO_THRESHOLD_MG_DL = 70.0;
    private static final double HYPO_THRESHOLD_MMOL_L = 3.9;
    private static final double MIN_PLAUSIBLE_MMOL = 0;
    private static final double MIN_PLAUSIBLE_MG_DL = 30;

    private InsulinMathEngine() {
        // Utility class
    }

    /**
     * Converts a raw carbohydrate input value to grams based on the selected CarbUnit.
     */
    public static double convertToGrams(double rawInput, CarbUnit unit, int beGramsDivisor) {
        if (rawInput <= 0 || unit == null) return 0.0;
        if (unit == CarbUnit.BE) {
            double divisor = beGramsDivisor > 0 ? beGramsDivisor : 12.0;
            return rawInput * divisor;
        } else if (unit == CarbUnit.KE) {
            return rawInput * 10.0;
        }
        return rawInput;
    }

    /**
     * Calculates Kohlenhydrateinheiten (1 KE = 10g KH).
     */
    public static double calculateKe(double carbGrams) {
        if (carbGrams <= 0) return 0.0;
        return carbGrams / 10.0;
    }

    /**
     * Calculates Broteinheiten based on custom BE divisor.
     */
    public static double calculateBe(double carbGrams, int beGramsDivisor) {
        if (carbGrams <= 0) return 0.0;
        double divisor = beGramsDivisor > 0 ? beGramsDivisor : 12.0;
        return carbGrams / divisor;
    }

    /**
     * Calculates meal insulin based on CarbUnit, input, grams, user factor, and BE divisor.
     */
    public static double calculateMealInsulin(double rawInput, CarbUnit unit, double carbGrams, double insulinFactor, int beGramsDivisor) {
        if (rawInput <= 0 || carbGrams <= 0 || insulinFactor <= 0 || unit == null) return 0.0;
        double unitsCount;
        if (unit == CarbUnit.BE) {
            double divisor = beGramsDivisor > 0 ? beGramsDivisor : 12.0;
            unitsCount = carbGrams / divisor;
        } else if (unit == CarbUnit.KE) {
            unitsCount = carbGrams / 10.0;
        } else {
            double divisor = beGramsDivisor > 0 ? beGramsDivisor : 12.0;
            unitsCount = carbGrams / divisor;
        }
        return unitsCount * insulinFactor;
    }

    /**
     * Calculates correction insulin dose (positive or negative).
     */
    public static double calculateCorrectionInsulin(boolean showCorrection, Double currentBg, Double targetBg, Double corrFactor) {
        if (!showCorrection || currentBg == null || targetBg == null || corrFactor == null || corrFactor <= 0) {
            return 0.0;
        }
        if (currentBg > targetBg) {
            double diff = currentBg - targetBg;
            return diff / corrFactor;
        } else if (currentBg < targetBg) {
            double diff = targetBg - currentBg;
            return -(diff / corrFactor);
        }
        return 0.0;
    }

    /**
     * Checks if the given blood glucose level indicates hypoglycemia.
     */
    public static boolean isHypoglycemia(Double currentBg, boolean isMmol) {
        if (currentBg == null) return false;
        double minPlausible = isMmol ? MIN_PLAUSIBLE_MMOL : MIN_PLAUSIBLE_MG_DL;
        double threshold = isMmol ? HYPO_THRESHOLD_MMOL_L : HYPO_THRESHOLD_MG_DL;
        return currentBg >= minPlausible && currentBg < threshold;
    }

    /**
     * Rounds insulin units to custom steps (e.g. 0.1, 0.5, 1.0).
     */
    public static double roundToStep(double value, double step) {
        if (step <= 0.0) return roundToDecimals(value, 2);
        double factor = 1.0 / step;
        int scale = (step == 0.1 || step == 0.5) ? 1 : 0;
        return BigDecimal.valueOf(Math.round(value * factor) / factor)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Rounds a double to specified decimal places.
     */
    public static double roundToDecimals(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }
}
