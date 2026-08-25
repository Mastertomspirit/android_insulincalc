package network.spiritscorp.model

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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import network.spiritscorp.ui.theme.EveningColor
import network.spiritscorp.ui.theme.MorningColor
import network.spiritscorp.ui.theme.NightColor
import network.spiritscorp.ui.theme.NoonColor
import java.util.Calendar

enum class TimeOfDay(
    val title: String,
    val subtitle: String,
    val defaultFactor: Double, // IE pro KE (10g)
    val startHour: Int,
    val endHour: Int,
    val icon: ImageVector,
    val accentColor: Color
) {
    MORNING("Morgens", "Frühstück (06:00 - 11:00)", 1.50, 6, 11, Icons.Default.Brightness5, MorningColor),
    NOON("Mittags", "Mittagessen (11:00 - 17:00)", 1.00, 11, 17, Icons.Default.WbSunny, NoonColor),
    EVENING("Abends", "Abendessen (17:00 - 22:00)", 1.20, 17, 22, Icons.Default.WbTwilight, EveningColor),
    NIGHT("Nachts", "Spätmahlzeit (22:00 - 06:00)", 0.80, 22, 6, Icons.Default.Bedtime, NightColor);

    companion object {
        fun fromHour(hour: Int): TimeOfDay {
            return when (hour) {
                in 6..10 -> MORNING
                in 11..16 -> NOON
                in 17..21 -> EVENING
                else -> NIGHT
            }
        }

        fun current(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return fromHour(hour)
        }
    }
}
