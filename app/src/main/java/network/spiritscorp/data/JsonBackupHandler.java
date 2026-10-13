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

import android.util.Log;
import androidx.annotation.NonNull;
import kotlin.Pair;
import network.spiritscorp.ai.GeminiAiModel;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.GlucoseUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;
import network.spiritscorp.ui.theme.AppTheme;
import network.spiritscorp.util.AppConstants;
import network.spiritscorp.util.DateTimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Handles JSON serialization and deserialization for therapy configuration settings and calculation history logs.
 * <p>
 * Provides robust versioned backup export strings and flexible JSON parser implementations
 * capable of handling legacy format variations and missing keys safely.
 */
public class JsonBackupHandler {

    /**
     * Logging tag used for debugging and error reporting during export/import operations.
     */
    private static final String TAG = "JsonBackupHandler";

    /**
     * Version identifier for backup file compatibility tracking.
     */
    private static final int BACKUP_VERSION = AppConstants.JSON_BACKUP_VERSION;

    /**
     * ISO date/time formatter used for serializing export timestamps in backups.
     */
    private final SimpleDateFormat isoDateFormat;

    /**
     * Constructs a JsonBackupHandler using the standard ISO date-time formatter.
     */
    public JsonBackupHandler() {
        this(DateTimeUtils.getIsoDateTimeFormatter());
    }

    /**
     * Constructs a JsonBackupHandler with a custom date formatter.
     *
     * @param dateFormat The {@link SimpleDateFormat} instance for parsing/formatting export dates.
     */
    public JsonBackupHandler(SimpleDateFormat dateFormat) {
        this.isoDateFormat = dateFormat;
    }

    /**
     * Serializes UserSettings and a list of CalculationLogs into a formatted, versioned JSON string.
     *
     * @param settings The active {@link UserSettings} configuration to back up.
     * @param logs     The list of saved {@link CalculationLog} history items.
     * @return A formatted JSON string representation of the backup, or empty JSON object "{}" on failure.
     */
    public String exportToJson(UserSettings settings, List<CalculationLog> logs) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", BACKUP_VERSION);
            root.put("exportDate", isoDateFormat.format(new Date()));
            root.put("app", "InsulinRechner");

            if (settings != null) {
                JSONObject settingsObj = new JSONObject();
                settingsObj.put("id", 1);
                settingsObj.put("morningFactor", settings.getMorningFactor());
                settingsObj.put("noonFactor", settings.getNoonFactor());
                settingsObj.put("eveningFactor", settings.getEveningFactor());
                settingsObj.put("nightFactor", settings.getNightFactor());
                settingsObj.put("defaultCarbUnit", settings.getDefaultCarbUnit());
                settingsObj.put("beGramsDivisor", settings.getBeGramsDivisor());
                settingsObj.put("glucoseUnit", settings.getGlucoseUnit());
                settingsObj.put("targetGlucoseMgDl", settings.getTargetGlucoseMgDl());
                settingsObj.put("correctionFactorMgDl", settings.getCorrectionFactorMgDl());
                settingsObj.put("roundingStep", settings.getRoundingStep());
                settingsObj.put("showDisclaimer", settings.isShowDisclaimer());
                settingsObj.put("selectedTheme", settings.getSelectedTheme());
                settingsObj.put("themeMode", settings.getThemeMode());
                settingsObj.put("selectedAiModel", settings.getSelectedAiModel());
                root.put("settings", settingsObj);
            }

            JSONArray logsArray = getJsonArray(logs);
            root.put("logs", logsArray);

            return root.toString(2);
        } catch (Exception e) {
            Log.e(TAG, "Error serializing backup to JSON: " + e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Parses a JSON backup string into a {@link Pair} containing restored {@link UserSettings}
     * and a list of {@link CalculationLog} items.
     * Handles both root backup objects containing settings+logs and standalone JSON arrays of logs.
     *
     * @param jsonString The raw JSON string from a backup file or import source.
     * @return A {@link Pair} holding optional UserSettings and parsed CalculationLogs, or null if parsing fails.
     */
    public Pair<UserSettings, List<CalculationLog>> parseJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("[")) {
                JSONArray array = new JSONArray(trimmed);
                List<CalculationLog> logs = parseLogsArray(array);
                return new Pair<>(null, logs);
            }

            JSONObject root = new JSONObject(trimmed);

            UserSettings settings = null;
            if (root.has("settings")) {
                JSONObject sObj = root.optJSONObject("settings");
                if (sObj != null) {
                    settings = parseUserSettingsJson(sObj);
                }
            }

            List<CalculationLog> logs = Collections.emptyList();
            if (root.has("logs")) {
                JSONArray lArray = root.optJSONArray("logs");
                if (lArray != null) {
                    logs = parseLogsArray(lArray);
                }
            }

            return new Pair<>(settings, logs);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON backup: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Serializes a list of {@link CalculationLog} objects into a {@link JSONArray}.
     *
     * @param logs List of log entries to convert.
     * @return A {@link JSONArray} containing serialized log JSON objects.
     * @throws JSONException If JSON construction fails.
     */
    @NonNull
    private JSONArray getJsonArray(List<CalculationLog> logs) throws JSONException {
        JSONArray logsArray = new JSONArray();
        if (logs != null) {
            for (CalculationLog log : logs) {
                JSONObject logObj = new JSONObject();
                logObj.put("id", log.getId());
                logObj.put("timestamp", log.getTimestamp());
                logObj.put("mealTitle", log.getMealTitle());
                logObj.put("rawCarbInput", log.getRawCarbInput());
                logObj.put("carbUnit", log.getCarbUnit().name());
                logObj.put("carbGrams", log.getCarbGrams());
                logObj.put("beValue", log.getBeValue());
                logObj.put("keValue", log.getKeValue());
                logObj.put("timeOfDay", log.getTimeOfDay().name());
                logObj.put("insulinFactor", log.getInsulinFactor());
                logObj.put("mealInsulin", log.getMealInsulin());
                if (log.getBloodGlucose() != null) {
                    logObj.put("bloodGlucose", log.getBloodGlucose());
                }
                if (log.getTargetGlucose() != null) {
                    logObj.put("targetGlucose", log.getTargetGlucose());
                }
                if (log.getCorrectionFactor() != null) {
                    logObj.put("correctionFactor", log.getCorrectionFactor());
                }
                if (log.getCorrectionInsulin() != null) {
                    logObj.put("correctionInsulin", log.getCorrectionInsulin());
                }
                logObj.put("totalInsulin", log.getTotalInsulin());
                logObj.put("roundedInsulin", log.getRoundedInsulin());
                logObj.put("notes", log.getNotes() != null ? log.getNotes() : "");
                logsArray.put(logObj);
            }
        }
        return logsArray;
    }

    /**
     * Deserializes a {@link JSONObject} representing user configuration settings into a {@link UserSettings} object.
     *
     * @param obj JSON object containing settings key-value pairs.
     * @return Fully populated {@link UserSettings} object with fallback defaults for missing keys.
     */
    private UserSettings parseUserSettingsJson(JSONObject obj) {
        UserSettings settings = new UserSettings();
        settings.setMorningFactor(getDoubleFlexible(obj, 1.5, "morningFactor"));
        settings.setNoonFactor(getDoubleFlexible(obj, 1.0, "noonFactor"));
        settings.setEveningFactor(getDoubleFlexible(obj, 1.2, "eveningFactor"));
        settings.setNightFactor(getDoubleFlexible(obj, 0.8, "nightFactor"));
        settings.setDefaultCarbUnit(CarbUnit.valueOf(getStringFlexible(obj, "G_KH", "defaultCarbUnit")));
        settings.setBeGramsDivisor(getIntFlexible(obj, 12, "beGramsDivisor"));
        settings.setGlucoseUnit(GlucoseUnit.valueOf(getStringFlexible(obj, "MG_DL", "glucoseUnit")));
        settings.setTargetGlucoseMgDl(getDoubleFlexible(obj, 120.0, "targetGlucoseMgDl"));
        settings.setCorrectionFactorMgDl(getDoubleFlexible(obj, 50.0, "correctionFactorMgDl"));
        settings.setRoundingStep(getDoubleFlexible(obj, 0.5, "roundingStep"));
        settings.setShowDisclaimer(getBooleanFlexible(obj, true, "showDisclaimer"));
        settings.setSelectedTheme(AppTheme.valueOf(getStringFlexible(obj, "MEDICAL_TEAL", "selectedTheme")));
        settings.setThemeMode(AppTheme.Mode.valueOf(getStringFlexible(obj, "SYSTEM", "themeMode")));
        settings.setSelectedAiModel(GeminiAiModel.valueOf(getStringFlexible(obj, "GEMINI_FLASH_LITE_LATEST", "selectedAiModel")));
        return settings;
    }

    /**
     * Flexibly retrieves a double value from a JSON object trying multiple candidate key names and parsing String numbers.
     *
     * @param obj        JSON object to extract value from.
     * @param defaultVal Fallback default value if key is absent or invalid.
     * @param keys       Candidate key names to query in sequence.
     * @return Extracted double value or defaultVal.
     */
    private double getDoubleFlexible(JSONObject obj, double defaultVal, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                Object val = obj.opt(key);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                } else if (val instanceof String) {
                    try {
                        String clean = ((String) val).trim().replace(",", ".");
                        return Double.parseDouble(clean);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return defaultVal;
    }

    /**
     * Flexibly retrieves an integer value from a JSON object trying multiple candidate key names.
     *
     * @param obj        JSON object to extract value from.
     * @param defaultVal Fallback default value if key is absent or invalid.
     * @param keys       Candidate key names to query in sequence.
     * @return Extracted integer value or defaultVal.
     */
    private int getIntFlexible(JSONObject obj, int defaultVal, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                Object val = obj.opt(key);
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                } else if (val instanceof String) {
                    try {
                        String clean = ((String) val).trim();
                        return Integer.parseInt(clean);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return defaultVal;
    }

    /**
     * Flexibly retrieves a String value from a JSON object trying multiple candidate key names.
     *
     * @param obj        JSON object to extract value from.
     * @param defaultVal Fallback default value if key is absent or invalid.
     * @param keys       Candidate key names to query in sequence.
     * @return Extracted String value or defaultVal.
     */
    private String getStringFlexible(JSONObject obj, String defaultVal, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optString(key, defaultVal);
            }
        }
        return defaultVal;
    }

    /**
     * Flexibly retrieves a boolean value from a JSON object trying multiple candidate key names and String values.
     *
     * @param obj        JSON object to extract value from.
     * @param defaultVal Fallback default value if key is absent or invalid.
     * @param keys       Candidate key names to query in sequence.
     * @return Extracted boolean value or defaultVal.
     */
    private boolean getBooleanFlexible(JSONObject obj, boolean defaultVal, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                Object val = obj.opt(key);
                if (val instanceof Boolean) {
                    return (Boolean) val;
                } else if (val instanceof String) {
                    return Boolean.parseBoolean((String) val);
                }
            }
        }
        return defaultVal;
    }

    /**
     * Parses a {@link JSONArray} of calculation log JSON objects into a list of {@link CalculationLog} items.
     *
     * @param array JSON array containing serialized log entries.
     * @return List of parsed {@link CalculationLog} instances.
     */
    private List<CalculationLog> parseLogsArray(JSONArray array) {
        List<CalculationLog> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;

            CalculationLog log = new CalculationLog(
                    obj.optLong("id", 0L),
                    obj.optLong("timestamp", System.currentTimeMillis()),
                    obj.optString("mealTitle", "Mahlzeit"),
                    obj.optDouble("rawCarbInput", 0.0),
                    CarbUnit.fromString(obj.optString("carbUnit", "GRAMS")),
                    obj.optDouble("carbGrams", 0.0),
                    obj.optDouble("beValue", 0.0),
                    obj.optDouble("keValue", 0.0),
                    TimeOfDay.fromString(obj.optString("timeOfDay", "MORNING")),
                    obj.optDouble("insulinFactor", 1.0),
                    obj.optDouble("mealInsulin", 0.0),
                    obj.has("bloodGlucose") && !obj.isNull("bloodGlucose") ? obj.optDouble("bloodGlucose") : null,
                    obj.has("targetGlucose") && !obj.isNull("targetGlucose") ? obj.optDouble("targetGlucose") : null,
                    obj.has("correctionFactor") && !obj.isNull("correctionFactor") ? obj.optDouble("correctionFactor") : null,
                    obj.has("correctionInsulin") && !obj.isNull("correctionInsulin") ? obj.optDouble("correctionInsulin") : null,
                    obj.optDouble("totalInsulin", 0.0),
                    obj.optDouble("roundedInsulin", 0.0),
                    obj.optString("notes", "")
            );
            list.add(log);
        }
        return list;
    }
}
