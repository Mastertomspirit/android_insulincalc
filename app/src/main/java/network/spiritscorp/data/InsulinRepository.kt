package network.spiritscorp.data

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

import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth repository managing data access for calculation logs
 * and personalized insulin therapy settings.
 *
 * Encapsulates asynchronous queries, reactive state streams, and persistence routines.
 *
 * @property calculationLogDao Data access object for historical calculation entries.
 * @property userSettingsDao Data access object for therapy configuration and preferences.
 */
class InsulinRepository(
    private val calculationLogDao: CalculationLogDao,
    private val userSettingsDao: UserSettingsDao
) {
    /**
     * Cold [Flow] emitting all historical calculation logs ordered chronologically descending.
     */
    val allLogs: Flow<List<CalculationLog>> = calculationLogDao.getAllLogs()

    /**
     * Cold [Flow] emitting current [UserSettings] updates reactively.
     */
    val settingsFlow: Flow<UserSettings?> = userSettingsDao.getSettings()

    /**
     * Retrieves the current user settings or initializes and persists default settings if none exist yet.
     *
     * @return The active [UserSettings] configuration.
     */
    suspend fun getSettings(): UserSettings {
        return userSettingsDao.getSettingsDirect() ?: UserSettings().also {
            userSettingsDao.saveSettings(it)
        }
    }

    /**
     * Persists updated user settings into the database.
     *
     * @param settings The new settings to save.
     */
    suspend fun saveSettings(settings: UserSettings) {
        userSettingsDao.saveSettings(settings)
    }

    /**
     * Retrieves a non-reactive snapshot list of all calculation logs.
     *
     * @return List of all [CalculationLog] entries.
     */
    suspend fun getAllLogsDirect(): List<CalculationLog> {
        return calculationLogDao.getAllLogsDirect()
    }

    /**
     * Inserts a new calculation log into the database.
     *
     * @param log The calculation log to persist.
     * @return The database ID of the newly inserted record.
     */
    suspend fun saveCalculation(log: CalculationLog): Long {
        return calculationLogDao.insertLog(log)
    }

    /**
     * Inserts a batch of calculation logs (used during data import/restore).
     *
     * @param logs List of calculation logs.
     * @return List of generated row IDs.
     */
    suspend fun saveLogs(logs: List<CalculationLog>): List<Long> {
        return calculationLogDao.insertLogs(logs)
    }

    /**
     * Deletes a specific calculation log by its ID.
     *
     * @param logId Database primary key ID of the log entry.
     */
    suspend fun deleteLog(logId: Long) {
        calculationLogDao.deleteLogById(logId)
    }

    /**
     * Deletes all calculation logs from the database.
     */
    suspend fun clearLogs() {
        calculationLogDao.clearAllLogs()
    }
}
