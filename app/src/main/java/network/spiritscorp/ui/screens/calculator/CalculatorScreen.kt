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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import network.spiritscorp.ui.components.MedicalDisclaimerBanner
import network.spiritscorp.ui.components.TimeOfDaySelector
import network.spiritscorp.viewmodel.CalculatorMainView
import network.spiritscorp.viewmodel.CalculatorUiState
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import network.spiritscorp.viewmodel.LogMainView
import androidx.compose.runtime.collectAsState

/**
 * Main Calculator Screen orchestrating the modular calculation cards.
 *
 * This screen manages the layout and state for insulin calculation inputs, rendering cards
 * from top to bottom:
 * - Medical Disclaimer Banner (optional)
 * - [CalculationResultHeroCard] for recommended dose and logbook saving
 * - [CarbsInputCard] for carbohydrate entry, unit selection, and AI estimation
 * - [TimeOfDaySelector] for time slot and target factors
 * - [GlucoseCorrectionCard] for expandable blood glucose correction inputs
 * - [MealNotesCard] for optional meal title and notes
 *
 * @param viewModel The [InsulinCalculatorViewModel] holding calculator business logic.
 * @param calculatorUiState The current [CalculatorUiState] representing input values and results.
 * @param onNavigateToAiEstimator Callback to trigger navigation to the AI carb estimator screen.
 * @param modifier Optional [Modifier] for configuring layout behavior.
 */
@Composable
internal fun CalculatorScreen(
    viewModel: InsulinCalculatorViewModel,
    calculatorMainView: CalculatorMainView,
    logMainView: LogMainView,
    calculatorUiState: CalculatorUiState,
    onNavigateToAiEstimator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
        if (viewModel.mainUiState.collectAsState().value.showDisclaimerBanner) {
            MedicalDisclaimerBanner(
                onDismiss = {
                    closeKeyboard()
                    viewModel.dismissDisclaimer()
                }
            )
        }

        // 2. Hero Calculation Result Display & Save to Logbook
        CalculationResultHeroCard(
            uiState = calculatorUiState,
            onSaveToLogbook = {
                closeKeyboard()
                logMainView.saveCalculationToLog()
                calculatorMainView.clearAllCalculatorInputs()
            }
        )

        // 3. Carbohydrate Input Card
        CarbsInputCard(
            calculatorUiState = calculatorUiState,
            onCarbInputChange = { calculatorMainView.onCarbInputChange(it) },
            onClearCarbs = {
                closeKeyboard()
                calculatorMainView.clearCarbs()
            },
            onUnitSelect = {
                closeKeyboard()
                calculatorMainView.setCarbUnit(it)
            },
            onQuickAdd = {
                closeKeyboard()
                calculatorMainView.addCarbs(it)
            },
            onNavigateToAiEstimator = {
                closeKeyboard()
                onNavigateToAiEstimator()
            },
            onDoneKeyboard = { closeKeyboard() }
        )

        // 4. Time of Day Selector & Active Factor
        TimeOfDaySelector(
            selectedTimeOfDay = calculatorUiState.selectedTimeOfDay,
            effectiveFactor = calculatorUiState.calculationSummary.factorUsed(),
            isAutoDetected = calculatorUiState.isAutoTimeDetection,
            onSelect = {
                closeKeyboard()
                calculatorMainView.selectTimeOfDay(it)
            },
            onAdjustFactor = {
                closeKeyboard()
                calculatorMainView.adjustFactor(it)
            },
            onResetAuto = {
                closeKeyboard()
                calculatorMainView.resetToAutoTime()
            }
        )

        // 5. Expandable Blood Glucose Correction Card
        GlucoseCorrectionCard(
            uiState = calculatorUiState,
            onToggleCorrection = {
                closeKeyboard()
                calculatorMainView.toggleCorrection()
            },
            onGlucoseInputChange = { calculatorMainView.onGlucoseInputChange(it) },
            onTargetGlucoseChange = { calculatorMainView.onTargetGlucoseChange(it) },
            onCorrectionFactorChange = { calculatorMainView.onCorrectionFactorChange(it) },
            onDoneKeyboard = { closeKeyboard() }
        )

        // 6. Expandable Meal Title & Notes Card
        MealNotesCard(
            uiState = calculatorUiState,
            onMealTitleChange = { calculatorMainView.onMealTitleChange(it) },
            onNotesChange = { calculatorMainView.onNotesChange(it) },
            onDoneKeyboard = { closeKeyboard() }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
