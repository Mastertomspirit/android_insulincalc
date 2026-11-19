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

import network.spiritscorp.model.CalculationSummary
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay

/**
 * Helper function to instantiate the initial [CalculatorUiState] with default values
 * based on the current time of day and default insulin factor.
 *
 * @param showDisclaimerBanner Flag indicating whether the medical disclaimer banner should be displayed initially.
 * @return Newly constructed [CalculatorUiState] instance.
 */
internal fun createInitialUiState(
    showDisclaimerBanner: Boolean = true
): CalculatorUiState {
    val currentTime = TimeOfDay.current()
    val initialFactor = currentTime.defaultFactor
    return CalculatorUiState(
        selectedTimeOfDay = currentTime,
        showDisclaimerBanner = showDisclaimerBanner,
        calculationSummary = CalculationSummary(
            0.0,
            0.0,
            0.0,
            initialFactor,
            0.0,
            null,
            null,
            0.0,
            0.0,
            0.0,
            0.5,
            false,
            ""
        )
    )
}

/**
 * Immutable data class representing the complete UI state for the insulin calculator screen,
 * including user inputs, active selections, calculation summaries, and UI flags.
 *
 * @property carbInput Raw string input representing carbohydrates entered by the user.
 * @property selectedUnit The active carbohydrate unit ([network.spiritscorp.model.CarbUnit]) currently selected (e.g., Grams, KE, BE).
 * @property selectedTimeOfDay The currently active [TimeOfDay] slot (Morning, Noon, Evening, Night).
 * @property factorOverride Optional manual override for the insulin factor.
 * @property isAutoTimeDetection Flag indicating whether time slot detection is automatic based on system clock.
 * @property glucoseUnit The active blood glucose measurement unit ([network.spiritscorp.model.GlucoseUnit]) (mg/dL or mmol/L).
 * @property currentGlucoseInput Current blood glucose reading input string.
 * @property targetGlucoseInput Target blood glucose level input string.
 * @property correctionFactorInput Individual insulin correction factor input string.
 * @property showCorrection Flag indicating whether correction bolus inputs are currently expanded/visible.
 * @property mealTitle Optional title describing the meal (e.g., Breakfast, Pizza).
 * @property notes Optional free-text notes associated with the meal or calculation.
 * @property showDisclaimerBanner Flag controlling the visibility of the medical disclaimer banner.
 * @property calculationSummary Detailed results object ([CalculationSummary]) holding insulin dosages and breakdown.
 * @property toastMessage Optional transient notification message to display to the user.
 * @property activeTab Index of the currently active bottom navigation tab (0: Calculator, 1: AI Estimator, 2: Logbook, 3: Settings).
 */
data class CalculatorUiState(
    val carbInput: String = "",
    val selectedUnit: CarbUnit = CarbUnit.GRAMS,
    val selectedTimeOfDay: TimeOfDay = TimeOfDay.current(),
    val factorOverride: Double? = null,
    val isAutoTimeDetection: Boolean = true,
    val glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL,
    val currentGlucoseInput: String = "",
    val targetGlucoseInput: String = "120",
    val correctionFactorInput: String = "50",
    val showCorrection: Boolean = false,
    val mealTitle: String = "",
    val notes: String = "",
    val showDisclaimerBanner: Boolean = true,
    val calculationSummary: CalculationSummary = CalculationSummary(
        0.0,
        0.0,
        0.0,
        TimeOfDay.current().defaultFactor,
        0.0,
        null,
        null,
        0.0,
        0.0,
        0.0,
        0.5,
        false,
        ""
    ),
    val toastMessage: String? = null,
    val activeTab: Int = 0
)