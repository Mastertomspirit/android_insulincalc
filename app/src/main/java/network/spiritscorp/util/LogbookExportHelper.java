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
public class LogbookExportHelper {

    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat csvDateFormat;

    /**
     * Constructs a new LogbookExportHelper using the system default Locale.
     */
    public LogbookExportHelper() {
        this(Locale.getDefault());
    }

    /**
     * Constructs a new LogbookExportHelper with a specific Locale.
     *
     * @param locale Desired locale for date/number formatting.
     */
    public LogbookExportHelper(Locale locale) {
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
        this.dateFormat = new SimpleDateFormat(DateTimeUtils.PATTERN_DISPLAY_DATETIME, effectiveLocale);
        this.csvDateFormat = new SimpleDateFormat(DateTimeUtils.PATTERN_ISO_DATETIME, effectiveLocale);
    }

    /**
     * Formats a complete export report of all or filtered logs for text sharing (e.g. Email/Messenger).
     */
    public String generateExportText(List<CalculationLog> logs, String filterDescription) {
        if (logs == null || logs.isEmpty()) {
            return "Insulin-Rechner Tagebuch\nKeine Einträge für den ausgewählten Zeitraum (" + filterDescription + ") vorhanden.";
        }

        double totalCarbs = 0.0;
        double totalInsulin = 0.0;
        double bgSum = 0.0;
        int bgCount = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Insulin-Rechner Tagebuch-Export\n");
        sb.append("Zeitraum: ").append(filterDescription).append("\n");
        sb.append("Erstellt am: ").append(dateFormat.format(new Date())).append("\n");
        sb.append("----------------------------------------\n\n");

        for (CalculationLog log : logs) {
            totalCarbs += log.getCarbGrams();
            totalInsulin += log.getRoundedInsulin();
            if (log.getBloodGlucose() != null) {
                bgSum += log.getBloodGlucose();
                bgCount++;
            }

            sb.append("📅 ").append(dateFormat.format(new Date(log.getTimestamp()))).append(" - ").append(log.getTimeOfDay()).append("\n");
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
        sb.append("• Einträge: ").append(logs.size()).append("\n");
        sb.append("• Gesamt-KH: ").append(Math.round(totalCarbs)).append(" g\n");
        sb.append("• Gesamtes Insulin: ").append(Math.round(totalInsulin * 10.0) / 10.0).append(" IE\n");
        if (bgCount > 0) {
            double avgBg = Math.round((bgSum / bgCount) * 10.0) / 10.0;
            sb.append("• Ø Blutzucker: ").append(avgBg).append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats a single log entry with full details, breakdown, correction bolus and symbols for sharing.
     */
    public String formatSingleLogShare(CalculationLog log) {
        if (log == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Insulin-Berechnung: ").append(log.getMealTitle()).append("\n");
        sb.append("📅 ").append(dateFormat.format(new Date(log.getTimestamp()))).append(" (").append(log.getTimeOfDay()).append(")\n");
        sb.append("----------------------------------------\n");

        // Kohlenhydrate & Mahlzeitenbolus
        sb.append("🍞 Kohlenhydrate: ").append(log.getCarbGrams()).append(" g");
        if (log.getBeValue() > 0 || log.getKeValue() > 0) {
            sb.append(" (").append(log.getBeValue()).append(" BE / ").append(log.getKeValue()).append(" KE)");
        }
        sb.append("\n");
        sb.append("⏱️ Faktor (").append(log.getTimeOfDay()).append("): ").append(log.getInsulinFactor()).append(" IE/KE\n");
        sb.append("🍽️ Mahlzeiten-Bolus: ").append(log.getMealInsulin()).append(" IE\n");

        // Blutzucker & Korrektur
        if (log.getBloodGlucose() != null) {
            sb.append("🩸 Gemessener BZ: ").append(log.getBloodGlucose());
            if (log.getTargetGlucose() != null) {
                sb.append(" (Ziel: ").append(log.getTargetGlucose()).append(")");
            }
            sb.append("\n");

            if (log.getCorrectionFactor() != null && log.getCorrectionFactor() > 0) {
                sb.append("🎯 Korrekturfaktor: 1 IE / ").append(log.getCorrectionFactor()).append("\n");
            }

            Double corr = log.getCorrectionInsulin();
            if (corr != null && corr != 0.0) {
                sb.append("⚡ Korrektur-Bolus: ")
                        .append(corr > 0 ? "+" : "")
                        .append(corr).append(" IE\n");
            }
        }

        // Gesamtdosis
        sb.append("----------------------------------------\n");
        sb.append("💉 Gesamtdosis: ").append(log.getRoundedInsulin()).append(" IE");
        if (Math.abs(log.getTotalInsulin() - log.getRoundedInsulin()) > 0.01) {
            sb.append(" (exakt: ").append(Math.round(log.getTotalInsulin() * 100.0) / 100.0).append(" IE)");
        }
        sb.append("\n");

        // Notiz
        if (log.getNotes() != null && !log.getNotes().trim().isEmpty()) {
            sb.append("📝 Notiz: ").append(log.getNotes().trim()).append("\n");
        }

        sb.append("----------------------------------------\n");
        sb.append("ℹ️ Erstellt mit InsulinRechner");
        return sb.toString();
    }

    /**
     * Builds standard CSV file content for medical or spreadsheet export.
     */
    public String generateCsvExport(List<CalculationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return "ID;Datum_Uhrzeit;Tageszeit;Mahlzeit;KH_Gramm;BE;KE;Faktor;Mahlzeiten_Insulin;Blutzucker;Ziel_BZ;Korrektur_Faktor;Korrektur_Insulin;Gesamt_Insulin_IE;Notizen\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID;Datum_Uhrzeit;Tageszeit;Mahlzeit;KH_Gramm;BE;KE;Faktor;Mahlzeiten_Insulin;Blutzucker;Ziel_BZ;Korrektur_Faktor;Korrektur_Insulin;Gesamt_Insulin_IE;Notizen\n");

        for (CalculationLog log : logs) {
            sb.append(log.getId()).append(";")
                    .append(csvDateFormat.format(new Date(log.getTimestamp()))).append(";")
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

    public record LogbookMetrics(int totalEntries, double totalCarbsGrams, double totalInsulinUnits,
                                 Double averageBloodGlucose) {
    }

    public LogbookMetrics calculateMetrics(List<CalculationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return new LogbookMetrics(0, 0.0, 0.0, null);
        }
        double totalCarbs = 0;
        double totalInsulin = 0;
        double bgSum = 0;
        int bgCount = 0;

        for (CalculationLog log : logs) {
            totalCarbs += log.getCarbGrams();
            totalInsulin += log.getRoundedInsulin();
            if (log.getBloodGlucose() != null) {
                bgSum += log.getBloodGlucose();
                bgCount++;
            }
        }
        Double avgBg = bgCount > 0 ? (bgSum / bgCount) : null;
        return new LogbookMetrics(logs.size(), totalCarbs, totalInsulin, avgBg);
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
