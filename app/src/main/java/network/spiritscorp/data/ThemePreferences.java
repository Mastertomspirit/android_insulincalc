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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/**
 * Synchronous theme preferences cache in Java to prevent theme flashing/flickering on app startup
 * while Room Database asynchronous Flow is initializing.
 */
public class ThemePreferences {

    private static final String PREFS_NAME = "insulin_calc_theme_prefs";
    private static final String KEY_SELECTED_THEME = "selected_theme";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final String DEFAULT_THEME = "MEDICAL_TEAL";
    public static final String DEFAULT_MODE = "SYSTEM";

    private final SharedPreferences mPrefs;

    /**
     * Constructs a new ThemePreferences instance bound to the application context.
     *
     * @param context Android context.
     */
    public ThemePreferences(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Constructs a new ThemePreferences instance with custom SharedPreferences for DI and testing.
     *
     * @param prefs SharedPreferences instance.
     */
    public ThemePreferences(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
    }

    /**
     * Reads the cached color theme identifier synchronously.
     *
     * @return Stored theme enum name or "MEDICAL_TEAL" default.
     */
    @NonNull
    public String getSelectedTheme() {
        return mPrefs.getString(KEY_SELECTED_THEME, DEFAULT_THEME);
    }

    /**
     * Reads the cached theme mode (LIGHT, DARK, or SYSTEM) synchronously.
     *
     * @return Stored mode string or "SYSTEM" default.
     */
    @NonNull
    public String getThemeMode() {
        return mPrefs.getString(KEY_THEME_MODE, DEFAULT_MODE);
    }

    /**
     * Persists the active theme preferences synchronously to SharedPreferences.
     *
     * @param selectedTheme Selected color theme identifier.
     * @param themeMode     Selected mode (LIGHT, DARK, or SYSTEM).
     */
    public void savePreferences(@NonNull String selectedTheme, @NonNull String themeMode) {
        mPrefs.edit()
                .putString(KEY_SELECTED_THEME, selectedTheme)
                .putString(KEY_THEME_MODE, themeMode)
                .apply();
    }
}
