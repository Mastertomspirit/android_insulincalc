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

import androidx.annotation.NonNull;

import kotlin.Pair;
import network.spiritscorp.R;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * High-level coordinator service for backup and restore operations across database, JSON, and CSV.
 * <p>
 * Delegates JSON processing to {@link JsonBackupHandler} and CSV processing to {@link CsvBackupHandler},
 * while interfacing directly with {@link UserSettingsDao} and {@link CalculationLogDao} to import and export data.
 */
public class DatabaseBackupManager {

    /**
     * Logging tag used for debugging and error reporting.
     */
    private static final String TAG = "DatabaseBackupManager";

    /**
     * Data access object for user settings.
     */
    private final UserSettingsDao userSettingsDao;

    /**
     * Data access object for calculation logs.
     */
    private final CalculationLogDao calculationLogDao;

    /**
     * Handler for JSON export and import operations.
     */
    private final JsonBackupHandler jsonHandler;

    /**
     * Handler for CSV export and import operations.
     */
    private final CsvBackupHandler csvHandler;

    /**
     * Application or activity context used for resolving localized string resources.
     */
    private Context context;

    /**
     * Constructs a new uninitialized DatabaseBackupManager.
     */
    public DatabaseBackupManager() {
        this(null, null);
    }

    /**
     * Constructs a new DatabaseBackupManager with specified DAOs.
     *
     * @param userSettingsDao   DAO for managing user settings.
     * @param calculationLogDao DAO for managing calculation logs.
     */
    public DatabaseBackupManager(UserSettingsDao userSettingsDao, CalculationLogDao calculationLogDao) {
        this.userSettingsDao = userSettingsDao;
        this.calculationLogDao = calculationLogDao;
        this.jsonHandler = new JsonBackupHandler();
        this.csvHandler = new CsvBackupHandler();
    }

    /**
     * Constructs a new DatabaseBackupManager from an {@link AppDatabase} instance.
     *
     * @param db The Room database instance.
     */
    public DatabaseBackupManager(AppDatabase db) {
        this(db.userSettingsDao(), db.calculationLogDao());
    }

    /**
     * Constructs a new DatabaseBackupManager bound to an application context and database.
     *
     * @param context Application context for resource lookups.
     */
    public DatabaseBackupManager(Context context) {
        this(AppDatabase.getDatabase(context));
        this.context = context;
    }

    /**
     * Helper method to retrieve a localized string resource.
     *
     * @param resId     String resource ID.
     * @param formatArgs Optional format arguments.
     * @return The formatted string or an empty string if context is null.
     */
    private String getString(int resId, Object... formatArgs) {
        if (context != null) {
            return context.getString(resId, formatArgs);
        }
        return "";
    }

    /**
     * Exports complete database state (user settings and all calculation logs) to a JSON string.
     *
     * @return Formatted JSON backup string.
     */
    public String exportToJson() {
        UserSettings settings = userSettingsDao != null ? userSettingsDao.getSettingsDirect() : null;
        if (settings == null) {
            settings = new UserSettings();
        }
        List<CalculationLog> logs = calculationLogDao != null ? calculationLogDao.getAllLogsDirect() : Collections.emptyList();
        return jsonHandler.exportToJson(settings, logs);
    }

    /**
     * Exports specific user settings and calculation logs to a JSON string.
     *
     * @param settings The user settings to export.
     * @param logs     The list of calculation logs to export.
     * @return Formatted JSON backup string.
     */
    public String exportToJson(UserSettings settings, List<CalculationLog> logs) {
        return jsonHandler.exportToJson(settings, logs);
    }

    /**
     * Parses a JSON backup string into a Kotlin Pair containing UserSettings and a list of CalculationLogs.
     *
     * @param jsonString The JSON backup string.
     * @return A {@link Pair} of {@link UserSettings} and {@link List} of {@link CalculationLog}.
     */
    public Pair<UserSettings, List<CalculationLog>> parseJson(String jsonString) {
        return jsonHandler.parseJson(jsonString);
    }

    /**
     * Imports user settings and calculation logs from a JSON content string into the database.
     *
     * @param jsonContent The raw JSON backup content.
     * @return An {@link ImportResult} detailing the import outcome, counts, and status message.
     */
    public ImportResult importFromJson(String jsonContent) {
        Pair<UserSettings, List<CalculationLog>> data = parseJson(jsonContent);
        if (data == null) {
            return new ImportResult(false, 0, false, getString(R.string.settings_backup_read_error));
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
                context != null ? context.getResources().getQuantityString(R.plurals.data_database_backup_manager_import_success_json, logsCount, logsCount, importedSettings ? getString(R.string.data_database_backup_manager_import_success_settings_and) : "") : ""
        );
    }

    /**
     * Exports a list of calculation logs to CSV format.
     *
     * @param logs The list of logs to export.
     * @return Formatted CSV string.
     */
    @NonNull
    public String exportToCsv(List<CalculationLog> logs) {
        return csvHandler.exportToCsv(logs);
    }

    /**
     * Parses a CSV content string into a list of calculation logs.
     *
     * @param csvContent The raw CSV content.
     * @return A list of parsed {@link CalculationLog} objects.
     */
    @NonNull
    public List<CalculationLog> parseCsv(String csvContent) {
        return csvHandler.parseCsv(csvContent);
    }

    /**
     * Imports calculation logs from a CSV content string into the database.
     *
     * @param csvContent The raw CSV content string.
     * @return An {@link ImportResult} detailing the import outcome and log count.
     */
    @NonNull
    public ImportResult importFromCsv(String csvContent) {
        List<CalculationLog> logs = parseCsv(csvContent);
        if (logs.isEmpty()) {
            return new ImportResult(false, 0, false, getString(R.string.data_database_backup_manager_csv_import_empty));
        }

        if (calculationLogDao != null) {
            calculationLogDao.insertLogs(logs);
        }

        return new ImportResult(
                true,
                logs.size(),
                false,
                context != null ? context.getResources().getQuantityString(R.plurals.data_database_backup_manager_csv_import_success, logs.size(), logs.size()) : ""
        );
    }

    /**
     * Splits a CSV line into tokens handling quoted fields and escaped quotes.
     *
     * @param line A single line of CSV text.
     * @return A list of column tokens.
     */
    public List<String> splitCsvLine(String line) {
        return csvHandler.splitCsvLine(line);
    }

    /**
     * Writes text content to a given file URI using the content resolver.
     *
     * @param context Application context.
     * @param uri     Target content URI.
     * @param text    Text content to write.
     * @return {@code true} if successful, {@code false} otherwise.
     */
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

    /**
     * Reads text content from a given file URI using the content resolver.
     *
     * @param context Application context.
     * @param uri     Source content URI.
     * @return The read text string, or {@code null} on failure.
     */
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
}
