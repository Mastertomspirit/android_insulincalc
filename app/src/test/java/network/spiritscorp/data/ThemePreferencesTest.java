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
import static org.junit.Assert.assertNotNull;

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
        assertEquals("MEDICAL_TEAL", themePreferences.getSelectedTheme());
        assertEquals("SYSTEM", themePreferences.getThemeMode());
    }

    @Test
    public void testSaveAndRetrievePreferences() {
        themePreferences.savePreferences("SUNSET_ORANGE", "DARK");

        assertEquals("SUNSET_ORANGE", themePreferences.getSelectedTheme());
        assertEquals("DARK", themePreferences.getThemeMode());

        // Create new instance pointing to same preferences to verify persistence
        ThemePreferences anotherInstance = new ThemePreferences(mockPrefs);
        assertEquals("SUNSET_ORANGE", anotherInstance.getSelectedTheme());
        assertEquals("DARK", anotherInstance.getThemeMode());
    }

    @Test
    public void testContextConstructor() {
        Context context = ApplicationProvider.getApplicationContext();
        ThemePreferences prefsFromContext = new ThemePreferences(context);
        assertNotNull(prefsFromContext.getSelectedTheme());
        assertNotNull(prefsFromContext.getThemeMode());
    }
}
