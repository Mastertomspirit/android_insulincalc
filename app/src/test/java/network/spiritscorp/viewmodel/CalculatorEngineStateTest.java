package network.spiritscorp.viewmodel;

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

import network.spiritscorp.model.CalculationSummary;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit & integration tests for the Calculator state computation engine in Java,
 * verifying accurate multi-step formulas, correction factors, unit toggles,
 * and user factor overrides.
 */
public class CalculatorEngineStateTest {

    private static final double DELTA = 0.001;

    private CalculationSummary calculateSummary(
            String carbInput,
            CarbUnit selectedUnit,
            TimeOfDay selectedTimeOfDay,
            Double factorOverride,
            String currentGlucoseInput,
            String targetGlucoseInput,
            String correctionFactorInput,
            boolean showCorrection,
            UserSettings settings
    ) {
        double rawInput = parseDoubleOrZero(carbInput);
        double beDivisor = settings.getBeGramsDivisor() > 0 ? settings.getBeGramsDivisor() : 12.0;

        double grams;
        switch (selectedUnit) {
            case GRAMS:
                grams = rawInput;
                break;
            case KE:
                grams = rawInput * 10.0;
                break;
            case BE:
            default:
                grams = rawInput * beDivisor;
                break;
        }

        double ke = grams / 10.0;
        double be = grams / 12.0;

        double factor;
        if (factorOverride != null) {
            factor = factorOverride;
        } else {
            switch (selectedTimeOfDay) {
                case MORNING:
                    factor = settings.getMorningFactor();
                    break;
                case NOON:
                    factor = settings.getNoonFactor();
                    break;
                case EVENING:
                    factor = settings.getEveningFactor();
                    break;
                case NIGHT:
                default:
                    factor = settings.getNightFactor();
                    break;
            }
        }

        double unitsCount;
        switch (selectedUnit) {
            case BE:
            case KE:
                unitsCount = rawInput;
                break;
            case GRAMS:
            default:
                unitsCount = grams / 12.0;
                break;
        }
        double mealInsulin = unitsCount * factor;

        double correctionInsulin = 0.0;
        Double currentBg = parseDoubleOrNull(currentGlucoseInput);
        Double targetBg = parseDoubleOrNull(targetGlucoseInput);
        Double corrFactor = parseDoubleOrNull(correctionFactorInput);

        boolean isHypoRisk = false;
        String advisory = "Standard-Dosis für die Mahlzeit";
        boolean isMmol = settings.getGlucoseUnit() != null && settings.getGlucoseUnit().toLowerCase().contains("mmol");
        double hypoThreshold = isMmol ? 3.9 : 70.0;

        if (showCorrection && currentBg != null && targetBg != null && corrFactor != null && corrFactor > 0) {
            if (currentBg < hypoThreshold) {
                isHypoRisk = true;
                advisory = "Achtung: Niedriger Blutzucker! Bitte zuerst 1-2 KE schnelle KH einnehmen.";
            } else if (currentBg < targetBg) {
                double diff = targetBg - currentBg;
                correctionInsulin = -(diff / corrFactor);
                advisory = "Blutzucker unter Zielbereich: Korrektur reduziert Gesamtdosis.";
            } else if (currentBg > targetBg) {
                double diff = currentBg - targetBg;
                correctionInsulin = diff / corrFactor;
                advisory = "Erhöhter Blutzucker: Korrektur-Bolus addiert.";
            }
        }

        double rawTotal = Math.max(0.0, mealInsulin + correctionInsulin);
        double roundingStep = settings.getRoundingStep();

        double factorStep = roundingStep > 0.0 ? 1.0 / roundingStep : 1.0;
        double roundedTotal;
        if (roundingStep > 0.0) {
            int scale = (roundingStep == 0.1 || roundingStep == 0.5) ? 1 : 0;
            roundedTotal = new BigDecimal(Math.round(rawTotal * factorStep) / factorStep)
                    .setScale(scale, RoundingMode.HALF_UP)
                    .doubleValue();
        } else {
            roundedTotal = new BigDecimal(rawTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return new CalculationSummary(
                new BigDecimal(grams).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                new BigDecimal(ke).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                new BigDecimal(be).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                factor,
                new BigDecimal(mealInsulin).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                currentBg,
                targetBg,
                new BigDecimal(correctionInsulin).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                new BigDecimal(rawTotal).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                roundedTotal,
                roundingStep,
                isHypoRisk,
                advisory
        );
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Test
    public void testMorningCalculationWithGrams() {
        UserSettings settings = new UserSettings();
        settings.setMorningFactor(1.50);
        settings.setRoundingStep(0.5);

        CalculationSummary summary = calculateSummary(
                "60",
                CarbUnit.GRAMS,
                TimeOfDay.MORNING,
                null,
                "",
                "100",
                "40",
                false,
                settings
        );

        // 60g KH / 12 = 5 BE. 5 BE * 1.5 = 7.5 IE
        assertEquals(60.0, summary.getCarbGrams(), DELTA);
        assertEquals(5.0, summary.getBeValue(), DELTA);
        assertEquals(6.0, summary.getKeValue(), DELTA);
        assertEquals(1.50, summary.getFactorUsed(), DELTA);
        assertEquals(7.50, summary.getMealInsulin(), DELTA);
        assertEquals(7.5, summary.getRoundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
    }

    @Test
    public void testNoonCalculationWithBEAndFactorOverride() {
        UserSettings settings = new UserSettings();
        settings.setNoonFactor(1.00);
        settings.setRoundingStep(0.5);

        // User overrides factor from 1.0 to 1.3
        CalculationSummary summary = calculateSummary(
                "4.5",
                CarbUnit.BE,
                TimeOfDay.NOON,
                1.30,
                "",
                "100",
                "40",
                false,
                settings
        );

        // 4.5 BE * 12 = 54.0g KH. 4.5 BE * 1.30 = 5.85 IE -> Rounded to step 0.5 = 6.0 IE
        assertEquals(54.0, summary.getCarbGrams(), DELTA);
        assertEquals(4.5, summary.getBeValue(), DELTA);
        assertEquals(1.30, summary.getFactorUsed(), DELTA);
        assertEquals(5.85, summary.getMealInsulin(), DELTA);
        assertEquals(6.0, summary.getRoundedTotalInsulin(), DELTA);
    }

    @Test
    public void testEveningCalculationWithHighGlucoseCorrection() {
        UserSettings settings = new UserSettings();
        settings.setEveningFactor(1.20);
        settings.setRoundingStep(0.5);

        // 3.0 BE (36g KH) -> 3.0 * 1.2 = 3.6 IE
        // Current BG 220, Target 100, CorrFactor 40 -> (220-100)/40 = +3.0 IE correction
        // Total = 3.6 + 3.0 = 6.6 IE -> Rounded to step 0.5 = 6.5 IE
        CalculationSummary summary = calculateSummary(
                "3.0",
                CarbUnit.BE,
                TimeOfDay.EVENING,
                null,
                "220",
                "100",
                "40",
                true,
                settings
        );

        assertEquals(36.0, summary.getCarbGrams(), DELTA);
        assertEquals(3.60, summary.getMealInsulin(), DELTA);
        assertEquals(3.00, summary.getCorrectionInsulin(), DELTA);
        assertEquals(6.60, summary.getRawTotalInsulin(), DELTA);
        assertEquals(6.5, summary.getRoundedTotalInsulin(), DELTA);
        assertTrue(summary.getAdvisoryNote().contains("Erhöhter Blutzucker"));
    }

    @Test
    public void testLowBloodGlucoseWarningAndDoseReduction() {
        UserSettings settings = new UserSettings();
        settings.setMorningFactor(1.50);
        settings.setRoundingStep(0.5);

        // Current BG = 65 (Hypo risk < 70)
        CalculationSummary summary = calculateSummary(
                "30",
                CarbUnit.GRAMS,
                TimeOfDay.MORNING,
                null,
                "65",
                "100",
                "40",
                true,
                settings
        );

        assertTrue(summary.isHypoRisk());
        assertTrue(summary.getAdvisoryNote().contains("Achtung: Niedriger Blutzucker"));
    }

    @Test
    public void testNegativeCorrectionDoseReduction() {
        UserSettings settings = new UserSettings();
        settings.setNoonFactor(1.0);
        settings.setRoundingStep(0.5);

        // 2 BE = 2.0 IE meal insulin. BG = 80, Target = 100, Corr = 40
        // Correction = -(20 / 40) = -0.5 IE
        // Total = 2.0 - 0.5 = 1.5 IE
        CalculationSummary summary = calculateSummary(
                "2.0",
                CarbUnit.BE,
                TimeOfDay.NOON,
                null,
                "80",
                "100",
                "40",
                true,
                settings
        );

        assertEquals(2.0, summary.getMealInsulin(), DELTA);
        assertEquals(-0.50, summary.getCorrectionInsulin(), DELTA);
        assertEquals(1.50, summary.getRawTotalInsulin(), DELTA);
        assertEquals(1.5, summary.getRoundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
        assertTrue(summary.getAdvisoryNote().contains("reduziert"));
    }

    @Test
    public void testMmolLCorrectionCalculation() {
        UserSettings settings = new UserSettings();
        settings.setEveningFactor(1.00);
        settings.setGlucoseUnit("mmol/l");
        settings.setTargetGlucoseMgDl(120.0);
        settings.setCorrectionFactorMgDl(50.0);
        settings.setRoundingStep(0.5);

        // 3 KE = 3.0 IE meal insulin
        // Target: 6.7 mmol/l, Current BG: 12.3 mmol/l, CorrFactor: 2.8 mmol/l pro IE
        // Diff = 12.3 - 6.7 = 5.6 mmol/l
        // Correction = 5.6 / 2.8 = 2.0 IE
        // Total = 3.0 + 2.0 = 5.0 IE
        CalculationSummary summary = calculateSummary(
                "3.0",
                CarbUnit.KE,
                TimeOfDay.EVENING,
                null,
                "12.3",
                "6.7",
                "2.8",
                true,
                settings
        );

        assertEquals(30.0, summary.getCarbGrams(), DELTA);
        assertEquals(3.00, summary.getMealInsulin(), DELTA);
        assertEquals(2.00, summary.getCorrectionInsulin(), DELTA);
        assertEquals(5.00, summary.getRawTotalInsulin(), DELTA);
        assertEquals(5.0, summary.getRoundedTotalInsulin(), DELTA);
        assertTrue(summary.getAdvisoryNote().contains("Erhöhter Blutzucker"));
    }

    @Test
    public void testSanitizeCarbInput() {
        assertEquals("45.5", sanitize("45,5"));
        assertEquals("12.0", sanitize("12.0"));
        assertEquals("60", sanitize("60g"));
        assertEquals("", sanitize("45.5.5"));
    }

    private String sanitize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        int dotsCount = 0;
        for (char c : input.replace(',', '.').toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '.') {
                dotsCount++;
                sb.append(c);
            }
        }
        String sanitized = sb.toString();
        return (dotsCount <= 1 && sanitized.length() <= 8) ? sanitized : "";
    }
}
