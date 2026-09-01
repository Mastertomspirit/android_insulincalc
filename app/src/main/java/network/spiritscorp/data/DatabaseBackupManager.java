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
import android.net.Uri;
import android.util.Log;
import kotlin.Pair;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * High-level coordinator service for backup and restore operations across database, JSON, and CSV.
 * Delegates JSON processing to {@link JsonBackupHandler} and CSV processing to {@link CsvBackupHandler}.
 */
public class DatabaseBackupManager {

    private static final String TAG = "DatabaseBackupManager";

    private final UserSettingsDao userSettingsDao;
    private final CalculationLogDao calculationLogDao;
    private final JsonBackupHandler jsonHandler;
    private final CsvBackupHandler csvHandler;

    public DatabaseBackupManager() {
        this(null, null);
    }

    public DatabaseBackupManager(UserSettingsDao userSettingsDao, CalculationLogDao calculationLogDao) {
        this.userSettingsDao = userSettingsDao;
        this.calculationLogDao = calculationLogDao;
        this.jsonHandler = new JsonBackupHandler();
        this.csvHandler = new CsvBackupHandler();
    }

    public DatabaseBackupManager(AppDatabase db) {
        this(db.userSettingsDao(), db.calculationLogDao());
    }

    public DatabaseBackupManager(Context context) {
        this(AppDatabase.getDatabase(context));
    }

    /**
     * Exports complete database state to a JSON string.
     */
    public String exportToJson() {
        UserSettings settings = userSettingsDao != null ? userSettingsDao.getSettingsDirect() : null;
        if (settings == null) {
            settings = new UserSettings();
        }
        List<CalculationLog> logs = calculationLogDao != null ? calculationLogDao.getAllLogsDirect() : Collections.emptyList();
        return jsonHandler.exportToJson(settings, logs);
    }

    public String exportToJson(UserSettings settings, List<CalculationLog> logs) {
        return jsonHandler.exportToJson(settings, logs);
    }

    public Pair<UserSettings, List<CalculationLog>> parseJson(String jsonString) {
        return jsonHandler.parseJson(jsonString);
    }

    public ImportResult importFromJson(String jsonContent) {
        Pair<UserSettings, List<CalculationLog>> data = parseJson(jsonContent);
        if (data == null) {
            return new ImportResult(false, 0, false, "Ungültiges oder beschädigtes JSON-Format.");
        }

        boolean importedSettings = false;
        if (data.getFirst() != null && userSettingsDao != null) {
            userSettingsDao.saveSettings(data.getFirst());
            importedSettings = true;
        }

        int logsCount = 0;
        if (data.getSecond() != null && !data.getSecond().isEmpty() && calculationLogDao != null) {
            calculationLogDao.insertLogs(data.getSecond());
            logsCount = data.getSecond().size();
        }

        return new ImportResult(
                true,
                logsCount,
                importedSettings,
                String.format(Locale.getDefault(), "Erfolgreich %d Einträge %s wiederhergestellt.", logsCount, importedSettings ? "und Einstellungen" : "")
        );
    }

    public String exportToCsv() {
        List<CalculationLog> logs = calculationLogDao != null ? calculationLogDao.getAllLogsDirect() : Collections.emptyList();
        return csvHandler.exportToCsv(logs);
    }

    public String exportToCsv(List<CalculationLog> logs) {
        return csvHandler.exportToCsv(logs);
    }

    public List<CalculationLog> parseCsv(String csvContent) {
        return csvHandler.parseCsv(csvContent);
    }

    public ImportResult importFromCsv(String csvContent) {
        List<CalculationLog> logs = parseCsv(csvContent);
        if (logs.isEmpty()) {
            return new ImportResult(false, 0, false, "Keine gültigen Einträge in der CSV-Datei gefunden.");
        }

        if (calculationLogDao != null) {
            calculationLogDao.insertLogs(logs);
        }

        return new ImportResult(
                true,
                logs.size(),
                false,
                String.format(Locale.getDefault(),"Erfolgreich %d Einträge aus CSV importiert.", logs.size())
        );
    }

    public List<String> splitCsvLine(String line) {
        return csvHandler.splitCsvLine(line);
    }

    public boolean writeTextToUri(Context context, Uri uri, String text) {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(text.getBytes(StandardCharsets.UTF_8));
                os.flush();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed writing text to URI: " + uri, e);
        }
        return false;
    }

    public String readTextFromUri(Context context, Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed reading text from URI: " + uri, e);
            return null;
        }
    }

    public ImportResult importFromUri(Context context, Uri uri) {
        String content = readTextFromUri(context, uri);
        if (content == null || content.trim().isEmpty()) {
            return new ImportResult(false, 0, false, "Konnte Datei nicht lesen oder Datei ist leer.");
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return importFromJson(trimmed);
        } else {
            return importFromCsv(trimmed);
        }
    }

    public static class ImportResult {
        private final boolean success;
        private final int logsImported;
        private final boolean settingsImported;
        private final String message;

        public ImportResult(boolean success, int logsImported, boolean settingsImported, String message) {
            this.success = success;
            this.logsImported = logsImported;
            this.settingsImported = settingsImported;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getImportedLogsCount() {
            return logsImported;
        }

        public boolean isImportedSettings() {
            return settingsImported;
        }

        public String getMessage() {
            return message;
        }
    }
}
