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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import network.spiritscorp.R
import network.spiritscorp.ui.theme.EveningColor
import network.spiritscorp.ui.theme.MorningColor
import network.spiritscorp.ui.theme.NightColor
import network.spiritscorp.ui.theme.NoonColor

/**
 * Settings composable for therapy factors (morning, noon, evening, night, and rounding step).
 *
 * Displays the current factors and allows the user to edit them. Includes a collapsible
 * expanded section that shows the rounding step options (0.5, 0.1, or 1.0). Uses colored
 * icons for morning (twilight), noon (sunny), evening (darkness), and night (bedtime) to
 * visually distinguish each factor type.
 *
 * @param morningFactor The current morning factor value.
 * @param onMorningFactorChange Callback when the morning factor value is changed.
 * @param noonFactor The current noon factor value.
 * @param onNoonFactorChange Callback when the noon factor value is changed.
 * @param eveningFactor The current evening factor value.
 * @param onEveningFactorChange Callback when the evening factor value is changed.
 * @param nightFactor The current night factor value.
 * @param onNightFactorChange Callback when the night factor value is changed.
 * @param roundingStep The current rounding step value (0.5, 0.1, or 1.0).
 * @param onRoundingStepChange Callback when the rounding step value is changed.
 * @param modifier Optional modifier for layout.
 * @param isExpanded True if the expanded settings section should be visible.
 * @param onToggleExpand Callback to toggle the expand state of the settings section.
 */
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
            // Accordion Section Header: Displays title, icon and toggle for expanding/collapsing settings
            SettingsSectionHeader(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.settings_therapy_factors_section_title),
                subtitle = stringResource(R.string.settings_therapy_factors_section_subtitle),
                isExpanded = isExpanded,
                onToggle = onToggleExpand
            )

            // Animated visibility controls whether the expanded settings section is shown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Divider and spacer above factor rows
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Morning factor row
                    FactorRow(
                        title = stringResource(R.string.settings_therapy_factors_morning_title),
                        subtitle = stringResource(R.string.settings_therapy_factors_morning_subtitle),
                        icon = Icons.Default.WbTwilight,
                        iconColor = MorningColor,
                        factor = morningFactor,
                        onFactorChange = onMorningFactorChange,
                        tagPrefix = "morning"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Noon factor row
                    FactorRow(
                        title = stringResource(R.string.settings_therapy_factors_noon_title),
                        subtitle = stringResource(R.string.settings_therapy_factors_noon_subtitle),
                        icon = Icons.Default.WbSunny,
                        iconColor = NoonColor,
                        factor = noonFactor,
                        onFactorChange = onNoonFactorChange,
                        tagPrefix = "noon"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Evening factor row
                    FactorRow(
                        title = stringResource(R.string.settings_therapy_factors_evening_title),
                        subtitle = stringResource(R.string.settings_therapy_factors_evening_subtitle),
                        icon = Icons.Default.Brightness5,
                        iconColor = EveningColor,
                        factor = eveningFactor,
                        onFactorChange = onEveningFactorChange,
                        tagPrefix = "evening"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Night factor row
                    FactorRow(
                        title = stringResource(R.string.settings_therapy_factors_night_title),
                        subtitle = stringResource(R.string.settings_therapy_factors_night_subtitle),
                        icon = Icons.Default.Bedtime,
                        iconColor = NightColor,
                        factor = nightFactor,
                        onFactorChange = onNightFactorChange,
                        tagPrefix = "night"
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    // Divider and spacer between factor rows and rounding step
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Rounding step label
                    Text(
                        text = stringResource(R.string.settings_therapy_factors_rounding_label),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // List of rounding step options (0.5, 0.1, or 1.0)
                    val roundingOptions = listOf(
                        0.5 to stringResource(R.string.settings_therapy_factors_rounding_half),
                        0.1 to stringResource(R.string.settings_therapy_factors_rounding_tenth),
                        1.0 to stringResource(R.string.settings_therapy_factors_rounding_full)
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
