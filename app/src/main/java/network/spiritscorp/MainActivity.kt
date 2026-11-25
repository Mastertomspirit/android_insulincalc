package network.spiritscorp

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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.spiritscorp.preferences.ThemePreferences
import network.spiritscorp.ui.screens.MainScreen
import network.spiritscorp.ui.theme.AppTheme
import network.spiritscorp.ui.theme.MyApplicationTheme
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel

/**
 * The primary entry point Activity for the application.
 * Responsible for initializing Edge-to-Edge window displays, managing global Theme preferences,
 * and hosting the root Jetpack Compose UI hierarchy via [MainScreen].
 */
class MainActivity : ComponentActivity() {

    // Shared ViewModel instance scoped to this Activity lifecycle
    private val viewModel: InsulinCalculatorViewModel by viewModels()

    /**
     * Called when the activity is starting. Sets up edge-to-edge layout, reads saved theme preferences,
     * and mounts the Compose UI tree with dynamic theme support.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge system bar rendering
        enableEdgeToEdge()

        // Load initial theme settings from SharedPreferences as fallback before Room loads
        val themePrefs = ThemePreferences(this)
        val initialSavedTheme = themePrefs.selectedTheme
        val initialSavedMode = themePrefs.themeMode

        setContent {
            // Collect user settings reactively from ViewModel
            val userSettings by viewModel.cachedSettings.collectAsStateWithLifecycle()
            val themeEnum = userSettings.selectedTheme ?: initialSavedTheme
            val modeEnum = userSettings.themeMode ?: initialSavedMode

            // Resolve light/dark mode preference
            val isDark = when (modeEnum) {
                AppTheme.Mode.LIGHT -> false
                AppTheme.Mode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            // Apply custom application theme to entire Compose tree
            MyApplicationTheme( selectedTheme = themeEnum, darkTheme = isDark ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
