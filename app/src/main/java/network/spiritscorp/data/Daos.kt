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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for interacting with the `calculation_logs` database table.
 * Provides asynchronous reactive [Flow] queries and suspend functions for CRUD operations.
 */
@Dao
interface CalculationLogDao {
    /**
     * Observes all calculation log entries ordered chronologically descending by creation timestamp.
     * Emits a new list whenever the database table updates.
     */
    @Query("SELECT * FROM calculation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CalculationLog>>

    /**
     * Retrieves a snapshot of all calculation log entries ordered chronologically descending.
     */
    @Query("SELECT * FROM calculation_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsDirect(): List<CalculationLog>

    /**
     * Inserts or updates a single [CalculationLog] entry.
     * @param log The calculation record to persist.
     * @return The auto-generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CalculationLog): Long

    /**
     * Inserts or updates a batch of [CalculationLog] entries (used during backup restore and batch imports).
     * @param logs List of calculation records to persist.
     * @return List of persisted row IDs.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<CalculationLog>): List<Long>

    /**
     * Deletes a specific calculation log entry by its primary key ID.
     * @param logId The database ID of the record to remove.
     */
    @Query("DELETE FROM calculation_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)

    /**
     * Clears all log entries from the table (used during data reset or restore operations).
     */
    @Query("DELETE FROM calculation_logs")
    suspend fun clearAllLogs()
}

/**
 * Data Access Object (DAO) for persisting and observing personalized [UserSettings].
 */
@Dao
interface UserSettingsDao {
    /**
     * Observes the active user settings (singleton row with ID = 1) reactively.
     */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettings?>

    /**
     * Retrieves the current user settings directly without establishing a reactive stream.
     */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsDirect(): UserSettings?

    /**
     * Saves or updates the user settings configuration.
     * @param settings The user preferences and therapy factor settings to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettings)
}
