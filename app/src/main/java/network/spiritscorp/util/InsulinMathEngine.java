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

/**
 * Pure Java calculation engine for diabetic insulin dosages, carbohydrate conversions,
 * correction bolus math, and hypo risk detection.
 */
public final class InsulinMathEngine {

    public static final double HYPO_THRESHOLD_MG_DL = 70.0;
    public static final double HYPO_THRESHOLD_MMOL_L = 3.9;
    public static final double MMOL_CONVERSION_FACTOR = 18.0182;

    private InsulinMathEngine() {
        // Utility class
    }

    /**
     * Converts a raw carbohydrate input value to grams based on the selected unit.
     */
    public static double convertToGrams(double rawInput, String unitShortName) {
        if (rawInput <= 0) return 0.0;
        if ("BE".equalsIgnoreCase(unitShortName)) {
            return rawInput * 12.0;
        } else if ("KE".equalsIgnoreCase(unitShortName)) {
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
     * Calculates Broteinheiten (1 BE = 12g KH).
     */
    public static double calculateBe(double carbGrams) {
        if (carbGrams <= 0) return 0.0;
        return carbGrams / 12.0;
    }

    /**
     * Calculates meal insulin based on unit, input, grams and user factor.
     */
    public static double calculateMealInsulin(double rawInput, String unitShortName, double carbGrams, double insulinFactor) {
        if (rawInput <= 0 || carbGrams <= 0 || insulinFactor <= 0) return 0.0;
        double unitsCount;
        if ("BE".equalsIgnoreCase(unitShortName) || "KE".equalsIgnoreCase(unitShortName)) {
            unitsCount = rawInput;
        } else {
            unitsCount = carbGrams / 12.0;
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
        double threshold = isMmol ? HYPO_THRESHOLD_MMOL_L : HYPO_THRESHOLD_MG_DL;
        return currentBg < threshold;
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

    /**
     * Converts blood glucose between mg/dL and mmol/L.
     */
    public static double convertMgDlToMmol(double mgDl) {
        return roundToDecimals(mgDl / MMOL_CONVERSION_FACTOR, 1);
    }

    public static double convertMmolToMgDl(double mmol) {
        return roundToDecimals(mmol * MMOL_CONVERSION_FACTOR, 0);
    }
}
