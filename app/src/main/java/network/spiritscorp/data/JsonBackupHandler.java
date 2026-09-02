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
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;
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
 * Handles JSON serialization and deserialization for therapy configuration and calculation logs.
 */
public class JsonBackupHandler {

    private static final String TAG = "JsonBackupHandler";
    public static final int BACKUP_VERSION = AppConstants.JSON_BACKUP_VERSION;

    private final SimpleDateFormat isoDateFormat;

    public JsonBackupHandler() {
        this(DateTimeUtils.getIsoDateTimeFormatter());
    }

    public JsonBackupHandler(SimpleDateFormat dateFormat) {
        this.isoDateFormat = dateFormat;
    }

    /**
     * Serializes UserSettings and CalculationLogs into a formatted JSON string.
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
                settingsObj.put("geminiApiKey", settings.getGeminiApiKey() != null ? settings.getGeminiApiKey() : "");
                settingsObj.put("selectedAiModel", settings.getSelectedAiModel() != null ? settings.getSelectedAiModel() : "gemini-3.5-flash");
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
                logObj.put("carbUnit", log.getCarbUnit());
                logObj.put("carbGrams", log.getCarbGrams());
                logObj.put("beValue", log.getBeValue());
                logObj.put("keValue", log.getKeValue());
                logObj.put("timeOfDay", log.getTimeOfDay());
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
     * Parses a JSON backup string into UserSettings and a list of CalculationLogs.
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

    private UserSettings parseUserSettingsJson(JSONObject obj) {
        UserSettings settings = new UserSettings();
        if (obj.has("morningFactor")) settings.setMorningFactor(obj.optDouble("morningFactor", 1.5));
        if (obj.has("noonFactor")) settings.setNoonFactor(obj.optDouble("noonFactor", 1.0));
        if (obj.has("eveningFactor")) settings.setEveningFactor(obj.optDouble("eveningFactor", 1.2));
        if (obj.has("nightFactor")) settings.setNightFactor(obj.optDouble("nightFactor", 0.8));
        if (obj.has("defaultCarbUnit")) settings.setDefaultCarbUnit(obj.optString("defaultCarbUnit", "g KH"));
        if (obj.has("beGramsDivisor")) {
            settings.setBeGramsDivisor(obj.optInt("beGramsDivisor", 12));
        } else if (obj.has("gramsPerBe")) {
            settings.setBeGramsDivisor(obj.optInt("gramsPerBe", 12));
        }
        if (obj.has("glucoseUnit")) settings.setGlucoseUnit(obj.optString("glucoseUnit", "mg/dl"));
        if (obj.has("targetGlucoseMgDl")) settings.setTargetGlucoseMgDl(obj.optDouble("targetGlucoseMgDl", 120.0));
        if (obj.has("correctionFactorMgDl")) settings.setCorrectionFactorMgDl(obj.optDouble("correctionFactorMgDl", 50.0));
        if (obj.has("roundingStep")) settings.setRoundingStep(obj.optDouble("roundingStep", 0.5));
        if (obj.has("showDisclaimer")) {
            settings.setShowDisclaimer(obj.optBoolean("showDisclaimer", true));
        } else if (obj.has("autoTimeDetection")) {
            settings.setShowDisclaimer(obj.optBoolean("autoTimeDetection", true));
        }
        if (obj.has("selectedTheme")) settings.setSelectedTheme(obj.optString("selectedTheme", "MEDICAL_TEAL"));
        if (obj.has("themeMode")) settings.setThemeMode(obj.optString("themeMode", "SYSTEM"));
        if (obj.has("geminiApiKey")) settings.setGeminiApiKey(obj.optString("geminiApiKey", null));
        if (obj.has("selectedAiModel")) settings.setSelectedAiModel(obj.optString("selectedAiModel", "gemini-2.5-flash"));
        return settings;
    }

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
                    obj.optString("carbUnit", "g KH"),
                    obj.optDouble("carbGrams", 0.0),
                    obj.optDouble("beValue", 0.0),
                    obj.optDouble("keValue", 0.0),
                    obj.optString("timeOfDay", "Morgens"),
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
