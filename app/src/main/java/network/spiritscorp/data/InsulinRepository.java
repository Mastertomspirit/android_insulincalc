package network.spiritscorp.data;

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

import java.util.List;
import kotlinx.coroutines.flow.Flow;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;

/**
 * Single source of truth repository managing data access for calculation logs
 * and personalized insulin therapy settings in Java.
 */
public record InsulinRepository(CalculationLogDao calculationLogDao,
                                UserSettingsDao userSettingsDao) {

    /**
     * Cold Flow emitting all historical calculation logs ordered chronologically descending.
     */
    public Flow<List<CalculationLog>> getAllLogs() {
        return calculationLogDao.getAllLogs();
    }

    /**
     * Cold Flow emitting current UserSettings updates reactively.
     */
    public Flow<UserSettings> getSettingsFlow() {
        return userSettingsDao.getSettings();
    }

    /**
     * Retrieves the current user settings or initializes and persists default settings if none exist yet.
     */
    public UserSettings getSettings() {
        UserSettings existing = userSettingsDao.getSettingsDirect();
        if (existing == null) {
            UserSettings defaultSettings = new UserSettings();
            userSettingsDao.saveSettings(defaultSettings);
            return defaultSettings;
        }
        return existing;
    }

    /**
     * Persists updated user settings into the database.
     */
    public void saveSettings(UserSettings settings) {
        userSettingsDao.saveSettings(settings);
    }

    /**
     * Retrieves a non-reactive snapshot list of all calculation logs.
     */
    public List<CalculationLog> getAllLogsDirect() {
        return calculationLogDao.getAllLogsDirect();
    }

    /**
     * Inserts a new calculation log into the database.
     */
    public long saveCalculation(CalculationLog log) {
        return calculationLogDao.insertLog(log);
    }

    /**
     * Inserts a batch of calculation logs.
     */
    public long[] saveLogs(List<CalculationLog> logs) {
        return calculationLogDao.insertLogs(logs);
    }

    /**
     * Deletes a specific calculation log by its ID.
     */
    public void deleteLog(long logId) {
        calculationLogDao.deleteLogById(logId);
    }

    /**
     * Deletes all calculation logs from the database.
     */
    public void clearLogs() {
        calculationLogDao.clearAllLogs();
    }
}
