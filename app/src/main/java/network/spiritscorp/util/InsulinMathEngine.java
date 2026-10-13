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
 * Pure Java calculation engine for diabetic insulin dosages, carbohydrate conversions (grams, BE, KE),
 * correction boluses, blood glucose unit transformations, and hypoglycemia risk detection.
 * <p>
 * Encapsulates all core mathematical algorithms required for precision insulin therapy calculations
 * following standard diabetology formulas.
 */
public final class InsulinMathEngine {

    /** Hypoglycemia threshold in milligrams per deciliter (mg/dl). Values below 70.0 indicate low blood sugar risk. */
    private static final double HYPO_THRESHOLD_MG_DL = 70.0;
    /** Hypoglycemia threshold in millimoles per liter (mmol/l). Values below 3.9 indicate low blood sugar risk. */
    private static final double HYPO_THRESHOLD_MMOL_L = 3.9;
    /** Minimum plausible blood glucose value in mmol/l for validity checks. */
    private static final double MIN_PLAUSIBLE_MMOL = 0;
    /** Minimum plausible blood glucose value in mg/dl for validity checks. */
    private static final double MIN_PLAUSIBLE_MG_DL = 30;

    private InsulinMathEngine() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a raw carbohydrate input value to absolute grams based on the specified unit type.
     * <p>
     * Supports GRAMS (direct), BE (Broteinheiten using custom gram divisor, e.g. 12g),
     * and KE (Kohlenhydrateinheiten, fixed at 10g per KE).
     *
     * @param rawInput       The user's numeric carbohydrate input.
     * @param unit           The selected {@link CarbUnit} (GRAMS, BE, KE).
     * @param beGramsDivisor The number of grams per BE (defaults to 12 if <= 0).
     * @return The calculated equivalent in absolute grams of carbohydrates.
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
     * Calculates Kohlenhydrateinheiten (KE) from absolute carbohydrate grams (1 KE = 10g KH).
     *
     * @param carbGrams Absolute carbohydrate amount in grams.
     * @return Equivalent value in KE units.
     */
    public static double calculateKe(double carbGrams) {
        if (carbGrams <= 0) return 0.0;
        return carbGrams / 10.0;
    }

    /**
     * Calculates Broteinheiten (BE) from absolute carbohydrate grams using a custom divisor.
     *
     * @param carbGrams    Absolute carbohydrate amount in grams.
     * @param beGramsDivisor Grams per BE divisor (defaults to 12 if <= 0).
     * @return Equivalent value in BE units.
     */
    public static double calculateBe(double carbGrams, int beGramsDivisor) {
        if (carbGrams <= 0) return 0.0;
        double divisor = beGramsDivisor > 0 ? beGramsDivisor : 12.0;
        return carbGrams / divisor;
    }

    /**
     * Calculates the required meal insulin dose (in International Units, IE) based on carbohydrate units,
     * input amount, insulin factor, and BE divisors.
     *
     * @param rawInput      Raw carbohydrate input value.
     * @param unit          The selected {@link CarbUnit}.
     * @param carbGrams     Calculated total grams of carbohydrates.
     * @param insulinFactor Personal insulin-to-carb factor for the current time of day (IE per unit/carb).
     * @param beGramsDivisor Grams per BE divisor.
     * @return Calculated meal insulin dosage in IE.
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
     * Calculates the blood glucose correction bolus (positive for hyperglycemia, negative for hypoglycemia adjustments).
     *
     * @param showCorrection Whether correction calculation is enabled.
     * @param currentBg      Current measured blood glucose level.
     * @param targetBg       Target blood glucose level.
     * @param corrFactor     Correction factor (mg/dl drop per 1 IE insulin).
     * @return Calculated correction insulin in IE (positive adds insulin, negative reduces it).
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
     * Evaluates whether a given blood glucose measurement indicates hypoglycemia (low blood sugar).
     *
     * @param currentBg Measured blood glucose value.
     * @param isMmol    True if measurement is in mmol/l, false if in mg/dl.
     * @return True if glucose is within plausible range and below the hypoglycemia threshold.
     */
    public static boolean isHypoglycemia(Double currentBg, boolean isMmol) {
        if (currentBg == null) return false;
        double minPlausible = isMmol ? MIN_PLAUSIBLE_MMOL : MIN_PLAUSIBLE_MG_DL;
        double threshold = isMmol ? HYPO_THRESHOLD_MMOL_L : HYPO_THRESHOLD_MG_DL;
        return currentBg >= minPlausible && currentBg < threshold;
    }

    /**
     * Rounds an insulin dosage value to specified fractional steps (e.g., 0.1, 0.5, or 1.0 units).
     *
     * @param value Exact calculated insulin units.
     * @param step  Rounding step increment (e.g. 0.5).
     * @return Rounded insulin units adhering to half-up rounding mode.
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
     * Rounds a double precision value to a specified number of decimal places using HALF_UP rounding.
     *
     * @param value    Raw double value.
     * @param decimals Target decimal places count.
     * @return Rounded double value.
     */
    public static double roundToDecimals(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }
}
