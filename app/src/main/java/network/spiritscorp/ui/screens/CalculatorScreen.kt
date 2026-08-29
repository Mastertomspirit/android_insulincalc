package network.spiritscorp.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.ui.components.MedicalDisclaimerBanner
import network.spiritscorp.ui.components.TimeOfDaySelector
import network.spiritscorp.ui.components.UnitSelectorRow
import network.spiritscorp.ui.theme.AlertRed
import network.spiritscorp.viewmodel.CalculatorUiState
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: InsulinCalculatorViewModel,
    uiState: CalculatorUiState,
    onNavigateToAiEstimator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val settings by viewModel.userSettings.collectAsState(initial = null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Medical Disclaimer
        if (settings?.showDisclaimer == true) {
            MedicalDisclaimerBanner(
                onDismiss = { viewModel.dismissDisclaimer() }
            )
        }

        // 1. RESULT CARD (Big Hero Result Display & Save to Logbook)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insulin_result_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EMPFOHLENE INSULINDOSIS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Big rounded insulin units
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format(LocalLocale.current.platformLocale, "%.1f", uiState.calculationSummary.roundedTotalInsulin),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 58.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IE",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.calculationSummary.roundingStep > 0.0) {
                    Text(
                        text = "Gerundet auf ${uiState.calculationSummary.roundingStep} IE (Exakt: ${uiState.calculationSummary.rawTotalInsulin} IE)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // Detailed breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Mahlzeit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                        Text(
                            text = "${uiState.calculationSummary.mealInsulin} IE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${uiState.calculationSummary.carbGrams}g × ${uiState.calculationSummary.factorUsed}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                        )
                    }

                    if (uiState.showCorrection && uiState.calculationSummary.correctionInsulin != 0.0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Korrektur",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${if (uiState.calculationSummary.correctionInsulin > 0) "+" else ""}${uiState.calculationSummary.correctionInsulin} IE",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "BZ ${uiState.currentGlucoseInput} mg/dl",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tageszeit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                        Text(
                            text = uiState.selectedTimeOfDay.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Faktor: ${uiState.calculationSummary.factorUsed}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                        )
                    }
                }

                if (uiState.calculationSummary.isHypoRisk) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Hypoglykämie-Gefahr",
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.calculationSummary.advisoryNote,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action: Save to logbook button
                Button(
                    onClick = { viewModel.saveCalculationToLog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_to_logbook_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Im Tagebuch speichern",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 2. Carbohydrate Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("carb_input_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kohlenhydrate",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Button to open AI Assistant
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateToAiEstimator() }
                            .testTag("open_ai_estimator_button"),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "KI Schätzer",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "KI-Schätzer",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Large Input Box (now on top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.carbInput,
                        onValueChange = { viewModel.onCarbInputChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("carb_input_field"),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        placeholder = {
                            Text(
                                "0",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        trailingIcon = {
                            if (uiState.carbInput.isNotEmpty() && uiState.carbInput != "0") {
                                IconButton(
                                    onClick = { viewModel.clearCarbs() },
                                    modifier = Modifier.testTag("clear_carb_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Löschen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Unit selection (g KH, KE, BE) below input
                UnitSelectorRow(
                    selectedUnit = uiState.selectedUnit,
                    onUnitSelect = { viewModel.setUnit(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Add Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stepAmounts = when (uiState.selectedUnit) {
                        CarbUnit.GRAMS -> listOf(5.0, 10.0, 20.0, 50.0)
                        CarbUnit.KE -> listOf(0.5, 1.0, 2.0, 5.0)
                        CarbUnit.BE -> listOf(0.5, 1.0, 2.0, 5.0)
                    }

                    stepAmounts.forEach { step ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.addCarbs(step) }
                                .testTag("quick_add_${step}"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "+${if (step % 1.0 == 0.0) step.toInt().toString() else step.toString()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live equivalent converter badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.calculationSummary.carbGrams} g KH",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${uiState.calculationSummary.keValue} KE",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${uiState.calculationSummary.beValue} BE",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Time of Day Selector & Active Factor (Tageszeit)
        TimeOfDaySelector(
            selectedTimeOfDay = uiState.selectedTimeOfDay,
            effectiveFactor = uiState.calculationSummary.factorUsed,
            isAutoDetected = uiState.isAutoTimeDetection,
            onSelect = { viewModel.selectTimeOfDay(it) },
            onAdjustFactor = { viewModel.adjustFactor(it) },
            onResetAuto = { viewModel.resetToAutoTime() }
        )

        // 4. Expandable Correction Bolus (Blutzucker-Korrektur) Card
        Card(
            modifier = Modifier
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
                        .clickable { viewModel.toggleCorrection() }
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
                                text = "Blutzucker-Korrektur (Optional)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.showCorrection) "Korrektur-Bolus aktiviert" else "Tippe zum Hinzufügen von BZ-Korrektur",
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Current Glucose
                            OutlinedTextField(
                                value = uiState.currentGlucoseInput,
                                onValueChange = { viewModel.onGlucoseInputChange(it) },
                                label = { Text("Aktueller BZ (${uiState.glucoseUnit.shortName})") },
                                placeholder = {
                                    Text(
                                        if (uiState.glucoseUnit == GlucoseUnit.MMOL_L) "z.B. 9.5" else "z.B. 160"
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("current_glucose_field"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Target Glucose
                            OutlinedTextField(
                                value = uiState.targetGlucoseInput,
                                onValueChange = { viewModel.onTargetGlucoseChange(it) },
                                label = { Text("Zielwert (${uiState.glucoseUnit.shortName})") },
                                placeholder = {
                                    Text(
                                        if (uiState.glucoseUnit == GlucoseUnit.MMOL_L) "z.B. 6.7" else "z.B. 120"
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("target_glucose_field"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Correction Factor Input
                        OutlinedTextField(
                            value = uiState.correctionFactorInput,
                            onValueChange = { viewModel.onCorrectionFactorChange(it) },
                            label = { Text("Korrekturfaktor (${uiState.glucoseUnit.shortName} pro 1 IE)") },
                            placeholder = {
                                Text(
                                    if (uiState.glucoseUnit == GlucoseUnit.MMOL_L) "z.B. 2.8" else "z.B. 50"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("correction_factor_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (uiState.calculationSummary.correctionInsulin != 0.0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.calculationSummary.correctionInsulin > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.calculationSummary.correctionInsulin > 0) Icons.Default.Add else Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (uiState.calculationSummary.correctionInsulin > 0) MaterialTheme.colorScheme.onSecondaryContainer else AlertRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Korrektur-Dosis: ${if (uiState.calculationSummary.correctionInsulin > 0) "+" else ""}${uiState.calculationSummary.correctionInsulin} IE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (uiState.calculationSummary.correctionInsulin > 0) MaterialTheme.colorScheme.onSecondaryContainer else AlertRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Expandable Meal Title & Notes Card (Mahlzeit & Notizen Dropdown)
        var isMealNotesExpanded by remember { mutableStateOf(uiState.mealTitle.isNotBlank() || uiState.notes.isNotBlank()) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("meal_notes_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isMealNotesExpanded = !isMealNotesExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mahlzeit & Notizen (Optional)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.mealTitle.isNotBlank()) uiState.mealTitle else if (isMealNotesExpanded) "Details eingeblendet" else "Tippe für Bezeichnung & Notizen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isMealNotesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isMealNotesExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Meal Title Input Field
                        OutlinedTextField(
                            value = uiState.mealTitle,
                            onValueChange = { viewModel.onMealTitleChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meal_title_input_field"),
                            label = { Text("Mahlzeit / Bezeichnung") },
                            placeholder = { Text("z.B. Frühstück, Spaghetti Bolognese, Snack...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.mealTitle.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onMealTitleChange("") },
                                        modifier = Modifier.testTag("clear_meal_title_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Löschen",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Notes Input Field
                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = { viewModel.onNotesChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meal_notes_input_field"),
                            label = { Text("Notizen / Bemerkungen") },
                            placeholder = { Text("z.B. Sport vor Mahlzeit, Restaurant, Sensorwert...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.notes.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onNotesChange("") },
                                        modifier = Modifier.testTag("clear_notes_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Löschen",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            singleLine = false
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
