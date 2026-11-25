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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.spiritscorp.ai.GeminiAiModel
import network.spiritscorp.data.AppDatabase
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import network.spiritscorp.preferences.DisclaimerPreferences
import java.util.Locale

/**
 * ViewModel responsible for managing insulin calculation state, user preferences,
 * database interactions, and business logic for the calculator and AI estimation.
 *
 * @param application The Android application context used for string resources and preferences.
 * @param repository The [InsulinRepository] handling calculation logs and user settings storage.
 */
class InsulinCalculatorViewModel(
    application: Application,
    private var repository: InsulinRepository?
) : AndroidViewModel(application) {

    /**
     * Convenience secondary constructor initializing with default Room database instances.
     */
    @JvmOverloads
    constructor(
        application: Application,
        db: AppDatabase? = tryGetDatabase(application)
    ) : this(
        application,
        db?.let { InsulinRepository(it.calculationLogDao(), it.userSettingsDao()) }
    )

    private val disclaimerPreferences = DisclaimerPreferences(application)
    private val _mainUiState = MutableStateFlow(MainUiState(0,null,disclaimerPreferences.shouldShowDisclaimer()))

    /** Public read-only StateFlow representing the main UI state. */
    internal val mainUiState: StateFlow<MainUiState> = _mainUiState.asStateFlow()

    internal val databaseMainView: DatabaseMainView = DatabaseMainView(
        application = application,
        initialDatabaseError = { initialDatabaseError },
        repositoryProvider = { repository },
        setCalculator = { cUnit, gUnit, targetStr, corrStr ->
            calculatorMainView.setCarbUnit(cUnit)
            calculatorMainView.setGlucoseUnit(gUnit)
            calculatorMainView.onTargetGlucoseChange(targetStr)
            calculatorMainView.onCorrectionFactorChange(corrStr)
        },
        onShowToast = ::showToast
    )

    /** StateFlow exposing current user settings (insulin factors, targets, etc.). */
    internal val cachedSettings: StateFlow<UserSettings> get() = databaseMainView.cachedSettings

    internal val calculatorMainView: CalculatorMainView = CalculatorMainView(
        application = application,
        beGramsDivisor = { cachedSettings.value.beGramsDivisor },
        roundingStep = { cachedSettings.value.roundingStep },
        onTimeOfDay = { actualTime -> getFactorForTime(actualTime) },
    )

    internal val logMainView = LogMainView(
        application = application,
        repository = { repository },
        calculatorUiState = { calculatorMainView.calculatorUiState.value },
        onShowToast = ::showToast
    )

    internal val aiViewModel = AiViewModel(
        application = application,
        geminiAiModel = { cachedSettings.value.selectedAiModel },
        geminiApiKey = { cachedSettings.value.geminiApiKey },
        onUpdateSettings = { geminiAiModel: GeminiAiModel, geminiApiKey: String ->
            databaseMainView.updateGeminiSettings(geminiAiModel, geminiApiKey)
        },
        onApplyEstimateToCalculator = { formattedCarbs: String, mealName: String, notesText: String ->
            calculatorMainView.onCarbInputChange(formattedCarbs)
            calculatorMainView.onMealTitleChange(mealName)
            calculatorMainView.onNotesChange(notesText)
            setTab(0)
        },
    )

    init {
        viewModelScope.launch {
            cachedSettings.collect { settings ->
                val gUnit = settings.glucoseUnit
                val targetStr = if (gUnit == GlucoseUnit.MMOL_L) {
                    val mmol = GlucoseUnit.MMOL_L.fromMgDl(settings.targetGlucoseMgDl)
                    if (mmol % 1.0 == 0.0) mmol.toInt().toString() else "%.1f".format(Locale.getDefault(), mmol)
                } else {
                    settings.targetGlucoseMgDl.toString().replace(".0", "")
                }
                val corrStr = if (gUnit == GlucoseUnit.MMOL_L) {
                    val mmol = GlucoseUnit.MMOL_L.fromMgDl(settings.correctionFactorMgDl)
                    if (mmol % 1.0 == 0.0) mmol.toInt().toString() else "%.1f".format(Locale.getDefault(), mmol)
                } else {
                    settings.correctionFactorMgDl.toString().replace(".0", "")
                }

                calculatorMainView.setCarbUnit(settings.defaultCarbUnit)
                calculatorMainView.setGlucoseUnit(gUnit)
                calculatorMainView.onTargetGlucoseChange(targetStr)
                calculatorMainView.onCorrectionFactorChange(corrStr)
            }
        }
    }

    /**
     * Switches the active bottom navigation tab.
     *
     * @param index Tab index (0: Calculator, 1: AI Estimator, 2: Logbook, 3: Settings).
     */
    internal fun setTab(index: Int) {
        _mainUiState.update { it.copy(activeTab = index) }
    }

    /**
     * Dismisses the medical disclaimer banner, updates local UI state, persists the
     * preference dismissal in SharedPreferences, and updates user settings in the database.
     */
    internal fun dismissDisclaimer() {
        disclaimerPreferences.dismissDisclaimer()
        _mainUiState.update { it.copy(showDisclaimerBanner = false) }
        databaseMainView.updateDisclaimerOnDatabase(false)
    }

    /**
     * Shows a toast notification message.
     *
     * @param message Message to display.
     */
    internal fun showToast(message: String) {
        _mainUiState.update { it.copy(toastMessage = message) }
    }

    /**
     * Clears the currently active toast message.
     */
    internal fun clearToast() {
        _mainUiState.update { it.copy(toastMessage = null) }
    }

    private fun getFactorForTime(timeOfDay: TimeOfDay): Double {
        val settings = cachedSettings.value
        return when (timeOfDay) {
            TimeOfDay.MORNING -> settings.morningFactor
            TimeOfDay.NOON -> settings.noonFactor
            TimeOfDay.EVENING -> settings.eveningFactor
            TimeOfDay.NIGHT -> settings.nightFactor
        }
    }

    companion object {
        private var initialDatabaseError: Boolean = false

        @JvmStatic
        internal fun tryGetDatabase(application: Application): AppDatabase? {
            val db = AppDatabase.getDatabase(application)
            if (db == null) {
                initialDatabaseError = true
                return null
            } else {
                initialDatabaseError = false
                return db
            }
        }
    }
}
