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

import network.spiritscorp.model.CalculationLog;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure Java unit tests for {@link LogbookExportHelper}.
 */
public class LogbookExportHelperTest {

    private static final double DELTA = 0.001;

    private List<CalculationLog> createSampleLogs() {
        return Arrays.asList(
                new CalculationLog(
                        1L,
                        1700000000000L,
                        "Frühstück",
                        40.0,
                        "g KH",
                        40.0,
                        3.33,
                        4.0,
                        "Morgens",
                        1.5,
                        6.0,
                        130.0,
                        100.0,
                        40.0,
                        0.75,
                        6.75,
                        7.0,
                        "Haferflocken",
                        null
                ),
                new CalculationLog(
                        2L,
                        1700020000000L,
                        "Mittagessen",
                        50.0,
                        "g KH",
                        50.0,
                        4.17,
                        5.0,
                        "Mittags",
                        1.0,
                        5.0,
                        110.0,
                        100.0,
                        40.0,
                        0.25,
                        5.25,
                        5.5,
                        "Salat mit Brot",
                        null
                ),
                new CalculationLog(
                        3L,
                        1700040000000L,
                        "Abendessen",
                        30.0,
                        "g KH",
                        30.0,
                        2.5,
                        3.0,
                        "Abends",
                        1.2,
                        3.6,
                        null,
                        null,
                        null,
                        null,
                        3.6,
                        3.5,
                        "Suppe",
                        null
                )
        );
    }

    @Test
    public void testGenerateExportTextWithEmptyList() {
        String textNull = LogbookExportHelper.generateExportText(null, "Heute");
        assertTrue(textNull.contains("Keine Einträge"));

        String textEmpty = LogbookExportHelper.generateExportText(Collections.emptyList(), "Letzte 7 Tage");
        assertTrue(textEmpty.contains("Keine Einträge"));
        assertTrue(textEmpty.contains("Letzte 7 Tage"));
    }

    @Test
    public void testGenerateExportTextWithLogs() {
        List<CalculationLog> logs = createSampleLogs();
        String report = LogbookExportHelper.generateExportText(logs, "Alle Einträge");

        assertNotNull(report);
        assertTrue(report.contains("Insulin-Rechner Tagebuch-Export"));
        assertTrue(report.contains("Einträge: 3"));
        assertTrue(report.contains("Gesamt-KH: 120 g"));
        assertTrue(report.contains("Frühstück"));
        assertTrue(report.contains("Mittagessen"));
        assertTrue(report.contains("Abendessen"));
        assertTrue(report.contains("Haferflocken"));
    }

    @Test
    public void testGenerateCsvExport() {
        List<CalculationLog> logs = createSampleLogs();
        String csv = LogbookExportHelper.generateCsvExport(logs);

        assertNotNull(csv);
        String[] lines = csv.trim().split("\n");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                validLines.add(line);
            }
        }

        assertEquals(4, validLines.size()); // Header + 3 entries
        assertTrue(validLines.get(0).startsWith("ID,Datum_Uhrzeit,Mahlzeit"));
        assertTrue(validLines.get(1).contains("Frühstück"));
        assertTrue(validLines.get(2).contains("Mittagessen"));
        assertTrue(validLines.get(3).contains("Abendessen"));
    }

    @Test
    public void testCsvEscapingQuotesAndSpecialChars() {
        List<CalculationLog> logs = Collections.singletonList(
                new CalculationLog(
                        10L,
                        0L,
                        "Pizza \"Salami, Pilze\"",
                        60.0,
                        "g KH",
                        60.0,
                        5.0,
                        6.0,
                        "Abends",
                        1.0,
                        6.0,
                        null,
                        null,
                        null,
                        null,
                        6.0,
                        6.0,
                        "Notiz: \"Lecker, aber fettig\"",
                        null
                )
        );

        String csv = LogbookExportHelper.generateCsvExport(logs);
        assertTrue(csv.contains("\"Pizza \"\"Salami, Pilze\"\"\""));
        assertTrue(csv.contains("\"Notiz: \"\"Lecker, aber fettig\"\"\""));
    }

    @Test
    public void testCalculateSummaryMetrics() {
        List<CalculationLog> logs = createSampleLogs();
        LogbookExportHelper.LogbookMetrics metrics = LogbookExportHelper.calculateMetrics(logs);

        assertEquals(3, metrics.getTotalEntries());
        assertEquals(120.0, metrics.getTotalCarbsGrams(), DELTA);
        assertEquals(16.0, metrics.getTotalInsulinUnits(), DELTA);
        // Average BG of (130 + 110) / 2 = 120.0 (3rd log has null BG)
        assertNotNull(metrics.getAverageBloodGlucose());
        assertEquals(120.0, metrics.getAverageBloodGlucose(), DELTA);
    }
}
