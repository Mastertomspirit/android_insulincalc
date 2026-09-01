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
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.util.AppConstants;
import network.spiritscorp.util.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Handles CSV export formatting and parsing for insulin calculation logs.
 */
public class CsvBackupHandler {

    private static final String TAG = "CsvBackupHandler";

    /**
     * CSV Schema Format Version.
     */
    public static final int CSV_FORMAT_VERSION = AppConstants.CSV_BACKUP_VERSION;

    public static final String CSV_HEADER = "ID,Timestamp,Date,MealTitle,RawCarbInput,CarbUnit,CarbGrams,BE,KE,TimeOfDay,InsulinFactor,MealInsulin,BloodGlucose,TargetGlucose,CorrectionFactor,CorrectionInsulin,TotalInsulin,RoundedInsulin,Notes";

    private final SimpleDateFormat isoDateFormat;

    public CsvBackupHandler() {
        this(DateTimeUtils.getIsoDateTimeFormatter());
    }

    public CsvBackupHandler(SimpleDateFormat dateFormat) {
        this.isoDateFormat = dateFormat;
    }

    /**
     * Exports a list of calculation logs to standard CSV format.
     */
    public String exportToCsv(List<CalculationLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");

        if (logs == null || logs.isEmpty()) {
            return sb.toString();
        }

        for (CalculationLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(log.getTimestamp()).append(",");
            sb.append("\"").append(isoDateFormat.format(new Date(log.getTimestamp()))).append("\",");
            sb.append(escapeCsv(log.getMealTitle())).append(",");
            sb.append(log.getRawCarbInput()).append(",");
            sb.append(escapeCsv(log.getCarbUnit())).append(",");
            sb.append(log.getCarbGrams()).append(",");
            sb.append(log.getBeValue()).append(",");
            sb.append(log.getKeValue()).append(",");
            sb.append(escapeCsv(log.getTimeOfDay())).append(",");
            sb.append(log.getInsulinFactor()).append(",");
            sb.append(log.getMealInsulin()).append(",");
            sb.append(log.getBloodGlucose() != null ? log.getBloodGlucose() : "").append(",");
            sb.append(log.getTargetGlucose() != null ? log.getTargetGlucose() : "").append(",");
            sb.append(log.getCorrectionFactor() != null ? log.getCorrectionFactor() : "").append(",");
            sb.append(log.getCorrectionInsulin() != null ? log.getCorrectionInsulin() : "").append(",");
            sb.append(log.getTotalInsulin()).append(",");
            sb.append(log.getRoundedInsulin()).append(",");
            sb.append(escapeCsv(log.getNotes() != null ? log.getNotes() : ""));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Parses a CSV string into a list of CalculationLog entries.
     */
    public List<CalculationLog> parseCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<CalculationLog> logs = new ArrayList<>();
        String[] lines = csvContent.split("\r?\n");

        boolean isFirstLine = true;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            if (isFirstLine) {
                isFirstLine = false;
                if (trimmedLine.toUpperCase(Locale.ROOT).startsWith("ID") || trimmedLine.contains("MealTitle")) {
                    continue; // Skip header row
                }
            }

            try {
                List<String> tokens = splitCsvLine(line);
                if (tokens.size() < 10) continue;

                long id = parseLongSafe(tokens.get(0), 0L);
                long timestamp = parseLongSafe(tokens.get(1), System.currentTimeMillis());
                // tokens[2] is human readable date string
                String mealTitle = tokens.size() > 3 ? tokens.get(3) : "Mahlzeit";
                double rawCarbInput = tokens.size() > 4 ? parseDoubleSafe(tokens.get(4), 0.0) : 0.0;
                String carbUnit = tokens.size() > 5 ? tokens.get(5) : "g KH";
                double carbGrams = tokens.size() > 6 ? parseDoubleSafe(tokens.get(6), 0.0) : 0.0;
                double beValue = tokens.size() > 7 ? parseDoubleSafe(tokens.get(7), 0.0) : 0.0;
                double keValue = tokens.size() > 8 ? parseDoubleSafe(tokens.get(8), 0.0) : 0.0;
                String timeOfDay = tokens.size() > 9 ? tokens.get(9) : "Morgens";
                double insulinFactor = tokens.size() > 10 ? parseDoubleSafe(tokens.get(10), 1.0) : 1.0;
                double mealInsulin = tokens.size() > 11 ? parseDoubleSafe(tokens.get(11), 0.0) : 0.0;
                Double bloodGlucose = tokens.size() > 12 ? parseNullableDouble(tokens.get(12)) : null;
                Double targetGlucose = tokens.size() > 13 ? parseNullableDouble(tokens.get(13)) : null;
                Double correctionFactor = tokens.size() > 14 ? parseNullableDouble(tokens.get(14)) : null;
                Double correctionInsulin = tokens.size() > 15 ? parseNullableDouble(tokens.get(15)) : null;
                double totalInsulin = tokens.size() > 16 ? parseDoubleSafe(tokens.get(16), 0.0) : 0.0;
                double roundedInsulin = tokens.size() > 17 ? parseDoubleSafe(tokens.get(17), 0.0) : 0.0;
                String notes = tokens.size() > 18 ? tokens.get(18) : "";

                logs.add(new CalculationLog(
                        id, timestamp, mealTitle, rawCarbInput, carbUnit, carbGrams,
                        beValue, keValue, timeOfDay, insulinFactor, mealInsulin,
                        bloodGlucose, targetGlucose, correctionFactor, correctionInsulin,
                        totalInsulin, roundedInsulin, notes
                ));
            } catch (Exception e) {
                Log.w(TAG, "Skipping malformed CSV line: " + line, e);
            }
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

    public String escapeCsv(String input) {
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
