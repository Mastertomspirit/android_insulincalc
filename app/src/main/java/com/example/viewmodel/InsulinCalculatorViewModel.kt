package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiMealService
import com.example.ai.MealEstimateResult
import com.example.data.AppDatabase
import com.example.data.InsulinRepository
import com.example.model.CalculationLog
import com.example.model.CalculationSummary
import com.example.model.CarbUnit
import com.example.model.GlucoseUnit
import com.example.model.TimeOfDay
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

data class CalculatorUiState(
    val carbInput: String = "4.0",
    val selectedUnit: CarbUnit = CarbUnit.BE,
    val selectedTimeOfDay: TimeOfDay = TimeOfDay.current(),
    val factorOverride: Double? = null,
    val isAutoTimeDetection: Boolean = true,
    val currentGlucoseInput: String = "",
    val targetGlucoseInput: String = "100",
    val correctionFactorInput: String = "40",
    val showCorrection: Boolean = false,
    val mealTitle: String = "",
    val calculationSummary: CalculationSummary = CalculationSummary(
        carbGrams = 48.0,
        keValue = 4.80,
        beValue = 4.00,
        factorUsed = 1.50,
        mealInsulin = 7.20,
        bloodGlucoseInput = null,
        targetGlucose = null,
        correctionInsulin = 0.0,
        rawTotalInsulin = 7.20,
        roundedTotalInsulin = 7.0,
        roundingStep = 0.5,
        isHypoRisk = false,
        advisoryNote = "Reguläre Bolusberechnung"
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

    init {
        val db = AppDatabase.getDatabase(application)
        repository = InsulinRepository(db.calculationLogDao(), db.userSettingsDao())
    }

    val historyLogs: StateFlow<List<CalculationLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow<AiEstimateState>(AiEstimateState.Idle)
    val aiState: StateFlow<AiEstimateState> = _aiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.getSettings()
            _uiState.update { current ->
                val initialTime = TimeOfDay.current()
                val unit = when (settings.defaultCarbUnit) {
                    "BE" -> CarbUnit.BE
                    "KE" -> CarbUnit.KE
                    else -> CarbUnit.GRAMS
                }
                current.copy(
                    selectedUnit = unit,
                    carbInput = if (unit == CarbUnit.BE) "4.0" else if (unit == CarbUnit.KE) "5.0" else "50",
                    targetGlucoseInput = settings.targetGlucoseMgDl.toString().replace(".0", ""),
                    correctionFactorInput = settings.correctionFactorMgDl.toString().replace(".0", ""),
                    selectedTimeOfDay = initialTime
                )
            }
            recalculate()
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
            it.copy(carbInput = if (updated % 1.0 == 0.0) updated.toInt().toString() else "%.1f".format(java.util.Locale.US, updated))
        }
        recalculate()
    }

    fun clearCarbs() {
        _uiState.update { it.copy(carbInput = "0") }
        recalculate()
    }

    fun setUnit(unit: CarbUnit) {
        val currentInput = _uiState.value.carbInput.toDoubleOrNull() ?: 0.0
        val oldUnit = _uiState.value.selectedUnit
        val grams = oldUnit.toGrams(currentInput)
        val converted = unit.fromGrams(grams)
        val formatted = if (unit == CarbUnit.GRAMS) {
            converted.toInt().toString()
        } else {
            "%.1f".format(java.util.Locale.US, converted)
        }
        _uiState.update { it.copy(selectedUnit = unit, carbInput = formatted) }
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

    private fun getEffectiveFactor(): Double {
        val state = _uiState.value
        if (state.factorOverride != null) {
            return state.factorOverride
        }
        val settings = userSettings.value ?: UserSettings()
        return getFactorForTime(state.selectedTimeOfDay, settings)
    }

    private fun getFactorForTime(timeOfDay: TimeOfDay, settings: UserSettings): Double {
        return when (timeOfDay) {
            TimeOfDay.MORNING -> settings.morningFactor
            TimeOfDay.NOON -> settings.noonFactor
            TimeOfDay.EVENING -> settings.eveningFactor
            TimeOfDay.NIGHT -> settings.nightFactor
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val rawInput = state.carbInput.toDoubleOrNull() ?: 0.0
        val grams = state.selectedUnit.toGrams(rawInput)

        val ke = grams / 10.0
        val be = grams / 12.0

        val factor = getEffectiveFactor()
        val mealInsulin = ke * factor

        var correctionInsulin = 0.0
        val currentBg = state.currentGlucoseInput.toDoubleOrNull()
        val targetBg = state.targetGlucoseInput.toDoubleOrNull()
        val corrFactor = state.correctionFactorInput.toDoubleOrNull()

        var isHypoRisk = false
        var advisory = "Standard-Dosis für die Mahlzeit"

        if (state.showCorrection && currentBg != null && targetBg != null && corrFactor != null && corrFactor > 0) {
            if (currentBg < 70) {
                isHypoRisk = true
                advisory = "Achtung: Niedriger Blutzucker! Bitte zuerst 1-2 KE schnelle KH (z.B. Traubenzucker/Saft) einnehmen."
            } else if (currentBg < targetBg) {
                val diff = targetBg - currentBg
                // Negative correction
                correctionInsulin = - (diff / corrFactor)
                advisory = "Blutzucker unter Zielbereich: Korrektur reduziert Gesamtdosis."
            } else if (currentBg > targetBg) {
                val diff = currentBg - targetBg
                correctionInsulin = diff / corrFactor
                advisory = "Erhöhter Blutzucker: Korrektur-Bolus addiert."
            }
        }

        val rawTotal = (mealInsulin + correctionInsulin).coerceAtLeast(0.0)
        val roundingStep = (userSettings.value ?: UserSettings()).roundingStep

        val roundedTotal = roundToStep(rawTotal, roundingStep)

        _uiState.update {
            it.copy(
                calculationSummary = CalculationSummary(
                    carbGrams = roundToDecimals(grams, 1),
                    keValue = roundToDecimals(ke, 2),
                    beValue = roundToDecimals(be, 2),
                    factorUsed = factor,
                    mealInsulin = roundToDecimals(mealInsulin, 2),
                    bloodGlucoseInput = currentBg,
                    targetGlucose = targetBg,
                    correctionInsulin = roundToDecimals(correctionInsulin, 2),
                    rawTotalInsulin = roundToDecimals(rawTotal, 2),
                    roundedTotalInsulin = roundedTotal,
                    roundingStep = roundingStep,
                    isHypoRisk = isHypoRisk,
                    advisoryNote = advisory
                )
            )
        }
    }

    private fun roundToStep(value: Double, step: Double): Double {
        if (step <= 0.0) return roundToDecimals(value, 2)
        val factor = 1.0 / step
        return BigDecimal(Math.round(value * factor) / factor)
            .setScale(if (step == 0.1) 1 else if (step == 0.5) 1 else 0, RoundingMode.HALF_UP)
            .toDouble()
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        return BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
    }

    fun saveCalculationToLog(notes: String = "") {
        viewModelScope.launch {
            val state = _uiState.value
            val summary = state.calculationSummary
            val autoMealTitle = if (state.mealTitle.isNotBlank()) {
                state.mealTitle
            } else {
                "${state.selectedTimeOfDay.title} (${summary.carbGrams}g KH)"
            }

            val log = CalculationLog(
                mealTitle = autoMealTitle,
                rawCarbInput = state.carbInput.toDoubleOrNull() ?: 0.0,
                carbUnit = state.selectedUnit.shortName,
                carbGrams = summary.carbGrams,
                timeOfDay = state.selectedTimeOfDay.title,
                insulinFactor = summary.factorUsed,
                mealInsulin = summary.mealInsulin,
                bloodGlucose = summary.bloodGlucoseInput,
                targetGlucose = summary.targetGlucose,
                correctionFactor = state.correctionFactorInput.toDoubleOrNull(),
                correctionInsulin = summary.correctionInsulin,
                totalInsulin = summary.rawTotalInsulin,
                roundedInsulin = summary.roundedTotalInsulin,
                notes = notes
            )
            repository.saveCalculation(log)
            _uiState.update { it.copy(snackbarMessage = "Berechnung erfolgreich im Tagebuch gespeichert!") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteLog(logId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _uiState.update { it.copy(snackbarMessage = "Tagebuch wurde geleert.") }
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            recalculate()
            _uiState.update { it.copy(snackbarMessage = "Einstellungen gespeichert!") }
        }
    }

    // Gemini AI Meal Estimation
    fun estimateMealCarbs(description: String) {
        if (description.isBlank()) return
        _aiState.value = AiEstimateState.Loading
        viewModelScope.launch {
            val result = geminiService.estimateCarbsFromDescription(description)
            result.onSuccess { data ->
                _aiState.value = AiEstimateState.Success(data)
            }.onFailure { err ->
                _aiState.value = AiEstimateState.Error(err.message ?: "Fehler bei der KI-Analyse")
            }
        }
    }

    fun applyEstimatedCarbsToCalculator(carbsGrams: Double, mealName: String) {
        val currentUnit = _uiState.value.selectedUnit
        val converted = currentUnit.fromGrams(carbsGrams)
        val formatted = if (currentUnit == CarbUnit.GRAMS) {
            converted.toInt().toString()
        } else {
            "%.1f".format(java.util.Locale.US, converted)
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
