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
 * Pure Java utility for generating formatted text shares, CSV export structures, and statistical summary metrics
 * from diabetic calculation history logs.
 * <p>
 * Handles locale-aware date formatting and constructs clean, emoji-supported text blocks suitable for sharing
 * via email, messaging apps, or exporting logs for clinical consultation.
 */
public class LogbookExportHelper {

    /**
     * Date and time formatter initialized with locale-aware display patterns.
     */
    private final SimpleDateFormat dateFormat;

    /**
     * Android application or activity context used to resolve localized string resources.
     */
    private final Context context;

    /**
     * Constructs a new {@link LogbookExportHelper} using the default system locale and given Android Context.
     *
     * @param context Android context for resolving localized string resources.
     */
    public LogbookExportHelper(Context context) {
        this(Locale.getDefault(), context);
    }

    /**
     * Constructs a new {@link LogbookExportHelper} with a specific {@link Locale} and Android Context.
     *
     * @param locale  The target locale for date formatting and number output.
     * @param context Android context for resolving localized string resources.
     */
    public LogbookExportHelper(Locale locale, Context context) {
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
        this.dateFormat = new SimpleDateFormat(DateTimeUtils.PATTERN_DISPLAY_DATETIME, effectiveLocale);
        this.context = context;
    }

    /**
     * Resolves a localized string resource using the stored Android context.
     * Returns an empty string if context is null.
     *
     * @param resId      String resource ID to fetch.
     * @param formatArgs Formatting arguments to apply to the string template.
     * @return Localized formatted string, or empty string if context is missing.
     */
    private String getString(int resId, Object... formatArgs) {
        if (context != null) {
            return context.getString(resId, formatArgs);
        }
        return "";
    }

    /**
     * Formats a complete export report of all or filtered logs for text sharing (e.g. via Email or Messenger).
     * Calculates summary metrics including total carbohydrates consumed, total insulin administered,
     * and average blood glucose over the selected period.
     *
     * @param logs              List of {@link CalculationLog} entries to include in the report.
     * @param filterDescription Human-readable description of the active time frame filter (e.g., "Letzte 7 Tage").
     * @param glucoseUnit       Active blood glucose unit (mg/dL or mmol/L) for formatting glucose values.
     * @return Multi-line formatted summary string ready for clipboard or sharing intents.
     */
    public String generateExportText(List<CalculationLog> logs, String filterDescription, GlucoseUnit glucoseUnit) {
        if (logs == null || logs.isEmpty()) {
            return getString(R.string.logbook_export_helper_no_entries_for_period, filterDescription);
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
     * Formats a single calculation log entry into a detailed shareable text snippet with full breakdowns,
     * meal bolus, correction bolus, blood glucose readings, and application attribution footer.
     *
     * @param log         The {@link CalculationLog} to format for sharing.
     * @param glucoseUnit Active blood glucose unit (mg/dL or mmol/L) for unit display.
     * @return Formatted single-entry share string, or empty string if log is null.
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
     * Appends a single calculation log entry to the target {@link StringBuilder} using standardized formatting.
     * Formats date/time, carbohydrates, insulin factors, meal boluses, blood glucose readings, corrections, and notes.
     *
     * @param sb          Target {@link StringBuilder} to append formatted content to.
     * @param log         The {@link CalculationLog} item to format.
     * @param glucoseUnit Active blood glucose unit for localized unit strings.
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
