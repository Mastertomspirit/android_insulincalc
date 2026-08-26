package network.spiritscorp.model;

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
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive domain unit tests for core diabetes medical formulas,
 * units conversion, boundary checks, and time-of-day interval logic.
 */
public class InsulinDomainAndCalculationTest {

    private static final double DELTA = 0.001;

    private double roundToStep(double value, double step) {
        if (step <= 0.0) {
            return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        double factor = 1.0 / step;
        int scale = (step == 0.1 || step == 0.5) ? 1 : 0;
        return new BigDecimal(Math.round(value * factor) / factor)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Test
    public void testCarbUnitGramsConversions() {
        CarbUnit unit = CarbUnit.GRAMS;
        assertEquals("g KH", unit.getShortName());
        assertEquals("Gramm Kohlenhydrate", unit.getLabel());
        assertEquals(100.0, unit.toGrams(100.0), DELTA);
        assertEquals(0.0, unit.toGrams(0.0), DELTA);
        assertEquals(75.5, unit.fromGrams(75.5), DELTA);
    }

    @Test
    public void testCarbUnitKEConversions() {
        CarbUnit unit = CarbUnit.KE;
        assertEquals("KE", unit.getShortName());
        assertEquals("Kohlenhydrateinheit (10g)", unit.getLabel());
        // 1 KE = 10g
        assertEquals(10.0, unit.toGrams(1.0), DELTA);
        assertEquals(35.0, unit.toGrams(3.5), DELTA);
        assertEquals(4.2, unit.fromGrams(42.0), DELTA);
    }

    @Test
    public void testCarbUnitBEConversions() {
        CarbUnit unit = CarbUnit.BE;
        assertEquals("BE", unit.getShortName());
        assertEquals("Broteinheit (12g)", unit.getLabel());
        // 1 BE = 12g (standard German divisor)
        assertEquals(12.0, unit.toGrams(1.0), DELTA);
        assertEquals(48.0, unit.toGrams(4.0), DELTA);
        assertEquals(5.0, unit.fromGrams(60.0), DELTA);
    }

    @Test
    public void testGlucoseUnitConversionsAndFormulas() {
        GlucoseUnit mgDlUnit = GlucoseUnit.MG_DL;
        assertEquals("mg/dl", mgDlUnit.getShortName());
        assertEquals(120.0, mgDlUnit.toMgDl(120.0), DELTA);
        assertEquals(120.0, mgDlUnit.fromMgDl(120.0), DELTA);

        GlucoseUnit mmolUnit = GlucoseUnit.MMOL_L;
        assertEquals("mmol/l", mmolUnit.getShortName());
        // Conversion factor: 18.0182
        double mgDlValue = 180.182;
        double expectedMmol = 10.0;
        assertEquals(expectedMmol, mmolUnit.fromMgDl(mgDlValue), 0.01);
        assertEquals(mgDlValue, mmolUnit.toMgDl(expectedMmol), 0.01);
    }

    @Test
    public void testTimeOfDayFull24HourCoverage() {
        // Morning: 06:00 - 10:59
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(6));
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(8));
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(10));

        // Noon: 11:00 - 16:59
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(11));
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(13));
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(16));

        // Evening: 17:00 - 21:59
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(17));
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(19));
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(21));

        // Night: 22:00 - 05:59
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(22));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(23));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(0));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(3));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(5));
    }

    @Test
    public void testTimeOfDayMetadataAndProperties() {
        for (TimeOfDay tod : TimeOfDay.values()) {
            assertNotNull(tod.getTitle());
            assertNotNull(tod.getSubtitle());
            assertTrue(tod.getDefaultFactor() > 0.0);
            assertNotNull(tod.getIcon());
            assertNotNull(tod.getAccentColor());
        }
    }

    @Test
    public void testHypoglycemiaBoundaryCheck() {
        CalculationSummary summaryNormal = new CalculationSummary(
                40.0,
                4.0,
                3.33,
                1.0,
                3.33,
                90.0,
                100.0,
                -0.25,
                3.08,
                3.0,
                0.5,
                false,
                "Normal"
        );
        assertFalse(summaryNormal.isHypoRisk());

        CalculationSummary summaryHypo = new CalculationSummary(
                20.0,
                2.0,
                1.67,
                1.0,
                1.67,
                58.0,
                100.0,
                -1.05,
                0.62,
                0.5,
                0.5,
                true,
                "Achtung: Niedriger Blutzucker!"
        );
        assertTrue(summaryHypo.isHypoRisk());
    }

    @Test
    public void testInsulinRoundingPrecisionSteps() {
        // Step 0.5 (Standard insulin pen)
        assertEquals(4.0, roundToStep(3.8, 0.5), DELTA);
        assertEquals(3.5, roundToStep(3.7, 0.5), DELTA);
        assertEquals(3.5, roundToStep(3.3, 0.5), DELTA);
        assertEquals(3.0, roundToStep(3.2, 0.5), DELTA);
        assertEquals(0.0, roundToStep(0.2, 0.5), DELTA);
        assertEquals(0.5, roundToStep(0.3, 0.5), DELTA);

        // Step 0.1 (Insulin pump)
        assertEquals(4.3, roundToStep(4.28, 0.1), DELTA);
        assertEquals(4.2, roundToStep(4.24, 0.1), DELTA);
        assertEquals(0.1, roundToStep(0.09, 0.1), DELTA);

        // Step 1.0 (Full units)
        assertEquals(5.0, roundToStep(4.6, 1.0), DELTA);
        assertEquals(4.0, roundToStep(4.4, 1.0), DELTA);
    }

    @Test
    public void testUserSettingsCustomDivisorAndFactors() {
        UserSettings customSettings = new UserSettings(
                1,
                2.0,
                1.25,
                1.5,
                0.75,
                "KE",
                10,
                110.0,
                35.0,
                0.1,
                false,
                "EMERALD_MINT",
                "SYSTEM",
                "mg/dl"
        );

        assertEquals(2.0, customSettings.getMorningFactor(), DELTA);
        assertEquals(10, customSettings.getBeGramsDivisor());
        assertEquals("KE", customSettings.getDefaultCarbUnit());
        assertEquals("EMERALD_MINT", customSettings.getSelectedTheme());
        assertFalse(customSettings.getShowDisclaimer());
    }
}
