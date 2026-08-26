package network.spiritscorp.util;

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

import network.spiritscorp.model.CalculationLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Pure Java utility for generating formatted text shares, CSV data, and statistical metrics
 * from diabetic calculation logs.
 */
public final class LogbookExportHelper {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private LogbookExportHelper() {
        // Utility class
    }

    /**
     * Formats a complete export report of all or filtered logs for text sharing (e.g. Email/Messenger).
     */
    public static String generateExportText(List<CalculationLog> logs, String filterDescription) {
        if (logs == null || logs.isEmpty()) {
            return "Insulin-Rechner Tagebuch\nKeine Einträge für den ausgewählten Zeitraum (" + filterDescription + ") vorhanden.";
        }

        double totalCarbs = 0.0;
        double totalInsulin = 0.0;
        double bgSum = 0.0;
        int bgCount = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Insulin-Tagebuch Export\n");
        sb.append("Zeitraum: ").append(filterDescription).append("\n");
        sb.append("Erstellt am: ").append(DATE_FORMAT.format(new Date())).append("\n");
        sb.append("----------------------------------------\n\n");

        for (CalculationLog log : logs) {
            totalCarbs += log.getCarbGrams();
            totalInsulin += log.getRoundedInsulin();
            if (log.getBloodGlucose() != null) {
                bgSum += log.getBloodGlucose();
                bgCount++;
            }

            sb.append("📅 ").append(DATE_FORMAT.format(new Date(log.getTimestamp()))).append(" - ").append(log.getTimeOfDay()).append("\n");
            sb.append("🍽️ Mahlzeit: ").append(log.getMealTitle()).append("\n");
            sb.append("🍞 Kohlenhydrate: ").append(log.getCarbGrams()).append(" g");
            if (log.getBeValue() > 0) {
                sb.append(" (").append(log.getBeValue()).append(" BE / ").append(log.getKeValue()).append(" KE)");
            }
            sb.append("\n");

            if (log.getBloodGlucose() != null) {
                sb.append("🩸 Blutzucker: ").append(log.getBloodGlucose()).append(" ").append(log.getCarbUnit());
                if (log.getTargetGlucose() != null) {
                    sb.append(" (Ziel: ").append(log.getTargetGlucose()).append(")");
                }
                sb.append("\n");
            }

            sb.append("💉 Insulin: ").append(log.getRoundedInsulin()).append(" IE");
            Double corr = log.getCorrectionInsulin();
            if (corr != null && corr != 0.0) {
                sb.append(" (Mahlzeit: ").append(log.getMealInsulin()).append(" IE, Korrektur: ")
                        .append(corr > 0 ? "+" : "")
                        .append(corr).append(" IE)");
            }
            sb.append("\n");

            if (log.getNotes() != null && !log.getNotes().trim().isEmpty()) {
                sb.append("📝 Notiz: ").append(log.getNotes()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("----------------------------------------\n");
        sb.append("📊 Zusammenfassung:\n");
        sb.append("• Anzahl Einträge: ").append(logs.size()).append("\n");
        sb.append("• Gesamte KH: ").append(Math.round(totalCarbs * 10.0) / 10.0).append(" g\n");
        sb.append("• Gesamtes Insulin: ").append(Math.round(totalInsulin * 10.0) / 10.0).append(" IE\n");
        if (bgCount > 0) {
            double avgBg = Math.round((bgSum / bgCount) * 10.0) / 10.0;
            sb.append("• Ø Blutzucker: ").append(avgBg).append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats a single log entry for sharing.
     */
    public static String formatSingleLogShare(CalculationLog log) {
        StringBuilder sb = new StringBuilder();
        sb.append("💉 Insulin-Eintrag: ").append(log.getMealTitle()).append("\n");
        sb.append("📅 ").append(DATE_FORMAT.format(new Date(log.getTimestamp()))).append(" (").append(log.getTimeOfDay()).append(")\n");
        sb.append("🍞 KH: ").append(log.getCarbGrams()).append(" g (").append(log.getBeValue()).append(" BE)\n");
        if (log.getBloodGlucose() != null) {
            sb.append("🩸 BZ: ").append(log.getBloodGlucose()).append("\n");
        }
        sb.append("💉 Dosis: ").append(log.getRoundedInsulin()).append(" IE");
        if (log.getNotes() != null && !log.getNotes().trim().isEmpty()) {
            sb.append("\n📝 Notiz: ").append(log.getNotes());
        }
        return sb.toString();
    }

    /**
     * Builds standard CSV file content for medical or spreadsheet export.
     */
    public static String buildCsvContent(List<CalculationLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID;Datum_Uhrzeit;Tageszeit;Mahlzeit;KH_Gramm;BE;KE;Faktor;Mahlzeiten_Insulin;Blutzucker;Ziel_BZ;Korrektur_Faktor;Korrektur_Insulin;Gesamt_Insulin_IE;Notizen\n");

        for (CalculationLog log : logs) {
            sb.append(log.getId()).append(";")
                    .append(CSV_DATE_FORMAT.format(new Date(log.getTimestamp()))).append(";")
                    .append(escapeCsv(log.getTimeOfDay())).append(";")
                    .append(escapeCsv(log.getMealTitle())).append(";")
                    .append(log.getCarbGrams()).append(";")
                    .append(log.getBeValue()).append(";")
                    .append(log.getKeValue()).append(";")
                    .append(log.getInsulinFactor()).append(";")
                    .append(log.getMealInsulin()).append(";")
                    .append(log.getBloodGlucose() != null ? log.getBloodGlucose() : "").append(";")
                    .append(log.getTargetGlucose() != null ? log.getTargetGlucose() : "").append(";")
                    .append(log.getCorrectionFactor() != null ? log.getCorrectionFactor() : "").append(";")
                    .append(log.getCorrectionInsulin() != null ? log.getCorrectionInsulin() : "").append(";")
                    .append(log.getRoundedInsulin()).append(";")
                    .append(escapeCsv(log.getNotes())).append("\n");
        }

        return sb.toString();
    }

    private static String escapeCsv(String text) {
        if (text == null) return "";
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
