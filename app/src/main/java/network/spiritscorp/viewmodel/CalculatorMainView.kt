package network.spiritscorp.viewmodel

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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.spiritscorp.insulincalc.R
import network.spiritscorp.model.CalculationSummary
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.util.InsulinMathEngine
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * ViewModel dedicated to handling insulin calculation business logic, user inputs (carbohydrates,
 * blood glucose, targets, correction factors, notes), time of day selection, unit conversions,
 * medical disclaimer state, and dosage calculation updates.
 *
 * Uses [CalculatorUiState] to manage screen state.
 *
 * @param application    The Android application context.
 * @param beGramsDivisor Lambda returning the current grams per BE divisor setting.
 * @param roundingStep   Lambda returning the current insulin rounding step increment.
 * @param onTimeOfDay    Lambda returning the insulin factor for a given [TimeOfDay].
 */
internal class CalculatorMainView(
    application: Application,
    private val beGramsDivisor: () -> Int,
    private val roundingStep: () -> Double,
    private val onTimeOfDay: (TimeOfDay) -> Double,
) : AndroidViewModel(application) {

    private val _calculatorUiState = MutableStateFlow(createInitialCalculatorUiState())

    internal val calculatorUiState: StateFlow<CalculatorUiState> = _calculatorUiState.asStateFlow()

    /**
     * Handles carbohydrate text input changes, sanitizing input (replacing commas with dots,
     * allowing numbers and single decimal separator) and triggering recalculation.
     *
     * @param input Raw input string from the carbohydrate text field.
     */
    internal fun onCarbInputChange(input: String) {
        val sanitized = sanitize(input)
        val dotsCount = sanitized.count { it == '.' }
        if (dotsCount <= 1 && sanitized.length <= 5) {
            _calculatorUiState.update { it.copy(carbInput = sanitized) }
            recalculate()
        }
    }

    /**
     * Adds a quick increment of carbohydrates (e.g., +5g, +10g) to the current carb input.
     *
     * @param amount Carbohydrate increment amount to add.
     */
    internal fun addCarbs(amount: Double) {
        val current = _calculatorUiState.value.carbInput.toDoubleOrNull() ?: 0.0
        val updated = (current + amount).coerceAtLeast(0.0)
        _calculatorUiState.update {
            it.copy(carbInput = if (updated % 1.0 == 0.0) updated.toInt().toString() else "%.1f".format(Locale.getDefault(), updated))
        }
        recalculate()
    }

    /** Resets the carbohydrate input to empty. */
    internal fun clearCarbs() {
        _calculatorUiState.update { it.copy(carbInput = "") }
        recalculate()
    }

    /** Clears all primary calculator input fields (carbs, blood glucose, meal title, notes). */
    internal fun clearAllCalculatorInputs() {
        _calculatorUiState.update {
            it.copy(
                carbInput = "",
                currentGlucoseInput = "",
                mealTitle = "",
                notes = ""
            )
        }
        recalculate()
    }

    /**
     * Updates the selected carbohydrate unit (Grams, KE, BE).
     *
     * @param carbUnit New [CarbUnit] selection.
     */
    internal fun setCarbUnit(carbUnit: CarbUnit) {
        _calculatorUiState.update { it.copy(selectedUnit = carbUnit) }
        recalculate()
    }

    /**
     * Updates the selected glucose unit (ml/dl, mmol).
     *
     * @param glucoseUnit New [GlucoseUnit] selection.
     */
    internal fun setGlucoseUnit(glucoseUnit: GlucoseUnit) {
        _calculatorUiState.update { it.copy(glucoseUnit = glucoseUnit) }
        recalculate()
    }

    /**
     * Manually selects a specific time of day slot and clears any manual factor override, disabling automatic time detection.
     *
     * @param timeOfDay The selected [TimeOfDay].
     */
    internal fun selectTimeOfDay(timeOfDay: TimeOfDay) {
        _calculatorUiState.update {
            it.copy(
                selectedTimeOfDay = timeOfDay,
                factorOverride = null,
                isAutoTimeDetection = false
            )
        }
        recalculate()
    }

    /** Re-enables automatic time-of-day detection based on system clock. */
    internal fun resetToAutoTime() {
        val current = TimeOfDay.current()
        _calculatorUiState.update {
            it.copy(
                selectedTimeOfDay = current,
                factorOverride = null,
                isAutoTimeDetection = true
            )
        }
        recalculate()
    }

    /**
     * Adjusts the insulin factor override by a given delta value (clamped between 0.05 and 10.00).
     *
     * @param delta Amount to add or subtract from the current factor.
     */
    internal fun adjustFactor(delta: Double) {
        val currentFactor = getEffectiveFactor()
        val updated = BigDecimal(currentFactor + delta).setScale(2, RoundingMode.HALF_UP).toDouble()
        val clamped = updated.coerceIn(0.05, 10.00)
        _calculatorUiState.update { it.copy(factorOverride = clamped) }
        recalculate()
    }

    /** Toggles visibility of blood glucose correction inputs section. */
    internal fun toggleCorrection() {
        _calculatorUiState.update { it.copy(showCorrection = !it.showCorrection) }
        recalculate()
    }

    /**
     * Handles current blood glucose text input changes, sanitizing input and triggering recalculation.
     *
     * @param input Raw input string for current blood glucose.
     */
    internal fun onGlucoseInputChange(input: String) {
        val sanitized = sanitize(input)
        _calculatorUiState.update { it.copy(currentGlucoseInput = sanitized) }
        recalculate()
    }

    /**
     * Handles target blood glucose text input changes, sanitizing input and triggering recalculation.
     *
     * @param input Raw input string for target blood glucose.
     */
    internal fun onTargetGlucoseChange(input: String) {
        val sanitized = sanitize(input)
        _calculatorUiState.update { it.copy(targetGlucoseInput = sanitized) }
        recalculate()
    }

    /**
     * Handles correction factor text input changes, sanitizing input and triggering recalculation.
     *
     * @param input Raw input string for the correction factor.
     */
    internal fun onCorrectionFactorChange(input: String) {
        val sanitized = sanitize(input)
        _calculatorUiState.update { it.copy(correctionFactorInput = sanitized) }
        recalculate()
    }

    /**
     * Updates the meal title input string.
     *
     * @param title New meal title.
     */
    internal fun onMealTitleChange(title: String) {
        _calculatorUiState.update { it.copy(mealTitle = title) }
    }

    /**
     * Updates the meal notes input string.
     *
     * @param notes New meal notes.
     */
    internal fun onNotesChange(notes: String) {
        _calculatorUiState.update { it.copy(notes = notes) }
    }

    /** Called when the app resumes from background to refresh auto-detected time of day if active. */
    internal fun onResume() {
        if (_calculatorUiState.value.isAutoTimeDetection) {
            recalculate()
        }
    }

    /**
     * Returns the effective insulin factor currently applied, taking into account
     * any manual factor override or time-of-day factor (auto-detected or manually selected).
     *
     * @return The effective insulin factor as a [Double].
     */
    internal fun getEffectiveFactor(): Double {
        val state = _calculatorUiState.value
        if (state.factorOverride != null) {
            return state.factorOverride
        }
        val activeTimeOfDay = if (state.isAutoTimeDetection) TimeOfDay.current() else state.selectedTimeOfDay
        return onTimeOfDay(activeTimeOfDay)
    }

    /**
     * Sanitizes raw text input by replacing commas with dots and filtering out invalid characters.
     *
     * @param input Raw input string.
     * @return Sanitized numeric string.
     */
    private fun sanitize(input: String): String {
        return input.replace(',', '.').filter { it.isDigit() || it == '.' }
    }

    /**
     * Performs insulin calculations (carbs conversion, KE/BE, meal insulin, correction insulin,
     * rounding, warnings) based on current UI state and updates [CalculatorUiState].
     */
    private fun recalculate() {
        val state = _calculatorUiState.value
//        tOdO: not needed? Only toDoubleOrNull?
        val rawInput = state.carbInput.replace(',','.').toDoubleOrNull() ?: 0.0
        val activeTimeOfDay = if (state.isAutoTimeDetection) TimeOfDay.current() else state.selectedTimeOfDay

        val divisor = beGramsDivisor()
        val grams = InsulinMathEngine.convertToGrams(rawInput, state.selectedUnit, divisor)
        val ke = InsulinMathEngine.calculateKe(grams)
        val be = InsulinMathEngine.calculateBe(grams, divisor)
        val factor = getEffectiveFactor()
        val mealInsulin = InsulinMathEngine.calculateMealInsulin(
            rawInput,
            state.selectedUnit,
            grams,
            factor,
            divisor
        )
//        tOdO: not needed? Only toDoubleOrNull? same?
        val currentBg = state.currentGlucoseInput.replace(',', '.').toDoubleOrNull()
        val targetBg = state.targetGlucoseInput.replace(',', '.').toDoubleOrNull()
        val corrFactor = state.correctionFactorInput.replace(',', '.').toDoubleOrNull()

        var isHypoRisk = false
        var advisory = ""
        val gUnit = state.glucoseUnit
        val isMmol = (gUnit == GlucoseUnit.MMOL_L)

        if (state.showCorrection && currentBg != null && targetBg != null && corrFactor != null) {
            val app = getApplication<Application>()
            if (InsulinMathEngine.isHypoglycemia(currentBg, isMmol)) {
                isHypoRisk = true
                val unitStr = app.getString(gUnit.shortNameResId)
                val thresholdStr = if (isMmol) "3.9 $unitStr" else "70 $unitStr"
                advisory = app.getString(R.string.view_model_advisory_hypo, thresholdStr)
            } else if (currentBg < targetBg) {
                advisory = app.getString(R.string.view_model_advisory_below_target)
            } else if (currentBg > targetBg) {
                advisory = app.getString(R.string.view_model_advisory_above_target)
            }
        }
        val correctionInsulin = InsulinMathEngine.calculateCorrectionInsulin(state.showCorrection, currentBg, targetBg, corrFactor)
        val rawTotal = 0.0.coerceAtLeast(mealInsulin + correctionInsulin)
        val roundingStep = roundingStep()
        val roundedTotal = 0.0.coerceAtLeast(InsulinMathEngine.roundToStep(rawTotal, roundingStep))

        _calculatorUiState.update {
            it.copy(
                selectedTimeOfDay = activeTimeOfDay,
                calculationSummary = CalculationSummary(
                    InsulinMathEngine.roundToDecimals(grams, 1),
                    InsulinMathEngine.roundToDecimals(ke, 2),
                    InsulinMathEngine.roundToDecimals(be, 2),
                    factor,
                    InsulinMathEngine.roundToDecimals(mealInsulin, 2),
                    currentBg,
                    targetBg,
                    InsulinMathEngine.roundToDecimals(correctionInsulin, 2),
                    InsulinMathEngine.roundToDecimals(rawTotal, 2),
                    roundedTotal,
                    roundingStep,
                    isHypoRisk,
                    advisory
                )
            )
        }
    }
}
