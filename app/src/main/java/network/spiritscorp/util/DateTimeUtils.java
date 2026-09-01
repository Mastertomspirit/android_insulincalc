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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized Date & Time patterns, formatters, and helper functions.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // Utility class - prevent instantiation
    }

    // =========================================================================
    // Standard Date/Time Patterns
    // =========================================================================

    /**
     * Standard human-readable date & time display pattern (e.g., "01.09.2026 14:30").
     */
    public static final String PATTERN_DISPLAY_DATETIME = "dd.MM.yyyy HH:mm";

    /**
     * Bullet-separated date & time display pattern (e.g., "01.09.2026 • 14:30").
     */
    public static final String PATTERN_DISPLAY_DATETIME_BULLET = "dd.MM.yyyy • HH:mm";

    /**
     * Standard German / European date-only pattern (e.g., "01.09.2026").
     */
    public static final String PATTERN_DISPLAY_DATE = "dd.MM.yyyy";

    /**
     * Standard 24-hour time-only pattern (e.g., "14:30").
     */
    public static final String PATTERN_DISPLAY_TIME = "HH:mm";

    /**
     * ISO-like standard timestamp for database/CSV interoperability (e.g., "2026-09-01 14:30:00").
     */
    public static final String PATTERN_ISO_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * Timestamp format for exported files & backups (e.g., "20260901_1430").
     */
    public static final String PATTERN_FILENAME_TIMESTAMP = "yyyyMMdd_HHmm";

    // =========================================================================
    // Formatter Factory Methods
    // =========================================================================

    public static SimpleDateFormat getDisplayDateTimeFormatter() {
        return new SimpleDateFormat(PATTERN_DISPLAY_DATETIME, Locale.getDefault());
    }

    public static SimpleDateFormat getDisplayDateTimeBulletFormatter() {
        return new SimpleDateFormat(PATTERN_DISPLAY_DATETIME_BULLET, Locale.getDefault());
    }

    public static SimpleDateFormat getDisplayDateFormatter() {
        return new SimpleDateFormat(PATTERN_DISPLAY_DATE, Locale.getDefault());
    }

    public static SimpleDateFormat getDisplayTimeFormatter() {
        return new SimpleDateFormat(PATTERN_DISPLAY_TIME, Locale.getDefault());
    }

    public static SimpleDateFormat getIsoDateTimeFormatter() {
        return new SimpleDateFormat(PATTERN_ISO_DATETIME, Locale.getDefault());
    }

    public static SimpleDateFormat getFilenameTimestampFormatter() {
        return new SimpleDateFormat(PATTERN_FILENAME_TIMESTAMP, Locale.getDefault());
    }

    // =========================================================================
    // Convenience Formatting Functions
    // =========================================================================

    public static String formatDisplayDateTime(long timestampMillis) {
        return getDisplayDateTimeFormatter().format(new Date(timestampMillis));
    }

    public static String formatDisplayDateTimeBullet(long timestampMillis) {
        return getDisplayDateTimeBulletFormatter().format(new Date(timestampMillis));
    }

    public static String formatDisplayDate(long timestampMillis) {
        return getDisplayDateFormatter().format(new Date(timestampMillis));
    }

    public static String formatDisplayTime(long timestampMillis) {
        return getDisplayTimeFormatter().format(new Date(timestampMillis));
    }

    public static String formatIsoDateTime(long timestampMillis) {
        return getIsoDateTimeFormatter().format(new Date(timestampMillis));
    }

    public static String getFilenameTimestamp() {
        return getFilenameTimestampFormatter().format(new Date());
    }

    public static String getFilenameTimestamp(Date date) {
        return getFilenameTimestampFormatter().format(date);
    }
}
