package network.spiritscorp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationLogDao {
    @Query("SELECT * FROM calculation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CalculationLog>>

    @Query("SELECT * FROM calculation_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsDirect(): List<CalculationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CalculationLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<CalculationLog>): List<Long>

    @Query("DELETE FROM calculation_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)

    @Query("DELETE FROM calculation_logs")
    suspend fun clearAllLogs()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsDirect(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettings)
}
