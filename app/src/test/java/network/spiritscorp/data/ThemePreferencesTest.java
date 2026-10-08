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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

import network.spiritscorp.ui.theme.AppTheme;

/**
 * Unit tests verifying {@link ThemePreferences} instantiable object behavior,
 * default fallback values, and preference storage.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ThemePreferencesTest {

    private SharedPreferences mockPrefs;
    private ThemePreferences themePreferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mockPrefs = context.getSharedPreferences("test_theme_prefs", Context.MODE_PRIVATE);
        mockPrefs.edit().clear().commit();
        themePreferences = new ThemePreferences(mockPrefs);
    }

    @Test
    public void testDefaultPreferences() {
        assertEquals("Default selected theme should be AppTheme.MEDICAL_TEAL when no preferences are set", AppTheme.MEDICAL_TEAL, themePreferences.getSelectedTheme());
        assertEquals("Default theme mode should be AppTheme.Mode.SYSTEM when no preferences are set", AppTheme.Mode.SYSTEM, themePreferences.getThemeMode());
    }

    @Test
    public void testSaveAndRetrievePreferences() {
        themePreferences.savePreferences(AppTheme.SUNSET_AMBER, AppTheme.Mode.DARK);

        assertEquals("Selected theme should match the saved value", AppTheme.SUNSET_AMBER, themePreferences.getSelectedTheme());
        assertEquals("Theme mode should match the saved value", AppTheme.Mode.DARK, themePreferences.getThemeMode());

        // Create new instance pointing to same preferences to verify persistence
        ThemePreferences anotherInstance = new ThemePreferences(mockPrefs);
        assertEquals("Persisted selected theme should be retrieved correctly by a new instance", AppTheme.SUNSET_AMBER, anotherInstance.getSelectedTheme());
        assertEquals("Persisted theme mode should be retrieved correctly by a new instance", AppTheme.Mode.DARK, anotherInstance.getThemeMode());
    }

    @Test
    public void testContextConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        ThemePreferences prefsFromContext = new ThemePreferences(context);
        prefsFromContext.savePreferences(AppTheme.EMERALD_GREEN, AppTheme.Mode.LIGHT);
        assertEquals("Context-initialized preferences should retrieve saved selected theme", AppTheme.EMERALD_GREEN, prefsFromContext.getSelectedTheme());
        assertEquals("Context-initialized preferences should retrieve saved theme mode", AppTheme.Mode.LIGHT, prefsFromContext.getThemeMode());
    }
}
