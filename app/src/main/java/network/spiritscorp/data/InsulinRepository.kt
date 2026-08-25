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

class InsulinRepository(
    private val calculationLogDao: CalculationLogDao,
    private val userSettingsDao: UserSettingsDao
) {
    val allLogs: Flow<List<CalculationLog>> = calculationLogDao.getAllLogs()
    val settingsFlow: Flow<UserSettings?> = userSettingsDao.getSettings()

    suspend fun getSettings(): UserSettings {
        return userSettingsDao.getSettingsDirect() ?: UserSettings().also {
            userSettingsDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: UserSettings) {
        userSettingsDao.saveSettings(settings)
    }

    suspend fun getAllLogsDirect(): List<CalculationLog> {
        return calculationLogDao.getAllLogsDirect()
    }

    suspend fun saveCalculation(log: CalculationLog): Long {
        return calculationLogDao.insertLog(log)
    }

    suspend fun saveLogs(logs: List<CalculationLog>): List<Long> {
        return calculationLogDao.insertLogs(logs)
    }

    suspend fun deleteLog(logId: Long) {
        calculationLogDao.deleteLogById(logId)
    }

    suspend fun clearLogs() {
        calculationLogDao.clearAllLogs()
    }
}
