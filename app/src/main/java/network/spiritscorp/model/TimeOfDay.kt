package network.spiritscorp.model

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
        fun current(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 6..10 -> MORNING
                in 11..16 -> NOON
                in 17..21 -> EVENING
                else -> NIGHT
            }
        }
    }
}
