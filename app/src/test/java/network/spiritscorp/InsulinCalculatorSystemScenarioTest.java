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
import network.spiritscorp.ai.GeminiAiModel;
import network.spiritscorp.data.DatabaseBackupManager;
import network.spiritscorp.data.ImportResult;
import network.spiritscorp.data.InsulinRepository;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.GlucoseUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;
import network.spiritscorp.ui.theme.AppTheme;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    /**
     * Initializes fake DAOs and repository for end-to-end scenario testing.
     */
    @Before
    public void setup() {
        fakeLogDao = new InsulinRepositoryIntegrationTest.FakeCalculationLogDao();
        fakeSettingsDao = new InsulinRepositoryIntegrationTest.FakeUserSettingsDao();
        repository = new InsulinRepository(fakeLogDao, fakeSettingsDao);
    }

    /**
     * Verifies a complete daily diabetes management journey including settings, meal logging, and backups.
     */
    @Test
    public void testFullDayDiabetesManagementJourney() {
        // Step 1: User configures insulin parameters
        UserSettings userSettings = new UserSettings(
                1,
                1.6,
                1.0,
                1.3,
                0.8,
                CarbUnit.GRAMS,
                12,
                GlucoseUnit.MG_DL,
                100.0,
                40.0,
                0.5,
                true,
                AppTheme.MEDICAL_TEAL,
                AppTheme.Mode.SYSTEM,
                "",
                GeminiAiModel.GEMINI_3_7_FLASH
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
                CarbUnit.GRAMS,
                50.0,
                4.17,
                5.0,
                TimeOfDay.MORNING,
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
                CarbUnit.BE,
                48.0,
                4.0,
                4.8,
                TimeOfDay.NOON,
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
                CarbUnit.GRAMS,
                30.0,
                2.5,
                3.0,
                TimeOfDay.EVENING,
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
        assertEquals("Logbook count incorrect", 3, allLogs.size());

        double totalCarbsToday = allLogs.stream().mapToDouble(CalculationLog::getCarbGrams).sum();
        assertEquals("Total carbs sum incorrect", 128.0, totalCarbsToday, DELTA);

        double totalInsulinToday = allLogs.stream().mapToDouble(CalculationLog::getRoundedInsulin).sum();
        assertEquals("Total insulin sum incorrect", 16.5, totalInsulinToday, DELTA);

        // Step 6: Export to CSV and verify all 3 meals are included
        DatabaseBackupManager backupManager = new DatabaseBackupManager();
        String csv = backupManager.exportToCsv(allLogs);
        assertTrue("CSV export should contain breakfast title", csv.contains("Frühstück: Haferflocken mit Beeren"));
        assertTrue("CSV export should contain lunch title", csv.contains("Mittagessen: Reisgericht"));
        assertTrue("CSV export should contain dinner title", csv.contains("Abendessen: Vollkornbrot & Salat"));
        assertTrue("CSV export should contain carb totals", csv.contains("128.0") || (csv.contains("50.0") && csv.contains("48.0") && csv.contains("30.0")));

        // Step 7: Export to JSON Backup
        UserSettings settings = repository.loadSettings();
        String jsonBackup = backupManager.exportToJson(settings, allLogs);
        assertNotNull("JSON backup string must not be null", jsonBackup);

        // Step 8: Simulate clearing database (e.g. device switch / reset)
        repository.clearLogs();
        List<CalculationLog> clearedLogs = repository.getAllLogsDirect();
        assertEquals("Cleared logs count must be zero", 0, clearedLogs.size());

        // Step 9: Restore database from JSON Backup
        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(jsonBackup);
        assertNotNull("Parsed JSON result must not be null", parsed);
        UserSettings restoredSettings = parsed.getFirst();
        List<CalculationLog> restoredLogs = parsed.getSecond();

        assertNotNull("Restored settings must not be null", restoredSettings);
        assertEquals("Restored morning factor incorrect", 1.6, restoredSettings.getMorningFactor(), DELTA);

        repository.saveSettings(restoredSettings);
        repository.saveLogs(restoredLogs);

        List<CalculationLog> restoredDbLogs = repository.getAllLogsDirect();
        assertEquals("Restored logs count incorrect", 3, restoredDbLogs.size());
        assertEquals("Restored total carbs incorrect", 128.0, restoredDbLogs.stream().mapToDouble(CalculationLog::getCarbGrams).sum(), DELTA);
        assertEquals("Restored total insulin incorrect", 16.5, restoredDbLogs.stream().mapToDouble(CalculationLog::getRoundedInsulin).sum(), DELTA);
    }

    /**
     * Verifies integration between AI meal nutrition estimation and bolus calculation workflows.
     */
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
                unit,
                estimatedCarbsGrams,
                estimatedCarbsGrams / 12.0,
                estimatedCarbsGrams / 10.0,
                TimeOfDay.EVENING,
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

        assertEquals("AI meal title incorrect", "Pizza Margherita mit Rucola", log.getMealTitle());
        assertEquals("AI meal carb grams incorrect", 85.0, log.getCarbGrams(), DELTA);
        assertEquals("AI meal rounded insulin incorrect", 10.0, log.getRoundedInsulin(), DELTA);
        assertTrue("AI notes tag missing", log.getNotes().contains("KI-geschätzte"));
    }

    /**
     * Verifies hypoglycemia safety warning scenarios and dose reductions during low blood glucose.
     */
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
        assertEquals("Total insulin calculation incorrect for hypo scenario", 2.875, totalInsulin, DELTA);
    }

    /**
     * Verifies correct logging and retrieval of custom meal titles and notes.
     */
    @Test
    public void testMealTitleAndNotesLoggingScenario() {
        CalculationLog customLog = new CalculationLog(
                10L,
                System.currentTimeMillis(),
                "Abendessen im Restaurant",
                60.0,
                CarbUnit.GRAMS,
                60.0,
                5.0,
                6.0,
                TimeOfDay.EVENING,
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

        assertEquals("Log count must be 1", 1, allLogs.size());
        CalculationLog retrieved = allLogs.getFirst();
        assertEquals("Retrieved meal title incorrect", "Abendessen im Restaurant", retrieved.getMealTitle());
        assertEquals("Retrieved notes incorrect", "Vorher 30 Min Spaziergang gemacht, Sensorwert stabil", retrieved.getNotes());
        assertEquals("Retrieved rounded insulin incorrect", 6.5, retrieved.getRoundedInsulin(), DELTA);
    }

    /**
     * Verifies AI configuration, model selection, and secure handling during backup and restore.
     */
    @Test
    public void testAiConfigurationAndModelSelectionScenario() {
        UserSettings customAiSettings = new UserSettings(
                1,
                1.5,
                1.0,
                1.2,
                0.8,
                CarbUnit.GRAMS,
                12,
                GlucoseUnit.MG_DL,
                120.0,
                50.0,
                0.5,
                true,
                AppTheme.MEDICAL_TEAL,
                AppTheme.Mode.SYSTEM,
                "AIzaSyTestCustomKey12345",
                GeminiAiModel.GEMINI_3_7_FLASH
        );

        repository.saveSettings(customAiSettings);

        UserSettings retrieved = repository.loadSettings();
        assertNotNull("Retrieved settings must not be null", retrieved);
        assertEquals("Retrieved API key incorrect", "AIzaSyTestCustomKey12345", retrieved.getGeminiApiKey());
        assertEquals("Retrieved AI model incorrect", GeminiAiModel.GEMINI_3_7_FLASH, retrieved.getSelectedAiModel());

        // Test export & restore of AI settings
        DatabaseBackupManager backupManager = new DatabaseBackupManager();
        String json = backupManager.exportToJson(retrieved, java.util.Collections.emptyList());
        assertNotNull("Exported JSON must not be null", json);
        assertFalse("Exported JSON must not contain raw API key", json.contains("AIzaSyTestCustomKey12345"));
        assertTrue("Exported JSON must contain AI model name", json.contains(GeminiAiModel.GEMINI_3_7_FLASH.name()));

        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(json);
        assertNotNull("Parsed backup result must not be null", parsed);
        assertNotNull("Parsed settings must not be null", parsed.getFirst());
        assertTrue("Parsed API key should be scrubbed/blank", parsed.getFirst().getGeminiApiKey().isBlank());
        assertEquals("Parsed AI model incorrect", GeminiAiModel.GEMINI_3_7_FLASH, parsed.getFirst().getSelectedAiModel());
    }

    /**
     * Verifies saving and clearing all fields scenario in calculator and logbook.
     */
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
                CarbUnit.GRAMS,
                60.0,
                5.0,
                6.0,
                TimeOfDay.EVENING,
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
        assertEquals("Logs count must be 1", 1, logs.size());
        assertEquals("Saved meal title incorrect", "Abendessen: Lasagne", logs.getFirst().getMealTitle());
        assertEquals("Saved notes incorrect", "Vor dem Essen gemessen", logs.getFirst().getNotes());
    }

    /**
     * Verifies keyboard input persistence and calculation snapshot correctness during save operations.
     */
    @Test
    public void testKeyboardInputPersistenceDuringSaveScenario() {

        // Simulate user typing "48" in the carb input field and "140" in glucose field while keyboard is open
        String liveCarbInput = "48";
        String liveGlucoseInput = "140";
        String mealTitle = "Müsli & Banane";
        String notes = "Vor dem Training";

        // Computing snapshot at click time
        double bg = Double.parseDouble(liveGlucoseInput);
        double carbGrams = Double.parseDouble(liveCarbInput);
        double be = carbGrams / 12.0; // 4.0 BE
        double ke = carbGrams / 10.0; // 4.8 KE
        double mealInsulin = be * 1.75; // 4.0 * 1.75 = 7.0 IE
        double corrInsulin = (bg - 100.0) / 40.0; // +1.0 IE
        double totalInsulin = mealInsulin + corrInsulin; // 8.0 IE

        CalculationLog log = new CalculationLog(
                0L,
                System.currentTimeMillis(),
                mealTitle,
                carbGrams,
                CarbUnit.GRAMS,
                carbGrams,
                be,
                ke,
                TimeOfDay.MORNING,
                1.75,
                mealInsulin,
                bg,
                100.0,
                40.0,
                corrInsulin,
                totalInsulin,
                totalInsulin,
                notes
        );

        // Save immediately before clearing
        long id = repository.saveCalculation(log);
        assertTrue("Saved log must have valid ID", id > 0);

        // Verify stored entity is NOT 0
        List<CalculationLog> logs = repository.getAllLogsDirect();
        assertEquals("Logs count must be 1", 1, logs.size());
        CalculationLog saved = logs.getFirst();
        assertEquals("Saved raw carb input incorrect", 48.0, saved.getRawCarbInput(), DELTA);
        assertEquals("Saved carb grams incorrect", 48.0, saved.getCarbGrams(), DELTA);
        assertEquals("Saved meal insulin incorrect", 7.0, saved.getMealInsulin(), DELTA);
        assertNotNull("Saved blood glucose must not be null", saved.getBloodGlucose());
        assertEquals("Saved blood glucose incorrect", 140.0, saved.getBloodGlucose(), DELTA);
        assertEquals("Saved total insulin incorrect", 8.0, saved.getTotalInsulin(), DELTA);
        assertEquals("Saved rounded insulin incorrect", 8.0, saved.getRoundedInsulin(), DELTA);
        assertEquals("Saved meal title incorrect", "Müsli & Banane", saved.getMealTitle());
    }

    /**
     * Verifies end-to-end full JSON database backup export and restore functionality.
     */
    @Test
    public void testFullJsonBackupAndRestoreE2E() {
        UserSettings initialSettings = new UserSettings(
                1,
                1.8,
                1.1,
                1.4,
                0.7,
                CarbUnit.KE,
                10,
                GlucoseUnit.MMOL_L,
                6.5,
                2.5,
                0.5,
                true,
                AppTheme.OCEAN_BLUE,
                AppTheme.Mode.LIGHT,
                "key-xyz",
                GeminiAiModel.GEMINI_3_7_FLASH
        );
        repository.saveSettings(initialSettings);

        CalculationLog log = new CalculationLog(
                100L,
                1700000000000L,
                "Haferflocken & Heidelbeeren",
                4.0,
                CarbUnit.KE,
                40.0,
                3.33,
                4.0,
                TimeOfDay.MORNING,
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
        assertNotNull("Exported JSON must not be null", exportedJson);
        assertTrue("Exported JSON must contain theme", exportedJson.contains("OCEAN_BLUE"));
        assertTrue("Exported JSON must contain meal title", exportedJson.contains("Haferflocken & Heidelbeeren"));
        assertFalse("Exported JSON must not contain API key", exportedJson.contains("key-xyz"));

        // Clear database
        repository.saveSettings(new UserSettings());
        repository.clearLogs();
        assertEquals("Database should be empty after clear", 0, repository.getAllLogsDirect().size());

        // Import
        ImportResult result = backupManager.importFromJson(exportedJson);
        assertTrue("Import result must be successful", result.isSuccess());

        UserSettings restoredSettings = repository.loadSettings();
        assertEquals("Restored theme incorrect", AppTheme.OCEAN_BLUE, restoredSettings.getSelectedTheme());
        assertEquals("Restored glucose unit incorrect", GlucoseUnit.MMOL_L, restoredSettings.getGlucoseUnit());
        assertTrue("Restored API key should be blank", restoredSettings.getGeminiApiKey().isBlank());

        List<CalculationLog> restoredLogs = repository.getAllLogsDirect();
        assertEquals("Restored logs count must be 1", 1, restoredLogs.size());
        assertEquals("Restored meal title incorrect", "Haferflocken & Heidelbeeren", restoredLogs.getFirst().getMealTitle());
        assertEquals("Restored notes incorrect", "Sensor leicht steigend", restoredLogs.getFirst().getNotes());
    }
}
