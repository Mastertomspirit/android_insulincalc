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

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * Manages database recovery and reset operations, decoupling UI components from direct
 * {@link AppDatabase} lifecycle internals.
 */
public class DatabaseRestoreManager {

    /**
     * Resets the AppDatabase singleton instance, closes active database connections,
     * deletes the existing database file, and clears stored security key preferences.
     *
     * <p>Note: {@code commit()} is intentionally used instead of {@code apply()} here because
     * the database deletion and preference clearing must be executed and synchronized
     * sequentially before any subsequent restore steps occur.
     *
     * @param context Application context.
     */
    @SuppressLint({"ApplySharedPref"})
    public void resetInstance(Context context) {
        AppDatabase.resetInstance();
        Context appContext = context.getApplicationContext();
        appContext.deleteDatabase(DatabaseConstants.DATABASE_NAME);
        appContext.getSharedPreferences(DatabaseConstants.SECURITY_PREFS_FILE, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }
}
