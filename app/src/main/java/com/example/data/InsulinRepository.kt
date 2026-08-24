package com.example.data

import com.example.model.CalculationLog
import com.example.model.UserSettings
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

    suspend fun saveCalculation(log: CalculationLog): Long {
        return calculationLogDao.insertLog(log)
    }

    suspend fun deleteLog(logId: Long) {
        calculationLogDao.deleteLogById(logId)
    }

    suspend fun clearLogs() {
        calculationLogDao.clearAllLogs()
    }
}
