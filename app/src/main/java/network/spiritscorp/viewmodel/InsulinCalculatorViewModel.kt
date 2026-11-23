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
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.spiritscorp.R
import network.spiritscorp.ai.GeminiAiModel
import network.spiritscorp.data.AppDatabase
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.data.DatabaseRestoreManager
import network.spiritscorp.data.ImportResult
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import network.spiritscorp.preferences.DisclaimerPreferences
import network.spiritscorp.preferences.ThemePreferences
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

    private val _databaseError = MutableStateFlow(repository == null || initialDatabaseError)
    private val _mainUiState = MutableStateFlow(MainUiState())
    private val themePreferences = ThemePreferences(application)
    private val disclaimerPreferences = DisclaimerPreferences(application)

    init {
        if (initialDatabaseError || repository == null) {
            _databaseError.value = true
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                repository?.loadSettings()
            }
        }
    }

    /** StateFlow exposing the list of past calculation logs for the history / logbook. */
    internal var historyLogs: StateFlow<List<CalculationLog>> = (repository?.allLogs ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** StateFlow exposing current user settings (insulin factors, targets, etc.). */
    internal var userSettings: StateFlow<UserSettings> = (repository?.settingsFlow ?: flowOf(null))
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private var cachedSettings = userSettings.value

    internal val calculatorMainView = CalculatorMainView(
        application = application,
        beGramsDivisor = {cachedSettings.beGramsDivisor},
        roundingStep = {cachedSettings.roundingStep},
        onTimeOfDay =  { actualTime: TimeOfDay -> getFactorForTime(actualTime, cachedSettings) },
    )

    internal val logMainView = LogMainView(
        application = application,
        repository = {repository},
        calculatorUiState = {calculatorMainView.calculatorUiState.value},
        onShowToast = ::showToast
    )

    internal val aiViewModel = AiViewModel(
        application = application,
        geminiAiModel = {cachedSettings.selectedAiModel},
        geminiApiKey = {cachedSettings.geminiApiKey},
        onUpdateSettings = {
            geminiAiModel: GeminiAiModel, geminiApiKey: String ->
                viewModelScope.launch {
                    cachedSettings.geminiApiKey = geminiApiKey.trim()
                    cachedSettings.selectedAiModel = geminiAiModel
                    updateUserSettings()
            }
        },
        onApplyEstimateToCalculator = {
            formattedCarbs: String, mealName: String, notesText: String ->
                calculatorMainView.onCarbInputChange(formattedCarbs)
                calculatorMainView.onMealTitleChange(mealName)
                calculatorMainView.onNotesChange(notesText)
                setTab(0)
        },
    )

    /** Public read-only StateFlow indicating whether a database initialization or access error has occurred. */
    internal val databaseError: StateFlow<Boolean> = _databaseError.asStateFlow()

    /** Public read-only StateFlow representing the calculator UI state. */
    internal val mainUiState: StateFlow<MainUiState> = _mainUiState.asStateFlow()

    init {
        viewModelScope.launch {
            (repository?.settingsFlow ?: flowOf(null)).collect { settingsNullable ->
                val settings = settingsNullable ?: UserSettings()
                cachedSettings = settings
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
                calculatorMainView.selectTimeOfDay(TimeOfDay.current())
                _mainUiState.update { current ->
                    current.copy(
                        showDisclaimerBanner = disclaimerPreferences.shouldShowDisclaimer()
                    )
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            cachedSettings.isShowDisclaimer = false
            repository?.saveSettings(cachedSettings)
        }
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

    private fun getFactorForTime(timeOfDay: TimeOfDay, settings: UserSettings): Double {
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
            if(db == null){
                initialDatabaseError = true
                return null
            }else{
                initialDatabaseError = false
                return db
            }
        }
    }

    /**
     * Exports all user settings and calculation logs to a JSON backup string.
     *
     * @return JSON string representing the database backup.
     */
    suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        val repo = repository ?: return@withContext ""
        val settings = repo.loadSettings()
        val logs = repo.allLogsDirect
        DatabaseBackupManager(repo.userSettingsDao(), repo.calculationLogDao()).exportToJson(settings, logs)
    }

    /**
     * Exports all calculation logs to a CSV backup string.
     *
     * @return CSV string representing the calculation history.
     */
    suspend fun exportCsvBackup(): String = withContext(Dispatchers.IO) {
        val repo = repository ?: return@withContext ""
        val logs = repo.allLogsDirect
        DatabaseBackupManager(repo.userSettingsDao(), repo.calculationLogDao()).exportToCsv(logs)
    }

    /**
     * Resets the database file and passphrase, reinstantiates [AppDatabase],
     * updates the internal repository, and clears the database error state.
     */
    internal fun resetDatabase() {
        try {
            DatabaseRestoreManager().resetInstance(getApplication())
            val newDb = AppDatabase.getDatabase(getApplication())
            repository = InsulinRepository(newDb.calculationLogDao(), newDb.userSettingsDao())
            _databaseError.value = false
            initialDatabaseError = false
            if (repository != null) {
                userSettings = (repository?.settingsFlow ?: flowOf(null))
                    .map { it ?: UserSettings() }
                    .stateIn( viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
                historyLogs = (repository?.allLogs ?: flowOf(emptyList()))
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
                val settings = repository?.loadSettings()
                if (settings != null) {
                    cachedSettings = settings
                }
            }
        } catch (e: Throwable) {
            e.message?.let { Log.e("Test-Tag", it) }
            _databaseError.value = true
        }
    }

    /** Signals a database error state to display the recovery dialog. */
    fun notifyDatabaseError() {
        _databaseError.value = true
    }

    /** Clears the active database error state. */
    fun clearDatabaseError() {
        _databaseError.value = false
    }

    /**
     * Imports backup content (auto-detecting JSON or CSV format) into the database.
     *
     * @param content Raw string content of the backup file.
     * @return [ImportResult] containing success status and statistics.
     */
    suspend fun importBackupContent(content: String): ImportResult = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        if (repository == null) {
            try {
                val db = AppDatabase.getDatabase(app)
                repository = InsulinRepository(db.calculationLogDao(), db.userSettingsDao())
            } catch (e: Throwable) {
                return@withContext ImportResult(false, 0, false, e.message ?: "Database error")
            }
        }
        val manager = DatabaseBackupManager(app)
        val trimmed = content.trim()
        val result = if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            manager.importFromJson(trimmed)
        } else {
            manager.importFromCsv(trimmed)
        }
        if (result.isSuccess) {
            _databaseError.value = false
            initialDatabaseError = false
            if (result.isImportedSettings) {
                val updated = repository?.loadSettings()
                if (updated != null) {
                    cachedSettings = updated
                    themePreferences.savePreferences(
                        cachedSettings.selectedTheme ?: themePreferences.getSelectedTheme(),
                        cachedSettings.themeMode ?: themePreferences.getThemeMode()
                    )
                }
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
        updateUserSettings()
    }
    fun updateUserSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.saveSettings(cachedSettings)
            // Persist theme choice synchronously in SharedPreferences to prevent start-up flicker
            themePreferences.savePreferences(
                cachedSettings.selectedTheme ?: themePreferences.getSelectedTheme(),
                cachedSettings.themeMode ?: themePreferences.getThemeMode()
            )
        }
        val gUnit = cachedSettings.glucoseUnit
        val targetStr = if (gUnit == GlucoseUnit.MMOL_L) {
            val mmol = GlucoseUnit.MMOL_L.fromMgDl(cachedSettings.targetGlucoseMgDl)
            if (mmol % 1.0 == 0.0) mmol.toInt().toString() else "%.1f".format(Locale.getDefault(), mmol)
        } else {
            cachedSettings.targetGlucoseMgDl.toString().replace(".0", "")
        }
        val corrStr = if (gUnit == GlucoseUnit.MMOL_L) {
            val mmol = GlucoseUnit.MMOL_L.fromMgDl(cachedSettings.correctionFactorMgDl)
            if (mmol % 1.0 == 0.0) mmol.toInt().toString() else "%.1f".format(Locale.getDefault(), mmol)
        } else {
            cachedSettings.correctionFactorMgDl.toString().replace(".0", "")
        }

        calculatorMainView.setCarbUnit(cachedSettings.defaultCarbUnit)
        calculatorMainView.setGlucoseUnit(gUnit)
        calculatorMainView.onTargetGlucoseChange(targetStr)
        calculatorMainView.onCorrectionFactorChange(corrStr)
        showToast(getApplication<Application>().getString(R.string.view_model_settings_saved_toast))
    }
}
