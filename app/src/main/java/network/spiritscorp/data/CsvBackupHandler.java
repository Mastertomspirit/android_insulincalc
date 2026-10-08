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

import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.util.AppConstants;
import network.spiritscorp.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles CSV export formatting and parsing for insulin calculation logs.
 */
public class CsvBackupHandler {

    private static final String TAG = "CsvBackupHandler";

    /**
     * CSV Schema Format Version.
     */
    private static final int CSV_FORMAT_VERSION = AppConstants.CSV_BACKUP_VERSION;

    private static final String CSV_VERSION = "# CSV Format Version: " + CSV_FORMAT_VERSION;
    private static final String CSV_HEADER = "ID,Timestamp,Date,MealTitle,RawCarbInput,CarbUnit,CarbGrams,BE,KE,TimeOfDay,InsulinFactor,MealInsulin,BloodGlucose,TargetGlucose,CorrectionFactor,CorrectionInsulin,TotalInsulin,RoundedInsulin,Notes";
    static final String CSV_HEAD = String.format("%s\n%s\n",CSV_VERSION, CSV_HEADER);

    /**
     * Exports a list of calculation logs to standard CSV format.
     */
    public String exportToCsv(List<CalculationLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEAD);

        if (logs == null || logs.isEmpty()) {
            return sb.toString();
        }

        for (CalculationLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(log.getTimestamp()).append(",");
            sb.append(escapeCsv(DateTimeUtils.formatIsoDateTime(log.getTimestamp()))).append(",");
            sb.append(escapeCsv(log.getMealTitle())).append(",");
            sb.append(log.getRawCarbInput()).append(",");
            sb.append(escapeCsv(log.getCarbUnit().name())).append(",");
            sb.append(log.getCarbGrams()).append(",");
            sb.append(log.getBeValue()).append(",");
            sb.append(log.getKeValue()).append(",");
            sb.append(escapeCsv(log.getTimeOfDay().name())).append(",");
            sb.append(log.getInsulinFactor()).append(",");
            sb.append(log.getMealInsulin()).append(",");
            sb.append(log.getBloodGlucose() != null ? log.getBloodGlucose() : "").append(",");
            sb.append(log.getTargetGlucose() != null ? log.getTargetGlucose() : "").append(",");
            sb.append(log.getCorrectionFactor() != null ? log.getCorrectionFactor() : "").append(",");
            sb.append(log.getCorrectionInsulin() != null ? log.getCorrectionInsulin() : "").append(",");
            sb.append(log.getTotalInsulin()).append(",");
            sb.append(log.getRoundedInsulin()).append(",");
            sb.append(escapeCsv(log.getNotes()));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Parses a CSV string into a list of CalculationLog entries.
     */
    public List<CalculationLog> parseCsv(@NonNull String csvContent) {
        if (csvContent.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<CalculationLog> logs = new ArrayList<>();
        boolean isFirstLine = true;

        for (String line : csvContent.split("\r?\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            List<String> tokens = splitCsvLine(line);
            if (isFirstLine && !tokens.isEmpty() && tokens.get(0).startsWith("#")){
                continue; // Skip comments
            }
            if (isFirstLine && !tokens.isEmpty() && tokens.get(0).equals("ID")) {
                isFirstLine = false;
                continue; // Skip header row
            }

            if (tokens.size() < 18){
                Log.w(TAG, "Skipping malformed CSV line (" + tokens.size() + " fields): " + line);
                continue; // Fields 0-17 mandatory, field 18 (notes) optional
            }
            // TODO throw exception for to many malformed lines?
            logs.add(new CalculationLog(
                    parseLongSafe(tokens.get(0), 0L),                          // 0: entry id
                    parseLongSafe(tokens.get(1), System.currentTimeMillis()),   // 1: epoch timestamp (ms)
                    // 2 is a human-readable date, not used here
                    tokens.get(3),                                              // 3: meal title
                    parseDoubleSafe(tokens.get(4), 0.0),              // 4: raw carb input (as entered)
                    CarbUnit.fromString(tokens.get(5)),                         // 5: carb unit of the raw input
                    parseDoubleSafe(tokens.get(6), 0.0),              // 6: carbs in grams
                    parseDoubleSafe(tokens.get(7), 0.0),              // 7: BE value
                    parseDoubleSafe(tokens.get(8), 0.0),              // 8: KE value
                    TimeOfDay.fromString(tokens.get(9)),                        // 9: time of day (breakfast etc.)
                    parseDoubleSafe(tokens.get(10), 1.0),             // 10: insulin factor (IE per BE/KE)
                    parseDoubleSafe(tokens.get(11), 0.0),             // 11: meal insulin dose
                    parseNullableDouble(tokens.get(12)),                        // 12: measured blood glucose (mg/dL)
                    parseNullableDouble(tokens.get(13)),                        // 13: target glucose value
                    parseNullableDouble(tokens.get(14)),                        // 14: correction factor
                    parseNullableDouble(tokens.get(15)),                        // 15: correction insulin dose
                    parseDoubleSafe(tokens.get(16), 0.0),             // 16: total insulin (meal + correction)
                    parseDoubleSafe(tokens.get(17), 0.0),             // 17: rounded insulin dose
                    tokens.size() > 18 ? tokens.get(18) : ""                    // 18: free-text notes (optional)
            ));
        }
        return logs;
    }

    /**
     * Splits a CSV line handling quoted fields and escaped internal quotes.
     */
    public List<String> splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // Skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    String escapeCsv(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\"", "\"\"") + "\"";
    }

    private double parseDoubleSafe(String val, double defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private Double parseNullableDouble(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long parseLongSafe(String val, long defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
