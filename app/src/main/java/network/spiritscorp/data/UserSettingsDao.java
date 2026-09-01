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
import network.spiritscorp.model.UserSettings;
import kotlinx.coroutines.flow.Flow;

/**
 * Data Access Object (DAO) for persisting and observing personalized UserSettings.
 */
@Dao
public interface UserSettingsDao {

    /**
     * Observes the active user settings (singleton row with ID = 1) reactively.
     */
    @NonNull
    @Query("SELECT * FROM user_settings WHERE id = 1")
    Flow<UserSettings> getSettings();

    /**
     * Retrieves the current user settings directly without establishing a reactive stream.
     */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    UserSettings getSettingsDirect();

    /**
     * Saves or updates the user settings configuration.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSettings(@NonNull UserSettings settings);
}
