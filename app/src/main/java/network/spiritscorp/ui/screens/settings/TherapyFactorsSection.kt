package network.spiritscorp.ui.screens.settings

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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import network.spiritscorp.ui.theme.EveningColor
import network.spiritscorp.ui.theme.MorningColor
import network.spiritscorp.ui.theme.NightColor
import network.spiritscorp.ui.theme.NoonColor

@Composable
fun TherapyFactorsSection(
    morningFactor: Double,
    onMorningFactorChange: (Double) -> Unit,
    noonFactor: Double,
    onNoonFactorChange: (Double) -> Unit,
    eveningFactor: Double,
    onEveningFactorChange: (Double) -> Unit,
    nightFactor: Double,
    onNightFactorChange: (Double) -> Unit,
    roundingStep: Double,
    onRoundingStepChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_factors_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSectionHeader(
                icon = Icons.Default.Tune,
                title = "1. Mahlzeiten-Faktoren (ICR)",
                subtitle = "Insulin-zu-Kohlenhydrat-Faktoren je Tageszeit",
                isExpanded = isExpanded,
                onToggle = onToggleExpand
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    FactorRow(
                        title = "Morgens (06:00 - 10:30)",
                        subtitle = "Höherer Insulinbedarf wegen Dawn-Phänomen",
                        icon = Icons.Default.WbTwilight,
                        iconColor = MorningColor,
                        factor = morningFactor,
                        onFactorChange = onMorningFactorChange,
                        tagPrefix = "morning"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FactorRow(
                        title = "Mittags (10:30 - 16:00)",
                        subtitle = "Typischerweise geringster Faktor des Tages",
                        icon = Icons.Default.WbSunny,
                        iconColor = NoonColor,
                        factor = noonFactor,
                        onFactorChange = onNoonFactorChange,
                        tagPrefix = "noon"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FactorRow(
                        title = "Abends (16:00 - 22:00)",
                        subtitle = "Mittlerer bis leicht erhöhter Bedarf",
                        icon = Icons.Default.Brightness5,
                        iconColor = EveningColor,
                        factor = eveningFactor,
                        onFactorChange = onEveningFactorChange,
                        tagPrefix = "evening"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FactorRow(
                        title = "Nachts / Spät (22:00 - 06:00)",
                        subtitle = "Vorsichtige Dosierung bei Spätmahlzeiten",
                        icon = Icons.Default.Bedtime,
                        iconColor = NightColor,
                        factor = nightFactor,
                        onFactorChange = onNightFactorChange,
                        tagPrefix = "night"
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Rundung der Insulindosis:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val roundingOptions = listOf(
                        0.5 to "Halbe Einheiten (0.5 IE) – Standard für Pen",
                        0.1 to "Zehntel Einheiten (0.1 IE) – für Insulinpumpe",
                        1.0 to "Ganze Einheiten (1.0 IE)"
                    )

                    roundingOptions.forEach { (step, label) ->
                        val isStepSelected = roundingStep == step
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRoundingStepChange(step) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isStepSelected,
                                onClick = { onRoundingStepChange(step) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isStepSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
