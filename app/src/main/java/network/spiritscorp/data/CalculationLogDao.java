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

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import org.jetbrains.annotations.NotNull;
import network.spiritscorp.model.CalculationLog;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

/**
 * Data Access Object (DAO) for interacting with the `calculation_logs` database table.
 */
@Dao
public interface CalculationLogDao {

    /**
     * Observes all calculation log entries ordered chronologically descending by creation timestamp.
     */
    @NonNull
    @Query("SELECT * FROM calculation_logs ORDER BY timestamp DESC")
    Flow<List<CalculationLog>> getAllLogs();

    /**
     * Retrieves a snapshot of all calculation log entries ordered chronologically descending.
     */
    @NonNull
    @Query("SELECT * FROM calculation_logs ORDER BY timestamp DESC")
    List<CalculationLog> getAllLogsDirect();

    /**
     * Inserts or updates a single CalculationLog entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLog(@NotNull CalculationLog log);

    /**
     * Inserts or updates a batch of CalculationLog entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertLogs(@NotNull List<CalculationLog> logs);

    /**
     * Deletes a specific calculation log entry by its primary key ID.
     */
    @Query("DELETE FROM calculation_logs WHERE id = :logId")
    void deleteLogById(long logId);

    /**
     * Clears all log entries from the table.
     */
    @Query("DELETE FROM calculation_logs")
    void clearAllLogs();
}
