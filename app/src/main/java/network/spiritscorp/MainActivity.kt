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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.spiritscorp.data.ThemePreferences
import network.spiritscorp.ui.screens.AiMealEstimatorScreen
import network.spiritscorp.ui.screens.CalculatorScreen
import network.spiritscorp.ui.screens.LogbookScreen
import network.spiritscorp.ui.screens.SettingsScreen
import network.spiritscorp.ui.theme.AppTheme
import network.spiritscorp.ui.theme.MyApplicationTheme
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel

/**
 * The primary entry point Activity for the application.
 * Responsible for initializing Edge-to-Edge window displays, managing global Theme preferences,
 * and hosting the root Jetpack Compose UI hierarchy via [MainApp].
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
        val initialSavedTheme = ThemePreferences.getSelectedTheme(this)
        val initialSavedMode = ThemePreferences.getThemeMode(this)

        setContent {
            // Collect user settings reactively from ViewModel
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val themeString = userSettings?.selectedTheme ?: initialSavedTheme
            val modeString = userSettings?.themeMode ?: initialSavedMode

            // Resolve color theme enum from stored string value
            val themeEnum = try {
                AppTheme.valueOf(themeString)
            } catch (_: Exception) {
                AppTheme.MEDICAL_TEAL
            }
            
            // Resolve light/dark mode preference
            val isDark = when (modeString) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            // Apply custom application theme to entire Compose tree
            MyApplicationTheme(
                selectedTheme = themeEnum,
                darkTheme = isDark
            ) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

/**
 * Root composable hosting the primary layout structure, including the top app bar,
 * bottom navigation bar, snackbar host, and tab content switching with slide/fade animations.
 *
 * @param viewModel The shared [InsulinCalculatorViewModel] supplying UI state and event handlers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: InsulinCalculatorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    val topBarTitle = when (uiState.activeTab) {
        0 -> "Insulin-Rechner"
        1 -> "KI Mahlzeiten-Schätzer"
        2 -> "Insulin-Tagebuch"
        3 -> "Einstellungen & Faktoren"
        else -> "Insulin-Rechner"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == 0) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                            contentDescription = "Rechner"
                        )
                    },
                    label = { Text("Rechner") },
                    modifier = Modifier.testTag("nav_calculator")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == 1) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "KI-Schätzer"
                        )
                    },
                    label = { Text("KI-Schätzer") },
                    modifier = Modifier.testTag("nav_ai_estimator")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == 2) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Tagebuch"
                        )
                    },
                    label = { Text("Tagebuch") },
                    modifier = Modifier.testTag("nav_logbook")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Einstellungen"
                        )
                    },
                    label = { Text("Einstellungen") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.activeTab,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val slideDistance = 300
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        initialOffsetX = { if (isForward) slideDistance else -slideDistance }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        targetOffsetX = { if (isForward) -slideDistance else slideDistance }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    ))
                },
                label = "tab_animation"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> CalculatorScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToAiEstimator = { viewModel.setTab(1) }
                    )
                    1 -> AiMealEstimatorScreen(
                        viewModel = viewModel,
                        aiState = aiState
                    )
                    2 -> LogbookScreen(
                        viewModel = viewModel,
                        logs = historyLogs
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        settings = userSettings
                    )
                }
            }
        }
    }
}
