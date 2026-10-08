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

public enum TimeOfDay {
    MORNING(
            R.string.time_of_day_morning_title,
            R.string.time_of_day_morning_subtitle,
            1.50,
            6.0,
            10.5,
            Brightness5Kt.getBrightness5(Icons.Filled.INSTANCE),
            0xFFFF9800L
    ),
    NOON(
            R.string.time_of_day_noon_title,
            R.string.time_of_day_noon_subtitle,
            1.00,
            10.5,
            16.0,
            WbSunnyKt.getWbSunny(Icons.Filled.INSTANCE),
            0xFF009688L
    ),
    EVENING(
            R.string.time_of_day_evening_title,
            R.string.time_of_day_evening_subtitle,
            1.20,
            16.0,
            22.0,
            WbTwilightKt.getWbTwilight(Icons.Filled.INSTANCE),
            0xFF3F51B5L
    ),
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

    public int getTitleResId() {
        return titleResId;
    }

    public int getSubtitleResId() {
        return subtitleResId;
    }

    public double getDefaultFactor() {
        return defaultFactor;
    }

    public double getStartHour() {
        return startHour;
    }

    public double getEndHour() {
        return endHour;
    }

    public ImageVector getIcon() {
        return icon;
    }

    public long getColorValue() {
        return colorValue;
    }

    public static TimeOfDay fromHour(double timeInHours) {
        if (timeInHours >= 6.0 && timeInHours < 10.5) {
            return MORNING;
        } else if (timeInHours >= 10.5 && timeInHours < 16.0) {
            return NOON;
        } else if (timeInHours >= 16.0 && timeInHours < 22.0) {
            return EVENING;
        } else {
            return NIGHT;
        }
    }

    public static TimeOfDay fromHour(int hour) {
        return fromHour((double) hour);
    }

    public static TimeOfDay fromTime(int hour, int minute) {
        return fromHour(hour + (minute / 60.0));
    }

    public static TimeOfDay current() {
        Calendar cal = Calendar.getInstance();
        return fromTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }
}
