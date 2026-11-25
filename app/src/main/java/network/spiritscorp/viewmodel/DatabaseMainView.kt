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
import androidx.annotation.VisibleForTesting
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
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.UserSettings
import network.spiritscorp.preferences.ThemePreferences
import java.util.Locale

/**
 * ViewModel dedicated to managing database status, database recovery, user settings persistence, and JSON/CSV backup exports and imports.
 *
 * @param application The Android application context.
 * @param repositoryProvider Supplier function returning the active [InsulinRepository] or null.
 * @param setCalculator Callback function to update calculator settings.
 * @param onShowToast Callback function to display toast notification messages.
 */
internal class DatabaseMainView(
    application: Application,
    private var initialDatabaseError: () -> Boolean,
    private var repositoryProvider: () -> InsulinRepository?,
    private val setCalculator: (CarbUnit, GlucoseUnit, String, String) -> Unit,
    private val onShowToast: (String) -> Unit,
) : AndroidViewModel(application) {


    /**
     * Application context reference for theme and resource loading.
     */
    private val themePreferences = ThemePreferences(application)

    /**
     * Mutable backing StateFlow for database error status [databaseError]. True if the repository is null or initial database checks failed.
     */
    private val _databaseError = MutableStateFlow(repositoryProvider() == null || initialDatabaseError())

    /** 
     * StateFlow exposing current user settings (insulin factors, targets, carb/glucose units, theme preferences, etc.).
     * Emits the latest [UserSettings] from the repository or falls back to default settings.
     */
    private var userSettings: StateFlow<UserSettings> = (repositoryProvider()?.settingsFlow ?: flowOf(null))
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    /**
     * Mutable backing StateFlow for the currently cached [UserSettings] snapshot ([cachedSettings]).
     */
    private val _cachedSettings: MutableStateFlow<UserSettings> = MutableStateFlow(userSettings.value)

    /** Public read-only StateFlow indicating whether a database initialization or access error has occurred. */
    internal val databaseError: StateFlow<Boolean> = _databaseError.asStateFlow()

    /** 
     * StateFlow exposing the list of past calculation logs for the history / logbook view.
     * Emits a list of [CalculationLog] items observed from the database repository.
     */
    internal var historyLogs: StateFlow<List<CalculationLog>> = (repositoryProvider()?.allLogs ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Public read-only StateFlow exposing the currently cached [UserSettings] snapshot. */
    internal val cachedSettings: StateFlow<UserSettings> = _cachedSettings.asStateFlow()

    init {
        if (initialDatabaseError() || repositoryProvider() == null) {
            _databaseError.value = true
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                repositoryProvider()?.loadSettings()
            }
            viewModelScope.launch {
                userSettings.collect { settings ->
                    _cachedSettings.value = settings
                }
            }
        }
    }

    /**
     * Exports all user settings and calculation logs to a JSON backup string.
     *
     * @return JSON string representing the database backup.
     */
    internal suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        val repo = repositoryProvider() ?: return@withContext ""
        val settings = repo.loadSettings()
        val logs = repo.allLogsDirect
        DatabaseBackupManager(repo.userSettingsDao(), repo.calculationLogDao()).exportToJson(settings, logs)
    }


    /**
     * Exports all calculation logs to a CSV backup string.
     *
     * @return CSV string representing the calculation history.
     */
    internal suspend fun exportCsvBackup(): String = withContext(Dispatchers.IO) {
        val repo = repositoryProvider() ?: return@withContext ""
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
            repositoryProvider = { InsulinRepository(newDb.calculationLogDao(), newDb.userSettingsDao()) }
            _databaseError.value = false
            initialDatabaseError = { false }
            if (repositoryProvider() != null) {
                userSettings = (repositoryProvider()?.settingsFlow ?: flowOf(null))
                    .map { it ?: UserSettings() }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
                historyLogs = (repositoryProvider()?.allLogs ?: flowOf(emptyList()))
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
                val settings = repositoryProvider()?.loadSettings()
                if (settings != null) {
                    _cachedSettings.value = settings
                }
            }
        } catch (e: Throwable) {
            e.message?.let { Log.e("Test-Tag", it) }
            _databaseError.value = true
        }
    }

    /**
     * Imports backup content (auto-detecting JSON or CSV format) into the database.
     *
     * @param content Raw string content of the backup file.
     * @return [ImportResult] containing success status and statistics.
     */
    internal suspend fun importBackupContent(content: String): ImportResult = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        if (repositoryProvider() == null) {
            try {
                val db = AppDatabase.getDatabase(app)
                repositoryProvider = { InsulinRepository(db.calculationLogDao(), db.userSettingsDao()) }
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
            initialDatabaseError = { false }
            if (result.isImportedSettings) {
                val updated = repositoryProvider()?.loadSettings()
                if (updated != null) {
                    _cachedSettings.value = updated
                    themePreferences.savePreferences(
                        _cachedSettings.value.selectedTheme ?: themePreferences.getSelectedTheme(),
                        _cachedSettings.value.themeMode ?: themePreferences.getThemeMode()
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
    internal fun updateUserSettings(settings: UserSettings) {
        _cachedSettings.value = settings
        updateUserSettings()
    }

    /**
     * Persists updated user settings to the database and refreshes active state and calculations. Uses the currently cached [UserSettings] instance.
     */
    internal fun updateUserSettings() {
        val currentSettings = _cachedSettings.value.copy()
        viewModelScope.launch(Dispatchers.IO) {
            repositoryProvider()?.saveSettings(currentSettings)
            // Persist theme choice synchronously in SharedPreferences to prevent start-up flicker
            themePreferences.savePreferences(
                currentSettings.selectedTheme ?: themePreferences.getSelectedTheme(),
                currentSettings.themeMode ?: themePreferences.getThemeMode()
            )
        }
        val gUnit = currentSettings.glucoseUnit
        val targetStr = if (gUnit == GlucoseUnit.MMOL_L) {
            val mmol = GlucoseUnit.MMOL_L.fromMgDl(currentSettings.targetGlucoseMgDl)
            if (mmol % 1.0 == 0.0) mmol.toInt().toString() else "%.1f".format(Locale.getDefault(), mmol)
        } else {
            currentSettings.targetGlucoseMgDl.toString().replace(".0", "")
        }
        val corrStr = if (gUnit == GlucoseUnit.MMOL_L) {
            val mmol = GlucoseUnit.MMOL_L.fromMgDl(currentSettings.correctionFactorMgDl)
            if (mmol % 1.0 == 0.0) mmol.toInt().toString() else "%.1f".format(Locale.getDefault(), mmol)
        } else {
            currentSettings.correctionFactorMgDl.toString().replace(".0", "")
        }
        setCalculator(currentSettings.defaultCarbUnit, gUnit, targetStr, corrStr)
        onShowToast(getApplication<Application>().getString(R.string.view_model_settings_saved_toast))
    }

    /**
     * Updates Gemini AI configuration (API key and model choice) in user settings.
     *
     * @param geminiAiModel Selected [GeminiAiModel].
     * @param geminiApiKey  Gemini API key string.
     */
    internal fun updateGeminiSettings(geminiAiModel: GeminiAiModel, geminiApiKey: String) {
        val updated = userSettings.value.copy()
        updated.geminiApiKey = geminiApiKey.trim()
        updated.selectedAiModel = geminiAiModel
        updateUserSettings(updated)
    }

    /**
     * Updates the medical disclaimer acceptance flag in user settings.
     *
     * @param showDisclaimer Disclaimer visibility flag.
     */

    internal fun updateDisclaimerOnDatabase(showDisclaimer: Boolean) {
        val updated = userSettings.value.copy()
        updated.isShowDisclaimer = showDisclaimer
        viewModelScope.launch(Dispatchers.IO) {
            repositoryProvider()?.saveSettings(updated)
        }
    }

    /** Signals a database error state to display the recovery dialog. */
    @VisibleForTesting
    internal fun notifyDatabaseError() {
        _databaseError.value = true
    }

    /** Clears the active database error state. */
    @VisibleForTesting
    internal fun clearDatabaseError() {
        _databaseError.value = false
        initialDatabaseError = { false }
    }
}
