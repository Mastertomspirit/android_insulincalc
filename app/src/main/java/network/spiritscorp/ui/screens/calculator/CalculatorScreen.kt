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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import network.spiritscorp.ui.components.MedicalDisclaimerBanner
import network.spiritscorp.ui.components.TimeOfDaySelector
import network.spiritscorp.viewmodel.CalculatorUiState
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel

/**
 * Main Calculator Screen orchestrating the modular calculation cards:
 * - Medical Disclaimer Banner
 * - CalculationResultHeroCard (Recommended dose & save to logbook)
 * - CarbsInputCard (Carb inputs, quick adds, AI estimator shortcut)
 * - TimeOfDaySelector (Target factors & daytime slot)
 * - GlucoseCorrectionCard (Expandable blood sugar correction inputs)
 * - MealNotesCard (Expandable meal title & notes)
 */
@Composable
fun CalculatorScreen(
    viewModel: InsulinCalculatorViewModel,
    uiState: CalculatorUiState,
    onNavigateToAiEstimator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val settings by viewModel.userSettings.collectAsState(initial = null)

    val closeKeyboard = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    closeKeyboard()
                })
            }
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Optional Medical Disclaimer Banner
        if (settings?.isShowDisclaimer == true) {
            MedicalDisclaimerBanner(
                onDismiss = {
                    closeKeyboard()
                    viewModel.dismissDisclaimer()
                }
            )
        }

        // 2. Hero Calculation Result Display & Save to Logbook
        CalculationResultHeroCard(
            uiState = uiState,
            onSaveToLogbook = {
                closeKeyboard()
                viewModel.saveCalculationToLog()
                viewModel.clearAllCalculatorInputs()
            }
        )

        // 3. Carbohydrate Input Card
        CarbsInputCard(
            uiState = uiState,
            onCarbInputChange = { viewModel.onCarbInputChange(it) },
            onClearCarbs = {
                closeKeyboard()
                viewModel.clearCarbs()
            },
            onUnitSelect = {
                closeKeyboard()
                viewModel.setUnit(it)
            },
            onQuickAdd = {
                closeKeyboard()
                viewModel.addCarbs(it)
            },
            onNavigateToAiEstimator = {
                closeKeyboard()
                onNavigateToAiEstimator()
            },
            onDoneKeyboard = { closeKeyboard() }
        )

        // 4. Time of Day Selector & Active Factor
        TimeOfDaySelector(
            selectedTimeOfDay = uiState.selectedTimeOfDay,
            effectiveFactor = uiState.calculationSummary.factorUsed(),
            isAutoDetected = uiState.isAutoTimeDetection,
            onSelect = {
                closeKeyboard()
                viewModel.selectTimeOfDay(it)
            },
            onAdjustFactor = {
                closeKeyboard()
                viewModel.adjustFactor(it)
            },
            onResetAuto = {
                closeKeyboard()
                viewModel.resetToAutoTime()
            }
        )

        // 5. Expandable Blood Glucose Correction Card
        GlucoseCorrectionCard(
            uiState = uiState,
            onToggleCorrection = {
                closeKeyboard()
                viewModel.toggleCorrection()
            },
            onGlucoseInputChange = { viewModel.onGlucoseInputChange(it) },
            onTargetGlucoseChange = { viewModel.onTargetGlucoseChange(it) },
            onCorrectionFactorChange = { viewModel.onCorrectionFactorChange(it) },
            onDoneKeyboard = { closeKeyboard() }
        )

        // 6. Expandable Meal Title & Notes Card
        MealNotesCard(
            uiState = uiState,
            onMealTitleChange = { viewModel.onMealTitleChange(it) },
            onNotesChange = { viewModel.onNotesChange(it) },
            onDoneKeyboard = { closeKeyboard() }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
