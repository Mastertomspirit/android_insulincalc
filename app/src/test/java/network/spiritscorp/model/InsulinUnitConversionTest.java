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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests verifying unit conversions, blood glucose calculations,
 * and default user settings integrity.
 */
public class InsulinUnitConversionTest {

    private static final double DELTA = 0.001;

    @Test
    public void testCarbUnitConversionFormulas() {
        // Grams
        assertEquals(50.0, CarbUnit.GRAMS.toGrams(50.0), DELTA);
        assertEquals(50.0, CarbUnit.GRAMS.fromGrams(50.0), DELTA);

        // KE (1 KE = 10g)
        assertEquals(40.0, CarbUnit.KE.toGrams(4.0), DELTA);
        assertEquals(4.5, CarbUnit.KE.fromGrams(45.0), DELTA);

        // BE (1 BE = 12g)
        assertEquals(36.0, CarbUnit.BE.toGrams(3.0), DELTA);
        assertEquals(3.0, CarbUnit.BE.fromGrams(36.0), DELTA);
    }

    @Test
    public void testGlucoseUnitConversion() {
        // 100 mg/dl in mmol/l: 100 / 18.0182 ≈ 5.55 mmol/l
        double mmol = GlucoseUnit.MMOL_L.fromMgDl(100.0);
        assertEquals(5.55, mmol, 0.01);

        // 5.55 mmol/l in mg/dl: 5.55 * 18.0182 ≈ 100 mg/dl
        double mgDl = GlucoseUnit.MMOL_L.toMgDl(5.55);
        assertEquals(100.0, mgDl, 0.1);

        // 120 mg/dl in mmol/l: 120 / 18.0182 ≈ 6.66 mmol/l
        double mmol120 = GlucoseUnit.MMOL_L.fromMgDl(120.0);
        assertEquals(6.66, mmol120, 0.01);

        // String parsing
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromString("mmol/l"));
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromString("MMOL/L"));
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromString("mmol"));
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromString("mg/dl"));
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromString("mgdl"));
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromString(null));
    }

    @Test
    public void testTimeOfDayDetection() {
        assertNotNull(TimeOfDay.fromHour(8));
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(8));
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromHour(12));
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(18));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(23));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(3));
    }

    @Test
    public void testDefaultUserSettingsIntegrity() {
        UserSettings settings = new UserSettings();
        assertEquals(1.50, settings.getMorningFactor(), DELTA);
        assertEquals(1.00, settings.getNoonFactor(), DELTA);
        assertEquals(1.20, settings.getEveningFactor(), DELTA);
        assertEquals(0.80, settings.getNightFactor(), DELTA);
        assertEquals(120.0, settings.getTargetGlucoseMgDl(), DELTA);
        assertEquals(50.0, settings.getCorrectionFactorMgDl(), DELTA);
        assertEquals(0.5, settings.getRoundingStep(), DELTA);
    }
}
