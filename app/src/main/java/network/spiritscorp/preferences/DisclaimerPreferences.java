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
import androidx.annotation.NonNull;

import network.spiritscorp.util.AppConstants;

/**
 * Synchronous disclaimer preferences cache in Java to manage medical disclaimer acceptance
 * and re-show periods (30 days).
 */
public class DisclaimerPreferences {

    private static final String PREFS_NAME = "insulin_calc_disclaimer_prefs";
    private static final String KEY_LAST_DISMISSED = "last_dismissed_timestamp";
    private final SharedPreferences mPrefs;

    /**
     * Constructs a new DisclaimerPreferences instance bound to the application context.
     *
     * @param context Android context.
     */
    public DisclaimerPreferences(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Constructs a new DisclaimerPreferences instance with custom SharedPreferences for DI and testing.
     *
     * @param prefs SharedPreferences instance.
     */
    public DisclaimerPreferences(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
    }

    /**
     * Checks whether the disclaimer should be shown to the user.
     * It returns true if it was never dismissed or if 30 days have passed since the last dismissal.
     *
     * @return true if disclaimer should be displayed, false otherwise.
     */
    public boolean shouldShowDisclaimer() {
        boolean wasDismissed = mPrefs.getBoolean(KEY_LAST_DISMISSED + "_bool", false);
        if (!wasDismissed) return true;

        long lastDismissed = mPrefs.getLong(KEY_LAST_DISMISSED, 0L);
        return (System.currentTimeMillis() - lastDismissed) > AppConstants.DISCLAIMER_RE_SHOW_INTERVAL_MS;
    }

    /**
     * Dismisses the disclaimer, recording the current timestamp.
     */
    public void dismissDisclaimer() {
        mPrefs.edit()
                .putBoolean(KEY_LAST_DISMISSED + "_bool", true)
                .putLong(KEY_LAST_DISMISSED, System.currentTimeMillis())
                .apply();
    }
}
