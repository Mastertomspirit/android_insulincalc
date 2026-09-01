package network.spiritscorp.data;

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
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import kotlin.Pair;
import network.spiritscorp.data.ImportResult;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit and integration tests verifying {@link DatabaseBackupManager} as an instantiable,
 * dependency-injected object. Tests JSON & CSV serialization, deserialization, DAO operations,
 * and robust handling of corrupted or edge-case input.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DatabaseBackupManagerTest {

    private static final double DELTA = 0.001;

    private AppDatabase inMemoryDb;
    private UserSettingsDao userSettingsDao;
    private CalculationLogDao calculationLogDao;
    private DatabaseBackupManager backupManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        userSettingsDao = inMemoryDb.userSettingsDao();
        calculationLogDao = inMemoryDb.calculationLogDao();
        backupManager = new DatabaseBackupManager(userSettingsDao, calculationLogDao);
    }

    @After
    public void tearDown() {
        if (inMemoryDb != null && inMemoryDb.isOpen()) {
            inMemoryDb.close();
        }
    }

    private List<CalculationLog> createSampleLogs() {
        return Arrays.asList(
                new CalculationLog(
                        1L,
                        1700000000000L,
                        "Frühstück (Müsli & Apfel)",
                        45.0,
                        "g KH",
                        45.0,
                        3.75,
                        4.5,
                        "Morgens",
                        1.5,
                        6.75,
                        140.0,
                        100.0,
                        40.0,
                        1.0,
                        7.75,
                        8.0,
                        "Haferflocken mit Apfel"
                ),
                new CalculationLog(
                        2L,
                        1700015000000L,
                        "Mittagessen (Pasta, Tomatensauce)",
                        6.0,
                        "BE",
                        72.0,
                        6.0,
                        7.2,
                        "Mittags",
                        1.0,
                        6.0,
                        110.0,
                        100.0,
                        40.0,
                        0.25,
                        6.25,
                        6.5,
                        "Vollkornnudeln"
                ),
                new CalculationLog(
                        3L,
                        1700035000000L,
                        "Abendessen (Brot mit Käse)",
                        3.5,
                        "KE",
                        35.0,
                        2.92,
                        3.5,
                        "Abends",
                        1.2,
                        4.2,
                        95.0,
                        100.0,
                        40.0,
                        0.0,
                        4.2,
                        4.0,
                        "Roggenbrot"
                ),
                new CalculationLog(
                        4L,
                        1700050000000L,
                        "Spät-Snack",
                        15.0,
                        "g KH",
                        15.0,
                        1.25,
                        1.5,
                        "Nachts",
                        0.8,
                        1.2,
                        null,
                        null,
                        null,
                        null,
                        1.2,
                        1.0,
                        "Joghurt"
                )
        );
    }

    @Test
    public void testExportAllLogsToJsonAndImportBack() {
        List<CalculationLog> sampleLogs = createSampleLogs();
        UserSettings sampleSettings = new UserSettings(
                1,
                1.80,
                1.10,
                1.35,
                0.90,
                "BE",
                12,
                "mg/dl",
                110,
                45,
                0.5,
                true,
                "LAVENDER_PURPLE",
                "SYSTEM"
        );

        // 1. Export to JSON via instance method
        String jsonOutput = backupManager.exportToJson(sampleSettings, sampleLogs);
        assertNotNull(jsonOutput);
        assertTrue(jsonOutput.contains("\"settings\""));
        assertTrue(jsonOutput.contains("\"logs\""));
        assertTrue(jsonOutput.contains("Frühstück (Müsli & Apfel)"));
        assertTrue(jsonOutput.contains("Spät-Snack"));

        // 2. Parse back via instance method
        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(jsonOutput);
        assertNotNull("Parsed result should not be null", parsed);

        UserSettings parsedSettings = parsed.getFirst();
        List<CalculationLog> parsedLogs = parsed.getSecond();

        // Verify Settings
        assertNotNull(parsedSettings);
        assertEquals(1.80, parsedSettings.getMorningFactor(), DELTA);
        assertEquals(1.10, parsedSettings.getNoonFactor(), DELTA);
        assertEquals("BE", parsedSettings.getDefaultCarbUnit());
        assertEquals("LAVENDER_PURPLE", parsedSettings.getSelectedTheme());

        // Verify ALL 4 logs were exported and restored
        assertEquals(sampleLogs.size(), parsedLogs.size());

        for (int i = 0; i < sampleLogs.size(); i++) {
            CalculationLog original = sampleLogs.get(i);
            CalculationLog restored = parsedLogs.get(i);
            assertEquals(original.getMealTitle(), restored.getMealTitle());
            assertEquals(original.getCarbGrams(), restored.getCarbGrams(), DELTA);
            assertEquals(original.getTotalInsulin(), restored.getTotalInsulin(), DELTA);
            assertEquals(original.getRoundedInsulin(), restored.getRoundedInsulin(), DELTA);
            assertEquals(original.getNotes(), restored.getNotes());
            assertEquals(original.getBloodGlucose(), restored.getBloodGlucose());
        }
    }

    @Test
    public void testDirectDaoIntegrationExportAndImport() {
        // Populate in-memory database
        UserSettings settings = new UserSettings(1, 2.0, 1.0, 1.5, 0.8, "g KH", 12, "mg/dl", 100, 40, 0.5, true, "MEDICAL_TEAL", "SYSTEM");
        userSettingsDao.saveSettings(settings);
        calculationLogDao.insertLogs(createSampleLogs());

        // Export directly from DAOs
        String exportedJson = backupManager.exportToJson();
        assertNotNull(exportedJson);
        assertTrue(exportedJson.contains("Frühstück (Müsli & Apfel)"));

        // Clear DB
        calculationLogDao.clearAllLogs();
        assertEquals(0, calculationLogDao.getAllLogsDirect().size());

        // Import back through DAO
        ImportResult result = backupManager.importFromJson(exportedJson);
        assertTrue(result.isSuccess());
        assertEquals(4, result.getImportedLogsCount());
        assertTrue(result.isImportedSettings());

        // Verify DAO holds restored items
        List<CalculationLog> restoredLogs = calculationLogDao.getAllLogsDirect();
        assertEquals(4, restoredLogs.size());
    }

    @Test
    public void testExportAllLogsToCsv() {
        List<CalculationLog> sampleLogs = createSampleLogs();
        String csvOutput = backupManager.exportToCsv(sampleLogs);

        assertNotNull(csvOutput);
        String[] lines = csvOutput.trim().split("\n");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                validLines.add(line);
            }
        }

        // Header line + 4 logs = 5 lines total
        assertEquals(5, validLines.size());
        assertTrue(validLines.get(0).startsWith("ID,Timestamp,Date,MealTitle"));
        assertTrue(validLines.get(1).contains("Frühstück (Müsli & Apfel)"));
        assertTrue(validLines.get(2).contains("Mittagessen (Pasta, Tomatensauce)"));
        assertTrue(validLines.get(3).contains("Abendessen (Brot mit Käse)"));
        assertTrue(validLines.get(4).contains("Spät-Snack"));

        // Parse CSV back
        List<CalculationLog> parsedLogs = backupManager.parseCsv(csvOutput);
        assertEquals(sampleLogs.size(), parsedLogs.size());

        assertEquals("Frühstück (Müsli & Apfel)", parsedLogs.get(0).getMealTitle());
        assertEquals(45.0, parsedLogs.get(0).getCarbGrams(), DELTA);
        assertEquals("Mittagessen (Pasta, Tomatensauce)", parsedLogs.get(1).getMealTitle());
        assertEquals(72.0, parsedLogs.get(1).getCarbGrams(), DELTA);
        assertEquals("Spät-Snack", parsedLogs.get(3).getMealTitle());
    }

    @Test
    public void testImportFromCsvDirectToDao() {
        List<CalculationLog> sampleLogs = createSampleLogs();
        String csv = backupManager.exportToCsv(sampleLogs);

        ImportResult result = backupManager.importFromCsv(csv);
        assertTrue(result.isSuccess());
        assertEquals(4, result.getImportedLogsCount());
        assertFalse(result.isImportedSettings());

        List<CalculationLog> fromDb = calculationLogDao.getAllLogsDirect();
        assertEquals(4, fromDb.size());
    }

    @Test
    public void testCsvEscapingWithQuotesAndCommas() {
        List<CalculationLog> logsWithCommas = List.of(
                new CalculationLog(
                        10L,
                        0L,
                        "Pizza \"Speciale\", extra Käse",
                        80.0,
                        "g KH",
                        80.0,
                        6.67,
                        8.0,
                        "Abends",
                        1.2,
                        9.6,
                        null,
                        null,
                        null,
                        null,
                        9.6,
                        9.5,
                        "Mit Salami, Pilzen, und \"Knoblauch-Öl\""
                )
        );

        String csv = backupManager.exportToCsv(logsWithCommas);
        List<CalculationLog> parsed = backupManager.parseCsv(csv);

        assertEquals(1, parsed.size());
        assertEquals("Pizza \"Speciale\", extra Käse", parsed.getFirst().getMealTitle());
        assertEquals("Mit Salami, Pilzen, und \"Knoblauch-Öl\"", parsed.getFirst().getNotes());
    }

    @Test
    public void testCorruptedJsonImportDoesNotCrash() {
        // Empty string
        assertNull(backupManager.parseJson(""));

        // Whitespace only
        assertNull(backupManager.parseJson("   \n\t  "));

        // Truncated/Invalid JSON
        assertNull(backupManager.parseJson("{\"settings\": { \"morningFactor\": "));
        assertNull(backupManager.parseJson("{not_valid_json}"));

        // Random binary/garbage data
        assertNull(backupManager.parseJson("0xDEADBEEF-Corrupted-Binary-Stream-%%%"));

        // Valid JSON with empty settings/logs should return non-null with empty list
        String emptyJson = "{\"version\": 1, \"settings\": {}, \"logs\": []}";
        Pair<UserSettings, List<CalculationLog>> parsedEmpty = backupManager.parseJson(emptyJson);
        assertNotNull(parsedEmpty);
        assertEquals(0, parsedEmpty.getSecond().size());
    }

    @Test
    public void testCorruptedCsvImportDoesNotCrash() {
        // Empty string
        List<CalculationLog> emptyResult = backupManager.parseCsv("");
        assertTrue(emptyResult.isEmpty());

        // Random string without commas
        List<CalculationLog> garbageResult = backupManager.parseCsv("Some random invalid non-csv text");
        assertTrue(garbageResult.isEmpty());

        // Partially broken rows mixed with valid rows
        String mixedCsv = """
                ID,Timestamp,Date,MealTitle,RawCarbInput,CarbUnit,CarbGrams,BE,KE,TimeOfDay,InsulinFactor,MealInsulin,BloodGlucose,TargetGlucose,CorrectionFactor,CorrectionInsulin,TotalInsulin,RoundedInsulin,Notes
                1,1700000000000,"2023-11-14 20:00:00","Salat",10.0,"g KH",10.0,0.83,1.0,"Abends",1.0,1.0,,,0.0,1.0,1.0,"Leicht"
                BrokenRowWithoutEnoughColumns
                2,1700000001000,"2023-11-14 21:00:00","Suppe",20.0,"g KH",20.0,1.67,2.0,"Abends",1.0,2.0,,,0.0,2.0,2.0,"Warm\"""";

        List<CalculationLog> parsedMixed = backupManager.parseCsv(mixedCsv);
        // Should safely parse the 2 valid rows while gracefully skipping the broken row
        assertEquals(2, parsedMixed.size());
        assertEquals("Salat", parsedMixed.get(0).getMealTitle());
        assertEquals("Suppe", parsedMixed.get(1).getMealTitle());
    }

    @Test
    public void testJsonImportWithDirectArrayOfLogs() {
        String arrayJson = """
                [
                  {
                    "id": 5,
                    "mealTitle": "Frühstücks-Smoothie",
                    "carbGrams": 30.0,
                    "rawCarbInput": 30.0,
                    "carbUnit": "g KH",
                    "timeOfDay": "Morgens",
                    "insulinFactor": 1.5,
                    "mealInsulin": 4.5,
                    "totalInsulin": 4.5,
                    "roundedInsulin": 4.5
                  }
                ]""";

        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(arrayJson);
        assertNotNull(parsed);
        UserSettings settings = parsed.getFirst();
        List<CalculationLog> logs = parsed.getSecond();
        assertNull(settings); // Settings was not provided
        assertEquals(1, logs.size());
        assertEquals("Frühstücks-Smoothie", logs.getFirst().getMealTitle());
        assertEquals(30.0, logs.getFirst().getCarbGrams(), DELTA);
    }

    @Test
    public void testSplitCsvLineHelper() {
        String line = "1,1700000000000,\"2023-11-14 20:00:00\",\"Pizza, Pasta & \"\"Vino\"\"\",50.0";
        List<String> tokens = backupManager.splitCsvLine(line);

        assertEquals(5, tokens.size());
        assertEquals("1", tokens.get(0));
        assertEquals("1700000000000", tokens.get(1));
        assertEquals("2023-11-14 20:00:00", tokens.get(2));
        assertEquals("Pizza, Pasta & \"Vino\"", tokens.get(3));
        assertEquals("50.0", tokens.get(4));
    }

    @Test
    public void testJsonBackupWithUmlautsAndNullValuesRoundtrip() {
        UserSettings originalSettings = new UserSettings(
                1, 1.75, 1.25, 1.5, 0.9, "BE", 12, "mg/dl", 115.0, 45.0, 0.5, true, "WARM_EMBER", "DARK", "test-api-key", "gemini-2.5-flash"
        );
        userSettingsDao.saveSettings(originalSettings);

        CalculationLog logWithUmlautsAndNulls = new CalculationLog(
                10L,
                1700000000000L,
                "Äpfel, Überbackenes & Öl-Salat (Mahlzeit)",
                4.5,
                "BE",
                54.0,
                4.5,
                5.4,
                "Mittags",
                1.25,
                5.625,
                null,
                null,
                null,
                null,
                5.625,
                5.5,
                "Notizen mit Umlauten: äöüß & Sonderzeichen <>&\""
        );
        calculationLogDao.insertLog(logWithUmlautsAndNulls);

        String json = backupManager.exportToJson();
        assertNotNull(json);
        assertTrue(json.contains("Äpfel, Überbackenes & Öl-Salat"));
        assertTrue(json.contains("Notizen mit Umlauten: äöüß & Sonderzeichen"));
        assertTrue(json.contains("test-api-key"));

        // Import into clean database
        userSettingsDao.saveSettings(new UserSettings());
        calculationLogDao.clearAllLogs();

        ImportResult result = backupManager.importFromJson(json);
        assertTrue(result.isSuccess());

        UserSettings restoredSettings = userSettingsDao.getSettingsDirect();
        assertNotNull(restoredSettings);
        assertEquals("WARM_EMBER", restoredSettings.getSelectedTheme());
        assertEquals("test-api-key", restoredSettings.getGeminiApiKey());

        List<CalculationLog> restoredLogs = calculationLogDao.getAllLogsDirect();
        assertEquals(1, restoredLogs.size());
        assertEquals("Äpfel, Überbackenes & Öl-Salat (Mahlzeit)", restoredLogs.getFirst().getMealTitle());
        assertNull(restoredLogs.getFirst().getBloodGlucose());
        assertEquals("Notizen mit Umlauten: äöüß & Sonderzeichen <>&\"", restoredLogs.getFirst().getNotes());
    }
}
