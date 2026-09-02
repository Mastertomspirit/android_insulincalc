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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.spiritscorp.ai.GeminiMealService
import network.spiritscorp.ai.MealEstimateResult
import network.spiritscorp.data.AppDatabase
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.data.ImportResult
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.data.ThemePreferences
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.CalculationSummary
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import network.spiritscorp.util.InsulinMathEngine
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

internal fun createInitialUiState(): CalculatorUiState {
    val currentTime = TimeOfDay.current()
    val initialFactor = currentTime.defaultFactor
    return CalculatorUiState(
        selectedTimeOfDay = currentTime,
        calculationSummary = CalculationSummary(
            carbGrams = 0.0,
            keValue = 0.0,
            beValue = 0.0,
            factorUsed = initialFactor,
            mealInsulin = 0.0,
            bloodGlucoseInput = null,
            targetGlucose = null,
            correctionInsulin = 0.0,
            rawTotalInsulin = 0.0,
            roundedTotalInsulin = 0.0,
            roundingStep = 0.5,
            isHypoRisk = false,
            advisoryNote = "Bereit für Eingabe"
        )
    )
}

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
    val calculationSummary: CalculationSummary = CalculationSummary(
        carbGrams = 0.0,
        keValue = 0.0,
        beValue = 0.0,
        factorUsed = TimeOfDay.current().defaultFactor,
        mealInsulin = 0.0,
        bloodGlucoseInput = null,
        targetGlucose = null,
        correctionInsulin = 0.0,
        rawTotalInsulin = 0.0,
        roundedTotalInsulin = 0.0,
        roundingStep = 0.5,
        isHypoRisk = false,
        advisoryNote = "Bereit für Eingabe"
    ),
    val snackbarMessage: String? = null,
    val activeTab: Int = 0 // 0: Rechner, 1: KI-Schätzer, 2: Tagebuch, 3: Einstellungen
)

sealed interface AiEstimateState {
    object Idle : AiEstimateState
    object Loading : AiEstimateState
    data class Success(val result: MealEstimateResult) : AiEstimateState
    data class Error(val message: String) : AiEstimateState
}

class InsulinCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InsulinRepository
    private val geminiService = GeminiMealService()
    private val themePreferences = ThemePreferences(application)
    private var cachedSettings: UserSettings = UserSettings()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = InsulinRepository(db.calculationLogDao(), db.userSettingsDao())
    }

    val historyLogs: StateFlow<List<CalculationLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow<AiEstimateState>(AiEstimateState.Idle)
    val aiState: StateFlow<AiEstimateState> = _aiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settingsNullable ->
                val settings = settingsNullable ?: UserSettings()
                cachedSettings = settings
                // Sync to ThemePreferences
                themePreferences.savePreferences(
                    settings.selectedTheme,
                    settings.themeMode
                )
                val gUnit = GlucoseUnit.fromString(settings.glucoseUnit)
                val initialTime = if (_uiState.value.isAutoTimeDetection) TimeOfDay.current() else _uiState.value.selectedTimeOfDay
                val unit = when (settings.defaultCarbUnit) {
                    "BE" -> CarbUnit.BE
                    "KE" -> CarbUnit.KE
                    else -> CarbUnit.GRAMS
                }
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
                        selectedUnit = if (current.carbInput.isEmpty()) unit else current.selectedUnit,
                        glucoseUnit = gUnit,
                        targetGlucoseInput = if (current.targetGlucoseInput.isEmpty() || current.targetGlucoseInput == "120") targetStr else current.targetGlucoseInput,
                        correctionFactorInput = if (current.correctionFactorInput.isEmpty() || current.correctionFactorInput == "50") corrStr else current.correctionFactorInput,
                        selectedTimeOfDay = initialTime
                    )
                }
                recalculate(settings)
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun onCarbInputChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        // Prevent multiple decimal dots
        val dotsCount = sanitized.count { it == '.' }
        if (dotsCount <= 1 && sanitized.length <= 8) {
            _uiState.update { it.copy(carbInput = sanitized) }
            recalculate()
        }
    }

    fun addCarbs(amount: Double) {
        val current = _uiState.value.carbInput.toDoubleOrNull() ?: 0.0
        val updated = (current + amount).coerceAtLeast(0.0)
        _uiState.update {
            it.copy(carbInput = if (updated % 1.0 == 0.0) updated.toInt().toString() else "%.1f".format(Locale.getDefault(), updated))
        }
        recalculate()
    }

    fun clearCarbs() {
        _uiState.update { it.copy(carbInput = "0") }
        recalculate()
    }

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

    fun setUnit(unit: CarbUnit) {
        // Keeps raw input value unchanged as requested ("die eingabe soll sich nicht ändern, wenn umgeschalten wird")
        _uiState.update { it.copy(selectedUnit = unit) }
        recalculate()
    }

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

    fun adjustFactor(delta: Double) {
        val currentFactor = getEffectiveFactor()
        val updated = BigDecimal(currentFactor + delta).setScale(2, RoundingMode.HALF_UP).toDouble()
        val clamped = updated.coerceIn(0.05, 10.00)
        _uiState.update { it.copy(factorOverride = clamped) }
        recalculate()
    }

    fun dismissDisclaimer() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = userSettings.value ?: cachedSettings
            currentSettings.isShowDisclaimer = false
            repository.saveSettings(currentSettings)
        }
    }

    fun toggleCorrection() {
        _uiState.update { it.copy(showCorrection = !it.showCorrection) }
        recalculate()
    }

    fun onGlucoseInputChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(currentGlucoseInput = sanitized) }
        recalculate()
    }

    fun onTargetGlucoseChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(targetGlucoseInput = sanitized) }
        recalculate()
    }

    fun onCorrectionFactorChange(newInput: String) {
        val sanitized = newInput.replace(',', '.').filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(correctionFactorInput = sanitized) }
        recalculate()
    }

    fun onMealTitleChange(title: String) {
        _uiState.update { it.copy(mealTitle = title) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    private fun getEffectiveFactor(settings: UserSettings = cachedSettings): Double {
        val state = _uiState.value
        if (state.factorOverride != null) {
            return state.factorOverride
        }
        val effectiveSettings = if (settings.id != 0 || settings != UserSettings()) settings else (userSettings.value ?: cachedSettings)
        return getFactorForTime(state.selectedTimeOfDay, effectiveSettings)
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
        val rawInput = state.carbInput.toDoubleOrNull() ?: 0.0
        val settings = passedSettings ?: cachedSettings
        val beDivisor = settings.beGramsDivisor.toDouble().let { if (it > 0.0) it else 12.0 }
        
        val grams = when (state.selectedUnit) {
            CarbUnit.GRAMS -> rawInput
            CarbUnit.KE -> rawInput * 10.0
            CarbUnit.BE -> rawInput * beDivisor
        }

        val ke = InsulinMathEngine.calculateKe(grams)
        val be = InsulinMathEngine.calculateBe(grams)

        val factor = getEffectiveFactor(settings)
        val mealInsulin = InsulinMathEngine.calculateMealInsulin(
            rawInput,
            state.selectedUnit.shortName,
            grams,
            factor
        )

        var correctionInsulin = 0.0
        val currentBg = state.currentGlucoseInput.toDoubleOrNull()
        val targetBg = state.targetGlucoseInput.toDoubleOrNull()
        val corrFactor = state.correctionFactorInput.toDoubleOrNull()

        var isHypoRisk = false
        var advisory = "Standard-Dosis für die Mahlzeit"
        val gUnit = state.glucoseUnit
        val isMmol = (gUnit == GlucoseUnit.MMOL_L)

        if (state.showCorrection && currentBg != null && targetBg != null && corrFactor != null && corrFactor > 0) {
            if (InsulinMathEngine.isHypoglycemia(currentBg, isMmol)) {
                isHypoRisk = true
                advisory = "Achtung: Niedriger Blutzucker (< ${if (isMmol) "3.9 mmol/l" else "70 mg/dl"})! Bitte zuerst 1-2 KE schnelle KH (z.B. Traubenzucker/Saft) einnehmen."
            } else if (currentBg < targetBg) {
                val diff = targetBg - currentBg
                correctionInsulin = - (diff / corrFactor)
                advisory = "Blutzucker unter Zielbereich: Korrektur reduziert Gesamtdosis."
            } else if (currentBg > targetBg) {
                val diff = currentBg - targetBg
                correctionInsulin = diff / corrFactor
                advisory = "Erhöhter Blutzucker: Korrektur-Bolus addiert."
            }
        }

        val rawTotal = (mealInsulin + correctionInsulin).coerceAtLeast(0.0)
        val roundingStep = settings.roundingStep
        val roundedTotal = InsulinMathEngine.roundToStep(rawTotal, roundingStep)

        _uiState.update {
            it.copy(
                calculationSummary = CalculationSummary(
                    carbGrams = InsulinMathEngine.roundToDecimals(grams, 1),
                    keValue = InsulinMathEngine.roundToDecimals(ke, 2),
                    beValue = InsulinMathEngine.roundToDecimals(be, 2),
                    factorUsed = factor,
                    mealInsulin = InsulinMathEngine.roundToDecimals(mealInsulin, 2),
                    bloodGlucoseInput = currentBg,
                    targetGlucose = targetBg,
                    correctionInsulin = InsulinMathEngine.roundToDecimals(correctionInsulin, 2),
                    rawTotalInsulin = InsulinMathEngine.roundToDecimals(rawTotal, 2),
                    roundedTotalInsulin = roundedTotal,
                    roundingStep = roundingStep,
                    isHypoRisk = isHypoRisk,
                    advisoryNote = advisory
                )
            )
        }
    }

    fun saveCalculationToLog(notesOverride: String? = null) {
        // Capture full snapshot synchronously at moment of invocation
        val state = _uiState.value
        val summary = state.calculationSummary
        val autoMealTitle = state.mealTitle.ifBlank {
            "${state.selectedTimeOfDay.title} (${summary.carbGrams}g KH)"
        }
        val finalNotes = notesOverride ?: state.notes

        val log = CalculationLog(
            0L,
            System.currentTimeMillis(),
            autoMealTitle,
            state.carbInput.toDoubleOrNull() ?: 0.0,
            state.selectedUnit.shortName,
            summary.carbGrams,
            summary.beValue,
            summary.keValue,
            state.selectedTimeOfDay.title,
            summary.factorUsed,
            summary.mealInsulin,
            summary.bloodGlucoseInput,
            summary.targetGlucose,
            state.correctionFactorInput.toDoubleOrNull(),
            summary.correctionInsulin,
            summary.rawTotalInsulin,
            summary.roundedTotalInsulin,
            finalNotes
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCalculation(log)
            _uiState.update { it.copy(snackbarMessage = "Berechnung erfolgreich im Tagebuch gespeichert!") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLog(logId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
            _uiState.update { it.copy(snackbarMessage = "Tagebuch wurde geleert.") }
        }
    }

    suspend fun getAllLogsDirect(): List<CalculationLog> = withContext(Dispatchers.IO) {
        repository.allLogsDirect
    }

    suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        val settings = repository.settings
        val logs = repository.allLogsDirect
        DatabaseBackupManager(repository.userSettingsDao(), repository.calculationLogDao()).exportToJson(settings, logs)
    }

    suspend fun exportCsvBackup(): String = withContext(Dispatchers.IO) {
        val logs = repository.allLogsDirect
        DatabaseBackupManager(repository.userSettingsDao(), repository.calculationLogDao()).exportToCsv(logs)
    }

    suspend fun importBackupContent(content: String): ImportResult = withContext(Dispatchers.IO) {
        val manager = DatabaseBackupManager(repository.userSettingsDao(), repository.calculationLogDao())
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

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            cachedSettings = settings
            repository.saveSettings(settings)
            // Persist theme choice synchronously in SharedPreferences to prevent start-up flicker
            themePreferences.savePreferences(
                settings.selectedTheme,
                settings.themeMode
            )
            val gUnit = GlucoseUnit.fromString(settings.glucoseUnit)
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
                    glucoseUnit = gUnit,
                    targetGlucoseInput = targetStr,
                    correctionFactorInput = corrStr,
                    snackbarMessage = "Einstellungen gespeichert!"
                )
            }
            recalculate(settings)
        }
    }

    // Gemini AI Meal Estimation
    fun estimateMealCarbs(description: String) {
        if (description.isBlank()) return
        _aiState.value = AiEstimateState.Loading
        viewModelScope.launch {
            val settings = userSettings.value ?: cachedSettings
            val result = geminiService.estimateCarbsFromDescription(
                foodDescription = description,
                customApiKey = settings.geminiApiKey,
                modelId = settings.selectedAiModel
            )
            result.onSuccess { data ->
                _aiState.value = AiEstimateState.Success(data)
            }.onFailure { err ->
                _aiState.value = AiEstimateState.Error(err.message ?: "Fehler bei der KI-Analyse")
            }
        }
    }

    fun saveAiConfiguration(apiKey: String, modelId: String) {
        viewModelScope.launch {
            val current = userSettings.value ?: cachedSettings
            val updated = current.copy()
            updated.geminiApiKey = apiKey.trim()
            updated.selectedAiModel = modelId
            updateUserSettings(updated)
        }
    }

    fun applyEstimatedCarbsToCalculator(carbsGrams: Double, mealName: String) {
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
                activeTab = 0 // Switch back to calculator
            )
        }
        recalculate()
    }
}
