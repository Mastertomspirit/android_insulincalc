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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.spiritscorp.R
import network.spiritscorp.ai.GeminiAiModel
import network.spiritscorp.ai.GeminiMealService
import network.spiritscorp.ai.MealEstimateResult
import network.spiritscorp.data.AppDatabase
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.data.ImportResult
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.CalculationSummary
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import network.spiritscorp.preferences.DisclaimerPreferences
import network.spiritscorp.preferences.ThemePreferences
import network.spiritscorp.util.InsulinMathEngine
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

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
 * @property selectedUnit The active carbohydrate unit ([CarbUnit]) currently selected (e.g., Grams, KE, BE).
 * @property selectedTimeOfDay The currently active [TimeOfDay] slot (Morning, Noon, Evening, Night).
 * @property factorOverride Optional manual override for the insulin factor.
 * @property isAutoTimeDetection Flag indicating whether time slot detection is automatic based on system clock.
 * @property glucoseUnit The active blood glucose measurement unit ([GlucoseUnit]) (mg/dL or mmol/L).
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

/**
 * Sealed interface representing the asynchronous state of AI meal estimation requests.
 */
sealed interface AiEstimateState {
    /** Idle state when no AI estimation is currently running. */
    object Idle : AiEstimateState
    /** Loading state while communicating with the Gemini AI model. */
    object Loading : AiEstimateState
    /** Success state containing the parsed [MealEstimateResult]. */
    data class Success(val result: MealEstimateResult) : AiEstimateState
    /** Error state containing failure details. */
    data class Error(val message: String) : AiEstimateState
}

/**
 * ViewModel responsible for managing insulin calculation state, user preferences,
 * database interactions, and business logic for the calculator and AI estimation.
 *
 * @param application The Android application context used for string resources and preferences.
 * @param repository The [InsulinRepository] handling calculation logs and user settings storage.
 */
class InsulinCalculatorViewModel(
    application: Application,
    private val repository: InsulinRepository
) : AndroidViewModel(application) {

    /**
     * Convenience secondary constructor initializing with default Room database instances.
     */
    @JvmOverloads
    constructor(
        application: Application,
        db: AppDatabase = AppDatabase.getDatabase(application)
    ) : this(application, InsulinRepository(db.calculationLogDao(), db.userSettingsDao()))
    private val geminiService = GeminiMealService(application)
    private val themePreferences = ThemePreferences(application)
    private val disclaimerPreferences = DisclaimerPreferences(application)
    private var cachedSettings: UserSettings = UserSettings()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSettings()
        }
    }

    /** StateFlow exposing the list of past calculation logs for the history / logbook. */
    val historyLogs: StateFlow<List<CalculationLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** StateFlow exposing current user settings (insulin factors, targets, etc.). */
    val userSettings: StateFlow<UserSettings> = repository.settingsFlow
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _uiState = MutableStateFlow(
        createInitialUiState(
            showDisclaimerBanner = disclaimerPreferences.shouldShowDisclaimer()
        )
    )

    /** Public read-only StateFlow representing the calculator UI state. */
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow<AiEstimateState>(AiEstimateState.Idle)
    /** Public read-only StateFlow representing AI estimation state. */
    val aiState: StateFlow<AiEstimateState> = _aiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settingsNullable ->
                val settings = settingsNullable ?: UserSettings()
                cachedSettings = settings
                val gUnit = settings.glucoseUnit
                val initialTime = if (_uiState.value.isAutoTimeDetection) TimeOfDay.current() else _uiState.value.selectedTimeOfDay
                val unit = settings.defaultCarbUnit
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
                val shouldShowBanner = disclaimerPreferences.shouldShowDisclaimer()

                _uiState.update { current ->
                    current.copy(
                        selectedUnit = if (current.carbInput.isEmpty()) unit else current.selectedUnit,
                        glucoseUnit = gUnit,
                        targetGlucoseInput = if (current.targetGlucoseInput.isEmpty() || current.targetGlucoseInput == "120") targetStr else current.targetGlucoseInput,
                        correctionFactorInput = if (current.correctionFactorInput.isEmpty() || current.correctionFactorInput == "50") corrStr else current.correctionFactorInput,
                        selectedTimeOfDay = initialTime,
                        showDisclaimerBanner = shouldShowBanner
                    )
                }
                recalculate(settings)
            }
        }
    }

    /**
     * Switches the active bottom navigation tab.
     *
     * @param index Tab index (0: Calculator, 1: AI Estimator, 2: Logbook, 3: Settings).
     */
    fun setTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    /**
     * Handles carbohydrate text input changes, sanitizing input (replacing commas with dots,
     * allowing numbers and single decimal separator) and triggering recalculation.
     *
     * @param newInput Raw input string from the carbohydrate text field.
     */
    fun onCarbInputChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        // Prevent multiple decimal dots
        val dotsCount = sanitized.count { it == '.' }
        if (dotsCount <= 1 && sanitized.length <= 8) {
            _uiState.update { it.copy(carbInput = sanitized) }
            recalculate()
        }
    }

    /**
     * Adds a quick increment of carbohydrates (e.g., +5g, +10g) to the current carb input.
     *
     * @param amount Carbohydrate increment amount to add.
     */
    fun addCarbs(amount: Double) {
        val current = _uiState.value.carbInput.toDoubleOrNull() ?: 0.0
        val updated = (current + amount).coerceAtLeast(0.0)
        _uiState.update {
            it.copy(carbInput = if (updated % 1.0 == 0.0) updated.toInt().toString() else "%.1f".format(Locale.getDefault(), updated))
        }
        recalculate()
    }

    /**
     * Resets the carbohydrate input to zero ("0").
     */
    fun clearCarbs() {
        _uiState.update { it.copy(carbInput = "0") }
        recalculate()
    }

    /**
     * Clears all primary calculator input fields (carbs, blood glucose, meal title, notes).
     */
    fun clearAllCalculatorInputs() {
        _uiState.update {
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
     * Updates the selected carbohydrate unit (Grams, KE, BE) while preserving raw input value.
     *
     * @param unit The new [CarbUnit] to set.
     */
    fun setUnit(unit: CarbUnit) {
        // Keeps raw input value unchanged as requested ("die eingabe soll sich nicht ändern, wenn umgeschalten wird")
        _uiState.update { it.copy(selectedUnit = unit) }
        recalculate()
    }

    /**
     * Manually selects a specific time of day slot and clears any manual factor override,
     * disabling automatic time detection.
     *
     * @param timeOfDay The selected [TimeOfDay].
     */
    fun selectTimeOfDay(timeOfDay: TimeOfDay) {
        _uiState.update {
            it.copy(
                selectedTimeOfDay = timeOfDay,
                factorOverride = null,
                isAutoTimeDetection = false
            )
        }
        recalculate()
    }

    /**
     * Re-enables automatic time-of-day detection based on the current system time.
     */
    fun resetToAutoTime() {
        val current = TimeOfDay.current()
        _uiState.update {
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
    fun adjustFactor(delta: Double) {
        val currentFactor = getEffectiveFactor()
        val updated = BigDecimal(currentFactor + delta).setScale(2, RoundingMode.HALF_UP).toDouble()
        val clamped = updated.coerceIn(0.05, 10.00)
        _uiState.update { it.copy(factorOverride = clamped) }
        recalculate()
    }

    fun dismissDisclaimer() {
        disclaimerPreferences.dismissDisclaimer()
        _uiState.update { it.copy(showDisclaimerBanner = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val updatedSettings = userSettings.value.copy()
            updatedSettings.isShowDisclaimer = false
            repository.saveSettings(updatedSettings)
        }
    }

    /**
     * Toggles the visibility of the blood glucose correction inputs section.
     */
    fun toggleCorrection() {
        _uiState.update { it.copy(showCorrection = !it.showCorrection) }
        recalculate()
    }

    /**
     * Handles current blood glucose text input changes, sanitizing input and triggering recalculation.
     *
     * @param newInput Raw input string for current blood glucose.
     */
    fun onGlucoseInputChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(currentGlucoseInput = sanitized) }
        recalculate()
    }

    /**
     * Handles target blood glucose text input changes, sanitizing input and triggering recalculation.
     *
     * @param newInput Raw input string for target blood glucose.
     */
    fun onTargetGlucoseChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(targetGlucoseInput = sanitized) }
        recalculate()
    }

    /**
     * Handles correction factor text input changes, sanitizing input and triggering recalculation.
     *
     * @param newInput Raw input string for the correction factor.
     */
    fun onCorrectionFactorChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(correctionFactorInput = sanitized) }
        recalculate()
    }

    /**
     * Updates the meal title input string.
     *
     * @param title New meal title.
     */
    fun onMealTitleChange(title: String) {
        _uiState.update { it.copy(mealTitle = title) }
    }

    /**
     * Updates the meal notes input string.
     *
     * @param notes New meal notes.
     */
    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    /**
     * Called when the app resumes from background to refresh
     * auto-detected time of day if active.
     */
    fun onResume() {
        if (_uiState.value.isAutoTimeDetection) {
            recalculate()
        }
    }

    private fun getEffectiveFactor(settings: UserSettings = cachedSettings): Double {
        val state = _uiState.value
        if (state.factorOverride != null) {
            return state.factorOverride
        }
        val effectiveSettings = if (settings.id != 0 || settings != UserSettings()) settings else (userSettings.value)
        val activeTimeOfDay = if (state.isAutoTimeDetection) TimeOfDay.current() else state.selectedTimeOfDay
        return getFactorForTime(activeTimeOfDay, effectiveSettings)
    }

    private fun getFactorForTime(timeOfDay: TimeOfDay, settings: UserSettings): Double {
        return when (timeOfDay) {
            TimeOfDay.MORNING -> settings.morningFactor
            TimeOfDay.NOON -> settings.noonFactor
            TimeOfDay.EVENING -> settings.eveningFactor
            TimeOfDay.NIGHT -> settings.nightFactor
        }
    }

    private fun recalculate(passedSettings: UserSettings? = null) {
        val state = _uiState.value
        val rawInput = state.carbInput.replace(',','.').toDoubleOrNull() ?: 0.0
        val settings = passedSettings ?: cachedSettings
        val activeTimeOfDay = if (state.isAutoTimeDetection) TimeOfDay.current() else state.selectedTimeOfDay

        val grams = InsulinMathEngine.convertToGrams(rawInput, state.selectedUnit, settings.beGramsDivisor)
        val ke = InsulinMathEngine.calculateKe(grams)
        val be = InsulinMathEngine.calculateBe(grams, settings.beGramsDivisor)
        val factor = getEffectiveFactor(settings)
        val mealInsulin = InsulinMathEngine.calculateMealInsulin(
            rawInput,
            state.selectedUnit,
            grams,
            factor,
            settings.beGramsDivisor
        )

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
        val correctionInsulin = InsulinMathEngine.calculateCorrectionInsulin(true, currentBg, targetBg, corrFactor)
        val rawTotal = (mealInsulin + correctionInsulin)
        val roundingStep = settings.roundingStep

        _uiState.update {
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
                    InsulinMathEngine.roundToStep(rawTotal, roundingStep),
                    roundingStep,
                    isHypoRisk,
                    advisory
                )
            )
        }
    }

    /**
     * Saves the current calculator state and calculation summary as a new entry in the logbook history database.
     * Captures a full snapshot of all inputs, insulin breakdown, and optional overrides.
     *
     * @param notesOverride Optional override string for the calculation notes.
     */
    fun saveCalculationToLog(notesOverride: String? = null) {
        // Capture full snapshot synchronously at moment of invocation
        val state = _uiState.value
        val summary = state.calculationSummary
        val autoMealTitle = state.mealTitle.ifBlank {
            "${getApplication<Application>().getString(state.selectedTimeOfDay.titleResId)} (${summary.carbGrams()}g KH)"
        }
        val finalNotes = notesOverride ?: state.notes

        val log = CalculationLog(
            0L,
            System.currentTimeMillis(),
            autoMealTitle,
            state.carbInput.replace(',', '.').toDoubleOrNull() ?: 0.0,
            state.selectedUnit,
            summary.carbGrams(),
            summary.beValue(),
            summary.keValue(),
            state.selectedTimeOfDay,
            summary.factorUsed(),
            summary.mealInsulin(),
            summary.bloodGlucoseInput(),
            summary.targetGlucose(),
            state.correctionFactorInput.replace(',', '.').toDoubleOrNull(),
            summary.correctionInsulin(),
            summary.rawTotalInsulin(),
            summary.roundedTotalInsulin(),
            finalNotes
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCalculation(log)
            _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.view_model_diary_saved_toast)) }
        }
    }

    /**
     * Shows a toast notification message.
     *
     * @param message Message to display.
     */
    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    /**
     * Clears the currently active toast message.
     */
    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    /**
     * Deletes a specific calculation log entry from the database.
     *
     * @param logId Unique identifier of the log entry to delete.
     */
    fun deleteLog(logId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLog(logId)
            _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.logbook_entry_deleted_toast)) }
        }
    }

    /**
     * Clears all calculation logs from the database.
     */
    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
            _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.view_model_logbook_cleared)) }
        }
    }

    /**
     * Exports all user settings and calculation logs to a JSON backup string.
     *
     * @return JSON string representing the database backup.
     */
    suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        val settings = repository.settings
        val logs = repository.allLogsDirect
        DatabaseBackupManager(repository.userSettingsDao(), repository.calculationLogDao()).exportToJson(settings, logs)
    }

    /**
     * Exports all calculation logs to a CSV backup string.
     *
     * @return CSV string representing the calculation history.
     */
    suspend fun exportCsvBackup(): String = withContext(Dispatchers.IO) {
        val logs = repository.allLogsDirect
        DatabaseBackupManager(repository.userSettingsDao(), repository.calculationLogDao()).exportToCsv(logs)
    }

    /**
     * Imports backup content (auto-detecting JSON or CSV format) into the database.
     *
     * @param content Raw string content of the backup file.
     * @return [ImportResult] containing success status and statistics.
     */
    suspend fun importBackupContent(content: String): ImportResult = withContext(Dispatchers.IO) {
        val manager = DatabaseBackupManager(getApplication<Application>())
        val trimmed = content.trim()
        val result = if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            manager.importFromJson(trimmed)
        } else {
            manager.importFromCsv(trimmed)
        }
        if (result.isSuccess && result.isImportedSettings) {
            val updated = repository.settings
            if (updated != null) {
                cachedSettings = updated
                themePreferences.savePreferences(updated.selectedTheme, updated.themeMode)
                recalculate(updated)
            }
        }
        result
    }

    /**
     * Persists updated user settings to the database and refreshes active state and calculations.
     *
     * @param settings The new [UserSettings] object.
     */
    fun updateUserSettings(settings: UserSettings) {
        cachedSettings = settings
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(settings)
            // Persist theme choice synchronously in SharedPreferences to prevent start-up flicker
            themePreferences.savePreferences(
                settings.selectedTheme,
                settings.themeMode
            )
        }
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

        _uiState.update { current ->
            current.copy(
                selectedUnit = settings.defaultCarbUnit,
                glucoseUnit = gUnit,
                targetGlucoseInput = targetStr,
                correctionFactorInput = corrStr,
                toastMessage = getApplication<Application>().getString(R.string.view_model_settings_saved_toast)
            )
        }
        recalculate(settings)
    }

    /**
     * Estimates carbohydrate values from a natural language food description using the Gemini AI service.
     *
     * @param description Food description string entered by the user.
     */
    fun estimateMealCarbs(description: String) {
        if (description.isBlank()) return
        _aiState.value = AiEstimateState.Loading
        viewModelScope.launch {
            val settings = userSettings.value
            val result = geminiService.estimateCarbsFromDescription(
                foodDescription = description,
                customApiKey = settings.geminiApiKey,
                selectedModel = settings.selectedAiModel
            )
            result.onSuccess { data ->
                _aiState.value = AiEstimateState.Success(data)
            }.onFailure { err ->
                _aiState.value = AiEstimateState.Error(err.message ?: getApplication<Application>().getString(R.string.view_model_ai_error_generic))
            }
        }
    }

    /**
     * Saves Gemini AI API key and model configuration to user settings.
     *
     * @param apiKey API key string.
     * @param modelId Selected [GeminiAiModel].
     */
    fun saveAiConfiguration(apiKey: String, modelId: GeminiAiModel) {
        viewModelScope.launch {
            val current = userSettings.value
            val updated = current.copy()
            updated.geminiApiKey = apiKey.trim()
            updated.selectedAiModel = modelId
            updateUserSettings(updated)
        }
    }

    /**
     * Applies estimated carbohydrates and meal details from AI estimation directly into the calculator inputs
     * and switches back to the calculator tab.
     *
     * @param carbsGrams Estimated carbohydrate amount in grams.
     * @param mealName Suggested meal title.
     * @param notesText Detailed breakdown notes.
     */
    fun applyEstimatedCarbsToCalculator(
        carbsGrams: Double,
        mealName: String,
        notesText: String
    ) {
        val currentUnit = _uiState.value.selectedUnit
        val converted = currentUnit.fromGrams(carbsGrams)
        val formatted = if (currentUnit == CarbUnit.GRAMS) {
            converted.toInt().toString()
        } else {
            "%.1f".format(Locale.getDefault(), converted)
        }

        _uiState.update {
            it.copy(
                carbInput = formatted,
                mealTitle = mealName,
                notes = notesText,
                activeTab = 0 // Switch back to calculator
            )
        }
        recalculate()
    }
}
