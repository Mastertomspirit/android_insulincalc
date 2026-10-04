package network.spiritscorp.data;

/*
 * Copyright (C) 2026 Tom Spirit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kotlin.Pair;
import network.spiritscorp.ai.GeminiAiModel;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.GlucoseUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;
import network.spiritscorp.ui.theme.AppTheme;
import network.spiritscorp.util.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link JsonBackupHandler}.
 * Tests serialization and deserialization of therapy settings and calculation logs.
 */
class JsonBackupHandlerTest {

    // === CONSTANTS & FIXTURES ===
    private static final long TEST_TIMESTAMP = 1700000000000L;
    private static final double DELTA = 0.0001;

    // User Settings Constants
    private static final float MORNING_FACTOR = 1.5f;
    private static final float NOON_FACTOR = 1.0f;
    private static final float EVENING_FACTOR = 1.2f;
    private static final float NIGHT_FACTOR = 0.8f;
    private static final CarbUnit DEFAULT_CARB_UNIT = CarbUnit.GRAMS;
    private static final int BE_GRAMS_DIVISOR = 12;
    private static final GlucoseUnit GLUCOSE_UNIT = GlucoseUnit.MG_DL;
    private static final int TARGET_GLUCOSE = 100;
    private static final int CORRECTION_FACTOR = 40;
    private static final float ROUNDING_STEP = 0.5f;
    private static final boolean SHOW_DISCLAIMER = false;
    private static final AppTheme SELECTED_THEME = AppTheme.BERRY_VIOLET;
    private static final AppTheme.Mode THEME_MODE = AppTheme.Mode.SYSTEM;
    private static final String API_KEY = "API_KEY";
    private static final GeminiAiModel CUSTOM_AI_MODEL = GeminiAiModel.GEMINI_FLASH_LATEST;

    // Calculation Log Constants
    private static final long LOG_ID = 101L;
    private static final String MEAL_TITLE = "Spaghetti Bolognese";
    private static final double RAW_CARB_INPUT = 60.0;
    private static final double CARB_GRAMS = 60.0;
    private static final double BE_VALUE = 5.0;
    private static final double KE_VALUE = 6.0;
    private static final TimeOfDay TIME_OF_DAY = TimeOfDay.NOON;
    private static final double INSULIN_FACTOR = 1.0;
    private static final double MEAL_INSULIN = 5.0;
    private static final Double BLOOD_GLUCOSE = 180.0;
    private static final Double TARGET_GLUCOSE_LOG = 100.0;
    private static final Double CORRECTION_FACTOR_LOG = 40.0;
    private static final Double CORRECTION_INSULIN = 2.0;
    private static final double TOTAL_INSULIN = 7.0;
    private static final double ROUNDED_INSULIN = 7.0;
    private static final String NOTES = "Pre-meal workout completed";

    private JsonBackupHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JsonBackupHandler(DateTimeUtils.getFilenameTimestampFormatter());
    }

    // ==========================================
    // EXPORT TESTS
    // ==========================================

    @Test
    @DisplayName("exportToJson: Null settings and empty logs return base metadata and empty logs array")
    void exportToJsonNullSettingsAndEmptyLogsTest() throws JSONException {
        String jsonResult = handler.exportToJson(null, Collections.emptyList());
        JSONObject jsonObject = new JSONObject(jsonResult);

        assertAll("Verify root JSON fields",
                () -> assertNotNull(jsonResult, "Exported JSON string should not be null"),
                () -> assertTrue(jsonObject.has("version"), "JSON root should contain 'version'"),
                () -> assertTrue(jsonObject.has("exportDate"), "JSON root should contain 'exportDate'"),
                () -> assertEquals("InsulinRechner", jsonObject.getString("app"), "App name mismatch"),
                () -> assertTrue(jsonObject.has("logs"), "JSON root should contain 'logs'"),
                () -> assertEquals(0, jsonObject.getJSONArray("logs").length(), "Logs array should be empty"),
                () -> assertFalse(jsonObject.has("settings"), "Settings key should be absent when null")
        );
    }

    @Test
    @DisplayName("exportToJson: Full verification of serialized settings and logs")
    void exportToJsonFullVerificationTest() throws JSONException {
        UserSettings settings = createSampleUserSettings();
        CalculationLog log = createSampleCalculationLog();
        List<CalculationLog> logs = Collections.singletonList(log);

        String jsonResult = handler.exportToJson(settings, logs);

        JSONObject jsonObject = new JSONObject(jsonResult);
        JSONObject settingsObj = jsonObject.getJSONObject("settings");
        JSONArray logsArray = jsonObject.getJSONArray("logs");

        assertAll("Verify serialized UserSettings fields",
                () -> assertNotNull(jsonResult, "Exported JSON string should not be null"),
                () -> assertTrue(jsonObject.has("settings"), "Exported JSON must contain 'settings'"),
                () -> assertTrue(jsonObject.has("logs"), "Exported JSON must contain 'logs'"),
                () -> assertEquals(1, logsArray.length(), "Logs array length mismatch"),
                () -> assertEquals(1, settingsObj.getInt("id"), "Settings ID mismatch"),
                () -> assertEquals(MORNING_FACTOR, (float) settingsObj.getDouble("morningFactor"), DELTA, "Morning factor mismatch"),
                () -> assertEquals(NOON_FACTOR, (float) settingsObj.getDouble("noonFactor"), DELTA, "Noon factor mismatch"),
                () -> assertEquals(EVENING_FACTOR, (float) settingsObj.getDouble("eveningFactor"), DELTA, "Evening factor mismatch"),
                () -> assertEquals(NIGHT_FACTOR, (float) settingsObj.getDouble("nightFactor"), DELTA, "Night factor mismatch"),
                () -> assertEquals(TARGET_GLUCOSE, settingsObj.getInt("targetGlucoseMgDl"), "Target glucose mismatch"),
                () -> assertEquals(CORRECTION_FACTOR, settingsObj.getInt("correctionFactorMgDl"), "Correction factor mismatch"),
                () -> assertEquals(SHOW_DISCLAIMER, settingsObj.getBoolean("showDisclaimer"), "Show disclaimer mismatch"),
                () -> assertEquals(CUSTOM_AI_MODEL.name(), settingsObj.getString("selectedAiModel"), "AI model mismatch"),
                () -> assertThrows(JSONException.class, () -> settingsObj.getString("geminiApiKey"), "Api Key should not be in JSON")
        );
    }

    // ==========================================
    // PARSE TESTS
    // ==========================================

    @Test
    @DisplayName("parseJson: Null or blank input returns null")
    void parseJsonNullOrEmptyInputTest() {
        assertAll("Verify null output for blank inputs",
                () -> assertNull(handler.parseJson(null), "Null input should return null"),
                () -> assertNull(handler.parseJson(""), "Empty input should return null"),
                () -> assertNull(handler.parseJson("   "), "Whitespace input should return null")
        );
    }

    @Test
    @DisplayName("parseJson: Invalid JSON syntax returns null")
    void parseJsonInvalidJsonTest() {
        String invalidJson = "{ \"version\": 1, \"settings\": { invalid } }";
        Pair<UserSettings, List<CalculationLog>> result = handler.parseJson(invalidJson);
        assertNull(result, "Parsing invalid JSON syntax should return null");
    }

    @Test
    @DisplayName("parseJson: Legacy JSON array returns null settings and parsed logs")
    void parseJsonLegacyArrayTest() throws JSONException {
        JSONArray array = new JSONArray();
        array.put(createSampleLogJsonObject());

        Pair<UserSettings, List<CalculationLog>> result = handler.parseJson(array.toString());
        List<CalculationLog> logs = result.getSecond();

        assertAll("Verify legacy JSON array parsing",
                () -> assertNull(result.getFirst(), "UserSettings should be null for legacy array JSON"),
                () -> assertNotNull(result, "Result pair should not be null"),
                () -> assertNotNull(logs, "Logs list should not be null"),
                () -> assertEquals(1, logs.size(), "Logs list size mismatch")
        );
    }

    @Test
    @DisplayName("parseJson: Full verification of parsed UserSettings and CalculationLogs")
    void parseJsonFullVerificationTest() throws JSONException {
        JSONObject root = new JSONObject();

        JSONObject settingsObj = new JSONObject();
        settingsObj.put("id", 1);
        settingsObj.put("morningFactor", (double) MORNING_FACTOR);
        settingsObj.put("noonFactor", (double) NOON_FACTOR);
        settingsObj.put("eveningFactor", (double) EVENING_FACTOR);
        settingsObj.put("nightFactor", (double) NIGHT_FACTOR);
        settingsObj.put("defaultCarbUnit", DEFAULT_CARB_UNIT.name());
        settingsObj.put("beGramsDivisor", BE_GRAMS_DIVISOR);
        settingsObj.put("glucoseUnit", GLUCOSE_UNIT.name());
        settingsObj.put("targetGlucoseMgDl", TARGET_GLUCOSE);
        settingsObj.put("correctionFactorMgDl", CORRECTION_FACTOR);
        settingsObj.put("roundingStep", (double) ROUNDING_STEP);
        settingsObj.put("showDisclaimer", SHOW_DISCLAIMER);
        settingsObj.put("selectedTheme", SELECTED_THEME.name());
        settingsObj.put("themeMode", THEME_MODE);
        settingsObj.put("selectedAiModel", CUSTOM_AI_MODEL);

        root.put("settings", settingsObj);

        JSONArray logsArray = new JSONArray();
        logsArray.put(createSampleLogJsonObject());
        root.put("logs", logsArray);

        Pair<UserSettings, List<CalculationLog>> result = handler.parseJson(root.toString());
        UserSettings settings = result.getFirst();
        List<CalculationLog> logs = result.getSecond();
        CalculationLog log = logs.getFirst();

        assertAll("Verify all parsed fields",
                () -> assertNotNull(settings, "Parsed UserSettings must not be null"),
                () -> assertNotNull(result, "Parsed result pair must not be null"),
                () -> assertEquals(MORNING_FACTOR, settings.getMorningFactor(), DELTA, "Morning factor mismatch"),
                () -> assertEquals(NOON_FACTOR, settings.getNoonFactor(), DELTA, "Noon factor mismatch"),
                () -> assertEquals(EVENING_FACTOR, settings.getEveningFactor(), DELTA, "Evening factor mismatch"),
                () -> assertEquals(NIGHT_FACTOR, settings.getNightFactor(), DELTA, "Night factor mismatch"),
                () -> assertEquals(BE_GRAMS_DIVISOR, settings.getBeGramsDivisor(), DELTA, "BE grams divisor mismatch"),
                () -> assertEquals(TARGET_GLUCOSE, settings.getTargetGlucoseMgDl(), "Target glucose mismatch"),
                () -> assertEquals(CORRECTION_FACTOR, settings.getCorrectionFactorMgDl(), "Correction factor mismatch"),
                () -> assertEquals(SHOW_DISCLAIMER, settings.isShowDisclaimer(), "Show disclaimer mismatch"),
                () -> assertEquals(CUSTOM_AI_MODEL, settings.getSelectedAiModel(), "Selected AI model mismatch"),
                () -> assertNotNull(logs, "Parsed logs list must not be null"),
                () -> assertEquals(1, logs.size(), "Logs list size mismatch"),
                () -> assertEquals(LOG_ID, log.getId(), "Log ID mismatch"),
                () -> assertEquals(TEST_TIMESTAMP, log.getTimestamp(), "Timestamp mismatch"),
                () -> assertEquals(MEAL_TITLE, log.getMealTitle(), "Meal title mismatch"),
                () -> assertEquals(RAW_CARB_INPUT, log.getRawCarbInput(), DELTA, "Raw carb input mismatch"),
                () -> assertEquals(CARB_GRAMS, log.getCarbGrams(), DELTA, "Carb grams mismatch"),
                () -> assertEquals(BE_VALUE, log.getBeValue(), DELTA, "BE value mismatch"),
                () -> assertEquals(KE_VALUE, log.getKeValue(), DELTA, "KE value mismatch"),
                () -> assertEquals(MEAL_INSULIN, log.getMealInsulin(), DELTA, "Meal insulin mismatch"),
                () -> assertEquals(BLOOD_GLUCOSE, log.getBloodGlucose(), DELTA, "Blood glucose mismatch"),
                () -> assertEquals(TARGET_GLUCOSE_LOG, log.getTargetGlucose(), DELTA, "Target glucose mismatch"),
                () -> assertEquals(CORRECTION_FACTOR_LOG, log.getCorrectionFactor(), DELTA, "Correction factor mismatch"),
                () -> assertEquals(CORRECTION_INSULIN, log.getCorrectionInsulin(), DELTA, "Correction insulin mismatch"),
                () -> assertEquals(TOTAL_INSULIN, log.getTotalInsulin(), DELTA, "Total insulin mismatch"),
                () -> assertEquals(ROUNDED_INSULIN, log.getRoundedInsulin(), DELTA, "Rounded insulin mismatch"),
                () -> assertEquals(NOTES, log.getNotes(), "Notes mismatch")
        );
    }

    // ==========================================
    // ROUNDTRIP TEST
    // ==========================================

    @Test
    @DisplayName("Roundtrip: Exporting to JSON and re-parsing preserves original data integrity")
    void roundtripExportAndParseTest() {
        UserSettings originalSettings = createSampleUserSettings();
        CalculationLog originalLog = createSampleCalculationLog();

        String jsonOutput = handler.exportToJson(originalSettings, Collections.singletonList(originalLog));
        Pair<UserSettings, List<CalculationLog>> parsedResult = handler.parseJson(jsonOutput);

        UserSettings parsedSettings = parsedResult.getFirst();
        List<CalculationLog> parsedLogs = parsedResult.getSecond();
        CalculationLog parsedLog = parsedLogs.getFirst();

        assertAll("Verify roundtrip equality",
                () -> assertNotNull(parsedResult, "Parse step should produce a non-null Pair"),
                () -> assertNotNull(parsedSettings, "Parsed settings should not be null"),
                () -> assertNotNull(parsedLogs, "Parsed logs should not be null"),
                () -> assertEquals(originalSettings.getMorningFactor(), parsedSettings.getMorningFactor(), DELTA, "Morning factor roundtrip equality"),
                () -> assertEquals(originalSettings.getSelectedAiModel(), parsedSettings.getSelectedAiModel(), "AI model roundtrip equality"),
                () -> assertEquals("", parsedSettings.getGeminiApiKey(), "Api Key roundtrip equality"),
                () -> assertEquals(originalLog.getId(), parsedLog.getId(), "Log ID roundtrip equality"),
                () -> assertEquals(originalLog.getMealTitle(), parsedLog.getMealTitle(), "Meal title roundtrip equality"),
                () -> assertEquals(originalLog.getCarbGrams(), parsedLog.getCarbGrams(), DELTA, "Carb grams roundtrip equality"),
                () -> assertEquals(originalLog.getTotalInsulin(), parsedLog.getTotalInsulin(), DELTA, "Total insulin roundtrip equality")
        );
    }

    // ==========================================
    // FIXTURE HELPERS
    // ==========================================

    private UserSettings createSampleUserSettings() {
        return new UserSettings(
                1, MORNING_FACTOR, NOON_FACTOR, EVENING_FACTOR, NIGHT_FACTOR,
                DEFAULT_CARB_UNIT, BE_GRAMS_DIVISOR, GLUCOSE_UNIT, TARGET_GLUCOSE,
                CORRECTION_FACTOR, ROUNDING_STEP, SHOW_DISCLAIMER, SELECTED_THEME,
                THEME_MODE, API_KEY, CUSTOM_AI_MODEL
        );
    }

    private CalculationLog createSampleCalculationLog() {
        return new CalculationLog(
                LOG_ID, TEST_TIMESTAMP, MEAL_TITLE, RAW_CARB_INPUT, DEFAULT_CARB_UNIT,
                CARB_GRAMS, BE_VALUE, KE_VALUE, TIME_OF_DAY, INSULIN_FACTOR,
                MEAL_INSULIN, BLOOD_GLUCOSE, TARGET_GLUCOSE_LOG, CORRECTION_FACTOR_LOG,
                CORRECTION_INSULIN, TOTAL_INSULIN, ROUNDED_INSULIN, NOTES
        );
    }

    private JSONObject createSampleLogJsonObject() throws JSONException {
        JSONObject logObj = new JSONObject();
        logObj.put("id", LOG_ID);
        logObj.put("timestamp", TEST_TIMESTAMP);
        logObj.put("mealTitle", MEAL_TITLE);
        logObj.put("rawCarbInput", RAW_CARB_INPUT);
        logObj.put("carbUnit", DEFAULT_CARB_UNIT.name());
        logObj.put("carbGrams", CARB_GRAMS);
        logObj.put("beValue", BE_VALUE);
        logObj.put("keValue", KE_VALUE);
        logObj.put("timeOfDay", TIME_OF_DAY.name());
        logObj.put("insulinFactor", INSULIN_FACTOR);
        logObj.put("mealInsulin", MEAL_INSULIN);
        logObj.put("bloodGlucose", BLOOD_GLUCOSE);
        logObj.put("targetGlucose", TARGET_GLUCOSE_LOG);
        logObj.put("correctionFactor", CORRECTION_FACTOR_LOG);
        logObj.put("correctionInsulin", CORRECTION_INSULIN);
        logObj.put("totalInsulin", TOTAL_INSULIN);
        logObj.put("roundedInsulin", ROUNDED_INSULIN);
        logObj.put("notes", NOTES);
        return logObj;
    }
}