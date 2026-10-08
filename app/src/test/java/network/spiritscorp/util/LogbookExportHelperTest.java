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

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import network.spiritscorp.R;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.GlucoseUnit;
import network.spiritscorp.model.TimeOfDay;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure Java unit tests for {@link LogbookExportHelper} object instance.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34, qualifiers = "de")
public class LogbookExportHelperTest {

    private static final double DELTA = 0.001;
    private LogbookExportHelper exportHelper;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        exportHelper = new LogbookExportHelper(Locale.GERMANY, context);
    }

    private List<CalculationLog> createSampleLogs() {
        return Arrays.asList(
                new CalculationLog(
                        1L,
                        1700000000000L,
                        "Frühstück",
                        40.0,
                        CarbUnit.GRAMS,
                        40.0,
                        3.33,
                        4.0,
                        TimeOfDay.MORNING,
                        1.5,
                        6.0,
                        130.0,
                        100.0,
                        40.0,
                        0.75,
                        6.75,
                        7.0,
                        "Haferflocken"
                ),
                new CalculationLog(
                        2L,
                        1700020000000L,
                        "Mittagessen",
                        50.0,
                        CarbUnit.GRAMS,
                        50.0,
                        4.17,
                        5.0,
                        TimeOfDay.NOON,
                        1.0,
                        5.0,
                        110.0,
                        100.0,
                        40.0,
                        0.25,
                        5.25,
                        5.5,
                        "Salat mit Brot"
                ),
                new CalculationLog(
                        3L,
                        1700040000000L,
                        "Abendessen",
                        30.0,
                        CarbUnit.GRAMS,
                        30.0,
                        2.5,
                        3.0,
                        TimeOfDay.EVENING,
                        1.2,
                        3.6,
                        null,
                        null,
                        null,
                        null,
                        3.6,
                        3.5,
                        "Suppe"
                )
        );
    }

    @Test
    public void testGenerateExportTextWithEmptyList() {
        String textNull = exportHelper.generateExportText(null, "Heute", GlucoseUnit.MG_DL);
        assertTrue(textNull.contains(context.getString(R.string.logbook_export_helper_no_entries_for_period, "Heute")));

        String textEmpty = exportHelper.generateExportText(Collections.emptyList(), "Letzte 7 Tage", GlucoseUnit.MG_DL);
        assertTrue(textEmpty.contains(context.getString(R.string.logbook_export_helper_no_entries_for_period, "Letzte 7 Tage")));
        assertTrue(textEmpty.contains("Letzte 7 Tage"));
    }

    @Test
    public void testGenerateExportTextWithLogs() {
        List<CalculationLog> logs = createSampleLogs();
        String report = exportHelper.generateExportText(logs, "Alle Einträge", GlucoseUnit.MG_DL);

        assertNotNull(report);
        assertTrue(report.contains(context.getString(R.string.logbook_export_helper_title_header)));
        assertTrue(report.contains(context.getString(R.string.logbook_export_helper_entries_label) + ": 3"));
        assertTrue(report.contains(context.getString(R.string.logbook_export_helper_total_carbs_label) + ": 120 g"));
        assertTrue(report.contains("Frühstück"));
        assertTrue(report.contains("Mittagessen"));
        assertTrue(report.contains("Abendessen"));
        assertTrue(report.contains("Haferflocken"));
    }

    @Test
    public void testFormatSingleLogShare() {
        CalculationLog log = createSampleLogs().getFirst();
        String singleShare = exportHelper.formatSingleLogShare(log, GlucoseUnit.MG_DL);

        assertNotNull(singleShare);
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_calculation_prefix) + ": Frühstück"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_carbs_prefix) + ": 40.0 g"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_factor_prefix) + " (Morgens): 1.5 IE/KE"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_meal_bolus_prefix) + ": 6.0 IE"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_measured_bg_label) + ": 130.0 mg/dl"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_correction_prefix) + ": +0.75 IE"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_total_dose_prefix) + ": 7.0 IE"));
        assertTrue(singleShare.contains(context.getString(R.string.logbook_export_helper_note_prefix) + ": Haferflocken"));
    }
}
