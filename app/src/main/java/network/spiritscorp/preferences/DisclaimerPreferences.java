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
import androidx.annotation.VisibleForTesting;

import network.spiritscorp.util.AppConstants;

/**
 * Synchronous disclaimer preferences cache in Java to manage medical disclaimer acceptance
 * and re-show periods (30 days).
 */
public class DisclaimerPreferences {

    private static final String PREFS_NAME = "insulin_calc_disclaimer_prefs";
    private static final String KEY_SHOULD_SHOW_BOOL = "key_should_show_bool";
    private static final String KEY_LAST_SEEN_TIMESTAMP = "key_last_seen_timestamp";
    /**
     * Legacy preference key storing the last dismissal timestamp in milliseconds.
     *
     * @deprecated Replaced by {@link #KEY_SHOULD_SHOW_BOOL} and {@link #KEY_LAST_SEEN_TIMESTAMP}
     *             for clearer state tracking without implicit boolean/timestamp coupling.
     *             Retained temporarily for automatic on-read migration.
     */
    @Deprecated(since = "1.4.0")
    private static final String KEY_LAST_DISMISSED = "last_dismissed_timestamp";

    private final SharedPreferences mPrefs;

    /**
     * Creates a disclaimer persistence manager backed by the app-private
     * {@value #PREFS_NAME} {@link SharedPreferences} file ({@link Context#MODE_PRIVATE}).
     *
     * <p>Unwraps the application context from {@code context} so this instance can be safely
     * retained across {@code Activity} or {@code Fragment} lifecycles without leaking UI references.
     *
     * @param context any Android context used to resolve the application-scoped preferences store
     */
    public DisclaimerPreferences(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Binds the disclaimer persistence logic to an explicitly supplied {@link SharedPreferences}
     * backend instead of opening the default application preferences file.
     *
     * <p>Decouples state management from the Android {@link Context} so unit tests and
     * dependency-injected callers can provide an isolated or mocked preference store without
     * mutating real device storage.
     *
     * @param prefs the pre-configured preferences storage used to read and persist disclaimer state
     */
    @VisibleForTesting()
    DisclaimerPreferences(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
    }

    /**
     * Evaluates whether the medical disclaimer must be presented to the user.
     *
     * <p>Returns {@code true} on first launch, when explicitly flagged for display, or once
     * {@link AppConstants#DISCLAIMER_RE_SHOW_INTERVAL_MS} has elapsed since the last dismissal.
     * If legacy preference entries from versions prior to {@code 1.4.0} are detected, they are
     * migrated in-place to the current key schema and removed.
     *
     * @return {@code true} if the disclaimer needs to be displayed, {@code false} otherwise
     */
    public boolean shouldShowDisclaimer() {
        if (mPrefs.contains(KEY_LAST_SEEN_TIMESTAMP)) {
            if (mPrefs.getBoolean(KEY_SHOULD_SHOW_BOOL, true)) {
                return true;
            }
            long lastSeenTimestamp = mPrefs.getLong(KEY_LAST_SEEN_TIMESTAMP, 0L);
            return (System.currentTimeMillis() - lastSeenTimestamp) > AppConstants.DISCLAIMER_RE_SHOW_INTERVAL_MS;
        }

        // TODO: Remove legacy key migration once v1.3.x installations are phased out
        // One-time migration from legacy keys
        String legacyBoolKey = KEY_LAST_DISMISSED + "_bool";
        if (mPrefs.contains(KEY_LAST_DISMISSED) || mPrefs.contains(legacyBoolKey)) {
            boolean wasDismissed = mPrefs.getBoolean(legacyBoolKey, false);
            long legacyTimestamp = mPrefs.getLong(KEY_LAST_DISMISSED, 0L);

            mPrefs.edit()
                    .remove(KEY_LAST_DISMISSED)
                    .remove(legacyBoolKey)
                    .putBoolean(KEY_SHOULD_SHOW_BOOL, !wasDismissed)
                    .putLong(KEY_LAST_SEEN_TIMESTAMP, legacyTimestamp)
                    .apply();

            return !wasDismissed || (System.currentTimeMillis() - legacyTimestamp) > AppConstants.DISCLAIMER_RE_SHOW_INTERVAL_MS;
        }
        return true;
    }

    /**
     * Marks the medical disclaimer as accepted and records the current wall-clock timestamp
     * as the baseline for the next {@link AppConstants#DISCLAIMER_RE_SHOW_INTERVAL_MS} cycle.
     *
     * <p>Clears any residual preference entries beforehand so obsolete keys are purged cleanly.
     */
    public void dismissDisclaimer() {
        mPrefs.edit()
                .clear()
                .putBoolean(KEY_SHOULD_SHOW_BOOL, false)
                .putLong(KEY_LAST_SEEN_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }
}
