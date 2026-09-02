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

import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.BedtimeKt;
import androidx.compose.material.icons.filled.Brightness5Kt;
import androidx.compose.material.icons.filled.WbSunnyKt;
import androidx.compose.material.icons.filled.WbTwilightKt;
import androidx.compose.ui.graphics.vector.ImageVector;

import java.util.Calendar;

public enum TimeOfDay {
    MORNING(
            "Morgens",
            "Frühstück (06:00 - 10:30)",
            1.50,
            6.0,
            10.5,
            Brightness5Kt.getBrightness5(Icons.Filled.INSTANCE),
            0xFFFF9800L
    ),
    NOON(
            "Mittags",
            "Mittagessen (10:30 - 16:00)",
            1.00,
            10.5,
            16.0,
            WbSunnyKt.getWbSunny(Icons.Filled.INSTANCE),
            0xFF009688L
    ),
    EVENING(
            "Abends",
            "Abendessen (16:00 - 22:00)",
            1.20,
            16.0,
            22.0,
            WbTwilightKt.getWbTwilight(Icons.Filled.INSTANCE),
            0xFF3F51B5L
    ),
    NIGHT(
            "Nachts",
            "Spätmahlzeit (22:00 - 06:00)",
            0.80,
            22.0,
            6.0,
            BedtimeKt.getBedtime(Icons.Filled.INSTANCE),
            0xFF673AB7L
    );

    private final String title;
    private final String subtitle;
    private final double defaultFactor;
    private final double startHour;
    private final double endHour;
    private final ImageVector icon;
    private final long colorValue;

    TimeOfDay(
            String title,
            String subtitle,
            double defaultFactor,
            double startHour,
            double endHour,
            ImageVector icon,
            long colorValue
    ) {
        this.title = title;
        this.subtitle = subtitle;
        this.defaultFactor = defaultFactor;
        this.startHour = startHour;
        this.endHour = endHour;
        this.icon = icon;
        this.colorValue = colorValue;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
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
