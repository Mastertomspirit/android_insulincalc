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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Synchronous theme preferences cache to prevent theme flashing/flickering on app startup
 * while Room Database asynchronous Flow is initializing.
 */
object ThemePreferences {
    private const val PREFS_NAME = "insulin_calc_theme_prefs"
    private const val KEY_SELECTED_THEME = "selected_theme"
    private const val KEY_THEME_MODE = "theme_mode"

    /**
     * Reads the cached color theme identifier synchronously.
     * @param context Android context.
     * @return Stored theme enum name or "MEDICAL_TEAL" default.
     */
    fun getSelectedTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_THEME, "MEDICAL_TEAL") ?: "MEDICAL_TEAL"
    }

    /**
     * Reads the cached theme mode (LIGHT, DARK, or SYSTEM) synchronously.
     * @param context Android context.
     * @return Stored mode string or "SYSTEM" default.
     */
    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    /**
     * Persists the active theme preferences synchronously to SharedPreferences.
     * @param context Android context.
     * @param selectedTheme Selected color theme identifier.
     * @param themeMode Selected mode (LIGHT, DARK, or SYSTEM).
     */
    fun saveThemePreferences(context: Context, selectedTheme: String, themeMode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_SELECTED_THEME, selectedTheme)
                .putString(KEY_THEME_MODE, themeMode)
        }
    }
}
