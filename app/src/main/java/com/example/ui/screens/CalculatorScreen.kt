package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarbUnit
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.TimeOfDaySelector
import com.example.ui.components.UnitSelectorRow
import com.example.ui.theme.AlertRed
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.InsulinCalculatorViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: InsulinCalculatorViewModel,
    uiState: CalculatorUiState,
    onNavigateToAiEstimator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showDisclaimer by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Medical Disclaimer (dismissible)
        if (showDisclaimer) {
            MedicalDisclaimerBanner(
                onDismiss = { showDisclaimer = false }
            )
        }

        // Time of Day Selector & Active Factor
        TimeOfDaySelector(
            selectedTimeOfDay = uiState.selectedTimeOfDay,
            effectiveFactor = uiState.calculationSummary.factorUsed,
            isAutoDetected = uiState.isAutoTimeDetection,
            onSelect = { viewModel.selectTimeOfDay(it) },
            onAdjustFactor = { viewModel.adjustFactor(it) },
            onResetAuto = { viewModel.resetToAutoTime() }
        )

        // Carbohydrate Input Card
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

                // Unit selection (g KH, KE, BE)
                UnitSelectorRow(
                    selectedUnit = uiState.selectedUnit,
                    onUnitSelect = { viewModel.setUnit(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Large Input Box
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
                            imeAction = ImeAction.Done
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

        // Expandable Correction Bolus (Blutzucker-Korrektur) Card
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
                                label = { Text("Aktueller BZ (mg/dl)") },
                                placeholder = { Text("z.B. 160") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                label = { Text("Zielwert (mg/dl)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            label = { Text("Korrekturfaktor (mg/dl pro 1 IE)") },
                            placeholder = { Text("z.B. 40") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

        // RESULT CARD (Big Hero Result Display)
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
                        text = String.format(java.util.Locale.US, "%.1f", uiState.calculationSummary.roundedTotalInsulin),
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

        Spacer(modifier = Modifier.height(20.dp))
    }
}
