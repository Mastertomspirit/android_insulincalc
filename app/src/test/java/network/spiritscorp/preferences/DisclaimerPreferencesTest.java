package network.spiritscorp.preferences;

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

import network.spiritscorp.util.AppConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying {@link DisclaimerPreferences} behavior, default values,
 * dismissal logic, and persistence.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DisclaimerPreferencesTest {

    private SharedPreferences mockPrefs;
    private DisclaimerPreferences disclaimerPreferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mockPrefs = context.getSharedPreferences("test_disclaimer_prefs", Context.MODE_PRIVATE);
        mockPrefs.edit().clear().commit();
        disclaimerPreferences = new DisclaimerPreferences(mockPrefs);
    }

    @Test
    public void testDefaultShouldShowDisclaimer() {
        assertTrue("Disclaimer should be shown by default when never dismissed", disclaimerPreferences.shouldShowDisclaimer());
    }

    @Test
    public void testDismissDisclaimer() {
        disclaimerPreferences.dismissDisclaimer();
        assertFalse("Disclaimer should not be shown right after dismissal", disclaimerPreferences.shouldShowDisclaimer());

        // Verify persistence via another instance
        DisclaimerPreferences anotherInstance = new DisclaimerPreferences(mockPrefs);
        assertFalse("Disclaimer should remain dismissed for a new instance", anotherInstance.shouldShowDisclaimer());
    }

    @Test
    public void testContextConstructor() {
        disclaimerPreferences.dismissDisclaimer();
        assertFalse("Context constructor initialized preferences should persist dismissal", disclaimerPreferences.shouldShowDisclaimer());
    }

    @Test
    public void testReShowAfterOneMonth() {
        // Dismiss disclaimer
        disclaimerPreferences.dismissDisclaimer();
        assertFalse("Disclaimer should not be shown right after dismissal", disclaimerPreferences.shouldShowDisclaimer());

        // Simulate time passing: set last dismissed timestamp to past the re-show interval
        long pastTime = System.currentTimeMillis() - (AppConstants.DISCLAIMER_RE_SHOW_INTERVAL_MS + 86400000L); // 31 days ago
        mockPrefs.edit()
                .putLong("last_dismissed_timestamp", pastTime)
                .apply();

        assertTrue("Disclaimer should be shown again after 30 days have passed", disclaimerPreferences.shouldShowDisclaimer());
    }
}
