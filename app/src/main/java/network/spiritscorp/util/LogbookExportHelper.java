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

import android.content.Context;
import network.spiritscorp.R;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.GlucoseUnit;

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
    private final Context context;

    /**
     * Constructs a new LogbookExportHelper with a Context for string resource resolution.
     */
    public LogbookExportHelper(Context context) {
        this(Locale.getDefault(), context);
    }

    public LogbookExportHelper(Locale locale, Context context) {
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
        this.dateFormat = new SimpleDateFormat(DateTimeUtils.PATTERN_DISPLAY_DATETIME, effectiveLocale);
        this.context = context;
    }

    private String getString(int resId, Object... formatArgs) {
        if (context != null) {
            return context.getString(resId, formatArgs);
        }
        return "";
    }

    /**
     * Formats a complete export report of all or filtered logs for text sharing (e.g. Email/Messenger).
     */
    public String generateExportText(List<CalculationLog> logs, String filterDescription, GlucoseUnit glucoseUnit) {
        if (logs == null || logs.isEmpty()) {
            return  getString(R.string.logbook_export_helper_no_entries_for_period, filterDescription);
        }

        double totalCarbs = 0.0;
        double totalInsulin = 0.0;
        double bgSum = 0.0;
        int bgCount = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("📋 ").append(getString(R.string.logbook_export_helper_title_header)).append("\n");
        sb.append(getString(R.string.logbook_export_helper_period_label, filterDescription)).append("\n");
        sb.append(getString(R.string.logbook_export_helper_created_label, dateFormat.format(new Date()))).append("\n");
        sb.append("----------------------------------------\n\n");

        for (CalculationLog log : logs) {
            totalCarbs += log.getCarbGrams();
            totalInsulin += log.getRoundedInsulin();
            if (log.getBloodGlucose() != null) {
                bgSum += log.getBloodGlucose();
                bgCount++;
            }
            appendLogEntry(sb, log, glucoseUnit);
            sb.append("\n");
        }

        sb.append("----------------------------------------\n");
        sb.append("📊 ").append(getString(R.string.logbook_export_helper_summary_title)).append("\n");
        sb.append("• ").append(getString(R.string.logbook_export_helper_entries_label)).append(": ").append(logs.size()).append("\n");
        sb.append("• ").append(getString(R.string.logbook_export_helper_total_carbs_label)).append(": ").append(Math.round(totalCarbs)).append(" g\n");
        sb.append("• ").append(getString(R.string.logbook_export_helper_total_insulin_label)).append(": ").append(Math.round(totalInsulin * 10.0) / 10.0).append(" IE\n");
        if (bgCount > 0) {
            double avgBg = Math.round((bgSum / bgCount) * 10.0) / 10.0;
            sb.append("• ").append(getString(R.string.logbook_export_helper_avg_bg_label)).append(": ").append(avgBg).append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats a single log entry with full details, breakdown, correction bolus and symbols for sharing.
     */
    public String formatSingleLogShare(CalculationLog log, GlucoseUnit glucoseUnit) {
        if (log == null) return "";
        StringBuilder sb = new StringBuilder();
        appendLogEntry(sb, log, glucoseUnit);
        sb.append("----------------------------------------\n");
        sb.append("ℹ️ ").append(getString(R.string.logbook_export_helper_created_with));
        return sb.toString();
    }

    /**
     * Appends one calculation log using the common format shared by the single-log view and the complete export.
     */
    private void appendLogEntry(StringBuilder sb, CalculationLog log, GlucoseUnit glucoseUnit) {
        sb.append("📋 ").append(getString(R.string.logbook_export_helper_calculation_prefix)).append(": ").append(log.getMealTitle()).append("\n")

            .append("📅 ")
            .append(dateFormat.format(new Date(log.getTimestamp())))
            .append(" (").append(log.getTimeOfDay()).append(")\n")

            .append("----------------------------------------\n")

            // Carbohydrates and meal bolus
            .append("🍞 ").append(getString(R.string.logbook_export_helper_carbs_prefix)).append(": ").append(log.getCarbGrams()).append(" g");

        if (log.getBeValue() > 0 || log.getKeValue() > 0) {
            sb.append(" (").append(log.getBeValue()).append(" BE / ").append(log.getKeValue()).append(" KE)");
        }

        // Insulin factor and meal bolus
        sb.append("\n")
            .append("⏱️ ").append(getString(R.string.logbook_export_helper_factor_prefix)).append(" (").append(getString(log.getTimeOfDay().getTitleResId())).append("): ").append(log.getInsulinFactor()).append(" IE/KE\n")
            .append("🍽️ ").append(getString(R.string.logbook_export_helper_meal_bolus_prefix)).append(": ").append(log.getMealInsulin()).append(" IE\n");

        // Blood glucose and correction
        if (log.getBloodGlucose() != null) {
            sb.append("🩸 ").append(getString(R.string.logbook_export_helper_measured_bg_label)).append(": ").append(log.getBloodGlucose()).append(" ").append(getString(glucoseUnit.getShortNameResId()));

            if (log.getTargetGlucose() != null) {
                sb.append(" (").append(getString(R.string.logbook_export_helper_target_bg_prefix)).append(": ").append(log.getTargetGlucose()).append(" ").append(getString(glucoseUnit.getShortNameResId())).append(")");
            }

            sb.append("\n");

            if (log.getCorrectionFactor() != null && log.getCorrectionFactor() > 0) {
                sb.append("🎯 ").append(getString(R.string.logbook_export_helper_correction_factor_label)).append(" ").append(log.getCorrectionFactor()).append("\n");
            }

            Double corr = log.getCorrectionInsulin();
            if (corr != null && corr != 0.0) {
                sb.append("⚡ ").append(getString(R.string.logbook_export_helper_correction_prefix)).append(": ").append(corr > 0 ? "+" : "").append(corr).append(" IE\n");
            }
        }

        // Total insulin dose
        sb.append("----------------------------------------\n");
        sb.append("💉 ").append(getString(R.string.logbook_export_helper_total_dose_prefix)).append(": ").append(log.getRoundedInsulin()).append(" IE");

        if (Math.abs(log.getTotalInsulin() - log.getRoundedInsulin()) > 0.01) {
            sb.append(" (").append(getString(R.string.logbook_export_helper_exact_dose_prefix)).append(": ").append(Math.round(log.getTotalInsulin() * 100.0) / 100.0).append(" IE)");
        }

        sb.append("\n");

        // Note
        if (log.getNotes() != null && !log.getNotes().trim().isEmpty()) {
            sb.append("📝 ").append(getString(R.string.logbook_export_helper_note_prefix)).append(": ").append(log.getNotes().trim()).append("\n");
        }
    }
}
