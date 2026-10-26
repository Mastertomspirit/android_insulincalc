package network.spiritscorp.model;

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

import androidx.annotation.StringRes;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.BedtimeKt;
import androidx.compose.material.icons.filled.Brightness5Kt;
import androidx.compose.material.icons.filled.WbSunnyKt;
import androidx.compose.material.icons.filled.WbTwilightKt;
import androidx.compose.ui.graphics.vector.ImageVector;

import java.util.Calendar;
import java.util.Locale;

import network.spiritscorp.R;

/**
 * Enumeration representing distinct time-of-day categories for diabetic insulin calculations,
 * time-specific factor lookup, UI presentation, and automatic daytime detection based on system clock.
 * <p>
 * Each constant defines localized string resource identifiers, default insulin factors, starting/ending hour bounds,
 * material vector icons, and associated theme color values.
 */
public enum TimeOfDay {

    /**
     * Morning / Breakfast timeframe (06:00 - 10:30).
     * Represents the breakfast period where insulin sensitivity typically varies due to the dawn phenomenon.
     */
    MORNING(
            R.string.time_of_day_morning_title,
            R.string.time_of_day_morning_subtitle,
            1.50,
            6.0,
            10.5,
            Brightness5Kt.getBrightness5(Icons.Filled.INSTANCE),
            0xFFFF9800L
    ),

    /**
     * Noon / Lunch timeframe (10:30 - 16:00).
     * Represents the midday period with standard baseline insulin sensitivity.
     */
    NOON(
            R.string.time_of_day_noon_title,
            R.string.time_of_day_noon_subtitle,
            1.00,
            10.5,
            16.0,
            WbSunnyKt.getWbSunny(Icons.Filled.INSTANCE),
            0xFF009688L
    ),

    /**
     * Evening / Dinner timeframe (16:00 - 22:00).
     * Represents the evening period leading into nighttime hours.
     */
    EVENING(
            R.string.time_of_day_evening_title,
            R.string.time_of_day_evening_subtitle,
            1.20,
            16.0,
            22.0,
            WbTwilightKt.getWbTwilight(Icons.Filled.INSTANCE),
            0xFF3F51B5L
    ),

    /**
     * Night / Bedtime timeframe (22:00 - 06:00).
     * Represents the overnight period requiring cautious dosing to prevent nocturnal hypoglycemia.
     */
    NIGHT(
            R.string.time_of_day_night_title,
            R.string.time_of_day_night_subtitle,
            0.80,
            22.0,
            6.0,
            BedtimeKt.getBedtime(Icons.Filled.INSTANCE),
            0xFF673AB7L
    );

    @StringRes private final int titleResId;
    @StringRes private final int subtitleResId;
    private final double defaultFactor;
    private final double startHour;
    private final double endHour;
    private final ImageVector icon;
    private final long colorValue;

    /**
     * Constructs a TimeOfDay enum constant with its full layout and calculation properties.
     *
     * @param titleResId    String resource ID for the time-of-day title.
     * @param subtitleResId String resource ID for the time-of-day subtitle.
     * @param defaultFactor Baseline default insulin factor (IE per KE/BE).
     * @param startHour     Starting hour in decimal format (0.0 to 24.0).
     * @param endHour       Ending hour in decimal format (0.0 to 24.0).
     * @param icon          Jetpack Compose Material vector icon representing the time period.
     * @param colorValue    32-bit ARGB color value associated with this time period for UI styling.
     */
    TimeOfDay(
            @StringRes int titleResId,
            @StringRes int subtitleResId,
            double defaultFactor,
            double startHour,
            double endHour,
            ImageVector icon,
            long colorValue
    ) {
        this.titleResId = titleResId;
        this.subtitleResId = subtitleResId;
        this.defaultFactor = defaultFactor;
        this.startHour = startHour;
        this.endHour = endHour;
        this.icon = icon;
        this.colorValue = colorValue;
    }

    /**
     * Parses a string representation (English or German, case-insensitive) into a matching {@link TimeOfDay} constant.
     * Defaults to {@link #MORNING} if the string is null, blank, or unrecognized.
     *
     * @param timeOfDay The name or identifier string to parse (e.g. "NOON", "MITTAGS", "EVENING").
     * @return The corresponding {@link TimeOfDay} enum constant, or {@link #MORNING} as a fallback.
     */
    public static TimeOfDay fromString(String timeOfDay) {
        if (timeOfDay == null) return MORNING;
        String trimmed = timeOfDay.trim().toUpperCase(Locale.getDefault());
        return switch (trimmed) {
            case "MITTAGS", "NOON" -> NOON;
            case "ABENDS", "EVENING" -> EVENING;
            case "NACHTS", "NIGHT" -> NIGHT;
            default -> MORNING;
        };
    }

    /**
     * Returns the localized string resource ID for the title of this time of day.
     *
     * @return String resource ID for the title.
     */
    public int getTitleResId() {
        return titleResId;
    }

    /**
     * Returns the localized string resource ID for the subtitle/description of this time of day.
     *
     * @return String resource ID for the subtitle.
     */
    public int getSubtitleResId() {
        return subtitleResId;
    }

    /**
     * Returns the default baseline insulin factor (IE per carb unit) for this time period.
     *
     * @return Default insulin factor.
     */
    public double getDefaultFactor() {
        return defaultFactor;
    }

    /**
     * Returns the start time of this period as a decimal hour (e.g. 10.5 for 10:30 AM).
     *
     * @return Start hour in decimal format.
     */
    public double getStartHour() {
        return startHour;
    }

    /**
     * Returns the end time of this period as a decimal hour (e.g. 16.0 for 04:00 PM).
     *
     * @return End hour in decimal format.
     */
    public double getEndHour() {
        return endHour;
    }

    /**
     * Returns the Compose Material vector icon representing this time of day.
     *
     * @return ImageVector graphic icon.
     */
    public ImageVector getIcon() {
        return icon;
    }

    /**
     * Returns the 32-bit ARGB color value associated with this time category.
     *
     * @return Long color value in ARGB format.
     */
    public long getColorValue() {
        return colorValue;
    }

    /**
     * Determines the appropriate {@link TimeOfDay} category for a given time of day specified as decimal hours.
     *
     * @param timeInHours Time in decimal hours (e.g. 14.5 for 14:30).
     * @return Corresponding {@link TimeOfDay} enum constant.
     */
    public static TimeOfDay fromHour(double timeInHours) {
        if (timeInHours >= MORNING.startHour && timeInHours < MORNING.endHour) {
            return MORNING;
        } else if (timeInHours >= NOON.startHour && timeInHours < NOON.endHour) {
            return NOON;
        } else if (timeInHours >= EVENING.startHour && timeInHours < EVENING.endHour) {
            return EVENING;
        } else {
            return NIGHT;
        }
    }

    /**
     * Determines the appropriate {@link TimeOfDay} category for a given hour and minute.
     *
     * @param hour   Hour of day (0 to 23).
     * @param minute Minute of hour (0 to 59).
     * @return Corresponding {@link TimeOfDay} enum constant.
     */
    public static TimeOfDay fromTime(int hour, int minute) {
        return fromHour(hour + (minute / 60.0));
    }

    /**
     * Returns the current {@link TimeOfDay} category based on the device's system clock.
     *
     * @return The active {@link TimeOfDay} constant for the current time.
     */
    public static TimeOfDay current() {
        Calendar cal = Calendar.getInstance();
        return fromTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }
}
