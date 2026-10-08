package network.spiritscorp.ui.screens.calculator

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import network.spiritscorp.R
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.ui.theme.AlertRed
import network.spiritscorp.viewmodel.CalculatorUiState

/**
 * Expandable card allowing users to enter current blood glucose, target value,
 * and individual correction factor to calculate correction bolus doses.
 */
@Composable
fun GlucoseCorrectionCard(
    uiState: CalculatorUiState,
    onToggleCorrection: () -> Unit,
    onGlucoseInputChange: (String) -> Unit,
    onTargetGlucoseChange: (String) -> Unit,
    onCorrectionFactorChange: (String) -> Unit,
    onDoneKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("glucose_correction_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleCorrection() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.glucose_correction_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.showCorrection) stringResource(R.string.glucose_correction_subtitle_active) else stringResource(R.string.glucose_correction_subtitle_tap),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (uiState.showCorrection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = uiState.showCorrection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current Glucose
                        OutlinedTextField(
                            value = uiState.currentGlucoseInput,
                            onValueChange = onGlucoseInputChange,
                            label = { Text(stringResource(R.string.glucose_correction_current_label, stringResource(uiState.glucoseUnit.shortNameResId))) },
                            placeholder = {
                                Text(
                                    if (uiState.glucoseUnit == GlucoseUnit.MMOL_L) stringResource(R.string.glucose_correction_mmol_placeholder) else stringResource(R.string.glucose_correction_mgdl_placeholder)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("current_glucose_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Target Glucose
                        OutlinedTextField(
                            value = uiState.targetGlucoseInput,
                            onValueChange = onTargetGlucoseChange,
                            label = { Text(stringResource(R.string.glucose_correction_target_label, stringResource(uiState.glucoseUnit.shortNameResId))) },
                            placeholder = {
                                Text(
                                    if (uiState.glucoseUnit == GlucoseUnit.MMOL_L) stringResource(R.string.glucose_correction_target_mmol_placeholder) else stringResource(R.string.glucose_correction_target_mgdl_placeholder)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("target_glucose_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Correction Factor Input
                    OutlinedTextField(
                        value = uiState.correctionFactorInput,
                        onValueChange = onCorrectionFactorChange,
                        label = { Text(stringResource(R.string.glucose_correction_factor_label, stringResource(uiState.glucoseUnit.shortNameResId))) },
                        placeholder = {
                            Text(
                                if (uiState.glucoseUnit == GlucoseUnit.MMOL_L) stringResource(R.string.glucose_correction_factor_mmol_placeholder) else stringResource(R.string.glucose_correction_factor_mgdl_placeholder)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onDoneKeyboard() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("correction_factor_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (uiState.calculationSummary.correctionInsulin() != 0.0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.calculationSummary.correctionInsulin() > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.calculationSummary.correctionInsulin() > 0) Icons.Default.Add else Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (uiState.calculationSummary.correctionInsulin() > 0) MaterialTheme.colorScheme.onSecondaryContainer else AlertRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        R.string.glucose_correction_dose_format,
                                        if (uiState.calculationSummary.correctionInsulin() > 0) "+" else "",
                                        uiState.calculationSummary.correctionInsulin()
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.calculationSummary.correctionInsulin() > 0) MaterialTheme.colorScheme.onSecondaryContainer else AlertRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
