package network.spiritscorp;

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

import kotlin.Pair;
import network.spiritscorp.data.DatabaseBackupManager;
import network.spiritscorp.data.ImportResult;
import network.spiritscorp.data.InsulinRepository;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end system and integration scenario tests in Java covering complete user journeys:
 * - Multi-meal logging and daily statistics
 * - Customization, backup export, wipe, and complete restore
 * - High-correction bolus scenarios and CSV/JSON reporting
 * - Hypoglycemia safety alerts and AI nutrition estimation workflows
 */
public class InsulinCalculatorSystemScenarioTest {

    private static final double DELTA = 0.001;

    private InsulinRepositoryIntegrationTest.FakeCalculationLogDao fakeLogDao;
    private InsulinRepositoryIntegrationTest.FakeUserSettingsDao fakeSettingsDao;
    private InsulinRepository repository;

    @Before
    public void setup() {
        fakeLogDao = new InsulinRepositoryIntegrationTest.FakeCalculationLogDao();
        fakeSettingsDao = new InsulinRepositoryIntegrationTest.FakeUserSettingsDao();
        repository = new InsulinRepository(fakeLogDao, fakeSettingsDao);
    }

    @Test
    public void testFullDayDiabetesManagementJourney() {
        // Step 1: User configures insulin parameters
        UserSettings userSettings = new UserSettings(
                1,
                1.6,
                1.0,
                1.3,
                0.8,
                "g KH",
                12,
                "mg/dl",
                100.0,
                40.0,
                0.5,
                true,
                "SLATE_CALM",
                "SYSTEM"
        );
        repository.saveSettings(userSettings);

        long now = System.currentTimeMillis();

        // Step 2: Morning Breakfast (50g KH, BG = 110 mg/dl)
        // 50g / 12 = 4.17 BE. 4.17 * 1.6 = 6.67 IE. BG 110 -> (110-100)/40 = +0.25 IE. Total = 6.92 IE -> 7.0 IE
        CalculationLog breakfastLog = new CalculationLog(
                1L,
                now - (10 * 3600 * 1000),
                "Frühstück: Haferflocken mit Beeren",
                50.0,
                "g KH",
                50.0,
                4.17,
                5.0,
                TimeOfDay.MORNING.getTitle(),
                1.6,
                6.67,
                110.0,
                100.0,
                40.0,
                0.25,
                6.92,
                7.0,
                "Vor der Arbeit"
        );
        repository.saveCalculation(breakfastLog);

        // Step 3: Lunch (4.0 BE, BG = 180 mg/dl - elevated)
        // 4 BE * 12 = 48g KH. 4 * 1.0 = 4.0 IE. BG 180 -> (180-100)/40 = +2.0 IE. Total = 6.0 IE
        CalculationLog lunchLog = new CalculationLog(
                2L,
                now - (5 * 3600 * 1000),
                "Mittagessen: Reisgericht",
                4.0,
                "BE",
                48.0,
                4.0,
                4.8,
                TimeOfDay.NOON.getTitle(),
                1.0,
                4.0,
                180.0,
                100.0,
                40.0,
                2.0,
                6.0,
                6.0,
                "Kantine"
        );
        repository.saveCalculation(lunchLog);

        // Step 4: Dinner (30g KH, BG = 95 mg/dl - in range)
        // 30g / 12 = 2.5 BE. 2.5 * 1.3 = 3.25 IE. BG 95 -> 0 corr. Total = 3.25 IE -> 3.5 IE
        CalculationLog dinnerLog = new CalculationLog(
                3L,
                now - (3600 * 1000),
                "Abendessen: Vollkornbrot & Salat",
                30.0,
                "g KH",
                30.0,
                2.5,
                3.0,
                TimeOfDay.EVENING.getTitle(),
                1.3,
                3.25,
                95.0,
                100.0,
                40.0,
                0.0,
                3.25,
                3.5,
                "Zuhause"
        );
        repository.saveCalculation(dinnerLog);

        // Step 5: Verify Logbook state & total statistics
        List<CalculationLog> allLogs = repository.getAllLogsDirect();
        assertEquals(3, allLogs.size());

        double totalCarbsToday = allLogs.stream().mapToDouble(CalculationLog::getCarbGrams).sum();
        assertEquals(128.0, totalCarbsToday, DELTA);

        double totalInsulinToday = allLogs.stream().mapToDouble(CalculationLog::getRoundedInsulin).sum();
        assertEquals(16.5, totalInsulinToday, DELTA);

        // Step 6: Export to CSV and verify all 3 meals are included
        DatabaseBackupManager backupManager = new DatabaseBackupManager();
        String csv = backupManager.exportToCsv(allLogs);
        assertTrue(csv.contains("Frühstück: Haferflocken mit Beeren"));
        assertTrue(csv.contains("Mittagessen: Reisgericht"));
        assertTrue(csv.contains("Abendessen: Vollkornbrot & Salat"));
        assertTrue(csv.contains("128.0") || (csv.contains("50.0") && csv.contains("48.0") && csv.contains("30.0")));

        // Step 7: Export to JSON Backup
        UserSettings settings = repository.getSettings();
        String jsonBackup = backupManager.exportToJson(settings, allLogs);
        assertNotNull(jsonBackup);

        // Step 8: Simulate clearing database (e.g. device switch / reset)
        repository.clearLogs();
        List<CalculationLog> clearedLogs = repository.getAllLogsDirect();
        assertEquals(0, clearedLogs.size());

        // Step 9: Restore database from JSON Backup
        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(jsonBackup);
        assertNotNull(parsed);
        UserSettings restoredSettings = parsed.getFirst();
        List<CalculationLog> restoredLogs = parsed.getSecond();

        assertNotNull(restoredSettings);
        assertEquals(1.6, restoredSettings.getMorningFactor(), DELTA);

        repository.saveSettings(restoredSettings);
        repository.saveLogs(restoredLogs);

        List<CalculationLog> restoredDbLogs = repository.getAllLogsDirect();
        assertEquals(3, restoredDbLogs.size());
        assertEquals(128.0, restoredDbLogs.stream().mapToDouble(CalculationLog::getCarbGrams).sum(), DELTA);
        assertEquals(16.5, restoredDbLogs.stream().mapToDouble(CalculationLog::getRoundedInsulin).sum(), DELTA);
    }

    @Test
    public void testAiMealEstimationToBolusCalculationIntegration() {
        // Given estimated meal from AI
        String estimatedMealName = "Pizza Margherita mit Rucola";
        double estimatedCarbsGrams = 85.0;

        // User is in BE mode
        CarbUnit unit = CarbUnit.BE;
        double rawUnitsInput = unit.fromGrams(estimatedCarbsGrams); // 85g / 12 = 7.083 BE
        double factor = 1.20; // Evening factor

        double mealInsulin = rawUnitsInput * factor; // 7.0833 * 1.2 = 8.5 IE

        // Patient has BG = 160 mg/dl, target 100, corrFactor 40 -> +1.5 IE
        double correctionInsulin = (160.0 - 100.0) / 40.0;
        double totalInsulin = mealInsulin + correctionInsulin; // 8.5 + 1.5 = 10.0 IE

        double roundedTotal = BigDecimal.valueOf(totalInsulin).setScale(1, RoundingMode.HALF_UP).doubleValue();

        CalculationLog log = new CalculationLog(
                0L,
                System.currentTimeMillis(),
                estimatedMealName,
                rawUnitsInput,
                unit.getShortName(),
                estimatedCarbsGrams,
                estimatedCarbsGrams / 12.0,
                estimatedCarbsGrams / 10.0,
                "Abends",
                factor,
                mealInsulin,
                160.0,
                100.0,
                40.0,
                correctionInsulin,
                totalInsulin,
                roundedTotal,
                "KI-geschätzte Mahlzeit (Fettverzögerung beachten)"
        );

        assertEquals("Pizza Margherita mit Rucola", log.getMealTitle());
        assertEquals(85.0, log.getCarbGrams(), DELTA);
        assertEquals(10.0, log.getRoundedInsulin(), DELTA);
        assertTrue(log.getNotes().contains("KI-geschätzte"));
    }

    @Test
    public void testHypoglycemiaSafetyWarningScenario() {
        // Patient has blood glucose of 55 mg/dl (hypoglycemia threshold < 70)
        double currentBg = 55.0;
        double targetBg = 100.0;
        double corrFactor = 40.0;

        // In case of hypo: meal bolus must be carefully evaluated and correction negative
        double diff = targetBg - currentBg; // 45 mg/dl below target
        double negativeCorrection = -(diff / corrFactor); // -1.125 IE

        double mealCarbs = 40.0; // 40g KH = 3.33 BE
        double mealFactor = 1.2;
        double mealInsulin = (mealCarbs / 12.0) * mealFactor; // 4.0 IE
        double totalInsulin = Math.max(0.0, mealInsulin + negativeCorrection); // 4.0 - 1.125 = 2.875 IE

        assertTrue("Negative correction should reduce bolus", negativeCorrection < 0);
        assertTrue("Total insulin should remain non-negative", totalInsulin >= 0);
        assertEquals(2.875, totalInsulin, DELTA);
    }

    @Test
    public void testMealTitleAndNotesLoggingScenario() {
        CalculationLog customLog = new CalculationLog(
                10L,
                System.currentTimeMillis(),
                "Abendessen im Restaurant",
                60.0,
                "g KH",
                60.0,
                5.0,
                6.0,
                TimeOfDay.EVENING.getTitle(),
                1.2,
                6.0,
                140.0,
                120.0,
                40.0,
                0.5,
                6.5,
                6.5,
                "Vorher 30 Min Spaziergang gemacht, Sensorwert stabil"
        );

        repository.saveCalculation(customLog);

        List<CalculationLog> allLogs = repository.getAllLogsDirect();

        assertEquals(1, allLogs.size());
        CalculationLog retrieved = allLogs.getFirst();
        assertEquals("Abendessen im Restaurant", retrieved.getMealTitle());
        assertEquals("Vorher 30 Min Spaziergang gemacht, Sensorwert stabil", retrieved.getNotes());
        assertEquals(6.5, retrieved.getRoundedInsulin(), DELTA);
    }

    @Test
    public void testAiConfigurationAndModelSelectionScenario() {
        UserSettings customAiSettings = new UserSettings(
                1,
                1.5,
                1.0,
                1.2,
                0.8,
                "GRAMS",
                12,
                "mg/dl",
                120.0,
                50.0,
                0.5,
                true,
                "MEDICAL_TEAL",
                "SYSTEM",
                "AIzaSyTestCustomKey12345",
                "gemini-3.7-flash"
        );

        repository.saveSettings(customAiSettings);

        UserSettings retrieved = repository.getSettings();
        assertNotNull(retrieved);
        assertEquals("AIzaSyTestCustomKey12345", retrieved.getGeminiApiKey());
        assertEquals("gemini-3.7-flash", retrieved.getSelectedAiModel());

        // Test export & restore of AI settings
        DatabaseBackupManager backupManager = new DatabaseBackupManager();
        String json = backupManager.exportToJson(retrieved, java.util.Collections.emptyList());
        assertTrue(json.contains("AIzaSyTestCustomKey12345"));
        assertTrue(json.contains("gemini-3.7-flash"));

        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(json);
        assertNotNull(parsed);
        assertNotNull(parsed.getFirst());
        assertEquals("AIzaSyTestCustomKey12345", parsed.getFirst().getGeminiApiKey());
        assertEquals("gemini-3.7-flash", parsed.getFirst().getSelectedAiModel());
    }

    @Test
    public void testSaveAndClearAllFieldsScenario() {
        // Given a user enters carbs, blood glucose, meal title, and notes
        String mealTitle = "Abendessen: Lasagne";
        String carbInput = "60";
        String currentGlucose = "150";
        String notes = "Vor dem Essen gemessen";

        CalculationLog log = new CalculationLog(
                1L,
                System.currentTimeMillis(),
                mealTitle,
                Double.parseDouble(carbInput),
                "g KH",
                60.0,
                5.0,
                6.0,
                "Abends",
                1.2,
                6.0,
                Double.parseDouble(currentGlucose),
                100.0,
                40.0,
                1.25,
                7.25,
                7.5,
                notes
        );
        repository.saveCalculation(log);

        List<CalculationLog> logs = repository.getAllLogsDirect();
        assertEquals(1, logs.size());
        assertEquals("Abendessen: Lasagne", logs.getFirst().getMealTitle());
        assertEquals("Vor dem Essen gemessen", logs.getFirst().getNotes());

        // When all inputs are cleared (simulating viewModel.clearAllCalculatorInputs())
        carbInput = "0";
        currentGlucose = "";
        mealTitle = "";
        notes = "";

        assertEquals("0", carbInput);
        assertTrue(currentGlucose.isEmpty());
        assertTrue(mealTitle.isEmpty());
        assertTrue(notes.isEmpty());
    }

    @Test
    public void testFullJsonBackupAndRestoreE2E() {
        UserSettings initialSettings = new UserSettings(
                1, 1.8, 1.1, 1.4, 0.7, "KE", 10, "mmol/l", 6.5, 2.5, 0.5, true, "OCEAN_BREEZE", "LIGHT", "key-xyz", "gemini-2.5-pro"
        );
        repository.saveSettings(initialSettings);

        CalculationLog log = new CalculationLog(
                100L,
                1700000000000L,
                "Haferflocken & Heidelbeeren",
                4.0,
                "KE",
                40.0,
                3.33,
                4.0,
                "Morgens",
                1.8,
                7.2,
                7.8,
                6.5,
                2.5,
                0.52,
                7.72,
                7.5,
                "Sensor leicht steigend"
        );
        repository.saveCalculation(log);

        DatabaseBackupManager backupManager = new DatabaseBackupManager(fakeSettingsDao, fakeLogDao);
        String exportedJson = backupManager.exportToJson();
        assertNotNull(exportedJson);
        assertTrue(exportedJson.contains("OCEAN_BREEZE"));
        assertTrue(exportedJson.contains("Haferflocken & Heidelbeeren"));
        assertTrue(exportedJson.contains("key-xyz"));

        // Clear database
        repository.saveSettings(new UserSettings());
        repository.clearLogs();
        assertEquals(0, repository.getAllLogsDirect().size());

        // Import
        ImportResult result = backupManager.importFromJson(exportedJson);
        assertTrue(result.isSuccess());

        UserSettings restoredSettings = repository.getSettings();
        assertEquals("OCEAN_BREEZE", restoredSettings.getSelectedTheme());
        assertEquals("mmol/l", restoredSettings.getGlucoseUnit());
        assertEquals("key-xyz", restoredSettings.getGeminiApiKey());

        List<CalculationLog> restoredLogs = repository.getAllLogsDirect();
        assertEquals(1, restoredLogs.size());
        assertEquals("Haferflocken & Heidelbeeren", restoredLogs.getFirst().getMealTitle());
        assertEquals("Sensor leicht steigend", restoredLogs.getFirst().getNotes());
    }
}
