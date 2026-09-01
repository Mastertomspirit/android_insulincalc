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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.abs
import network.spiritscorp.data.ThemePreferences
import network.spiritscorp.ui.screens.ai.AiMealEstimatorScreen
import network.spiritscorp.ui.screens.calculator.CalculatorScreen
import network.spiritscorp.ui.screens.logbook.LogbookScreen
import network.spiritscorp.ui.screens.settings.SettingsScreen
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
        val themePrefs = ThemePreferences(this)
        val initialSavedTheme = themePrefs.selectedTheme
        val initialSavedMode = themePrefs.themeMode

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
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = uiState.activeTab, pageCount = { 4 })

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    // Sync ViewModel with settled pager changes from swipe gestures
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (uiState.activeTab != settledPage) {
                viewModel.setTab(settledPage)
            }
        }
    }

    // Dismiss keyboard when page scrolling begins
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    val navigateToTab: (Int) -> Unit = { targetIndex ->
        focusManager.clearFocus()
        viewModel.setTab(targetIndex)
        coroutineScope.launch {
            val distance = abs(pagerState.currentPage - targetIndex)
            if (distance <= 1) {
                pagerState.animateScrollToPage(targetIndex)
            } else {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    val activeDisplayPage = pagerState.targetPage

    val topBarTitle = when (activeDisplayPage) {
        0 -> "Insulin-Rechner"
        1 -> "KI Mahlzeiten-Schätzer"
        2 -> "Insulin-Tagebuch"
        3 -> "Einstellungen & Faktoren"
        else -> "Insulin-Rechner"
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
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
                    selected = activeDisplayPage == 0,
                    onClick = { navigateToTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 0) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                            contentDescription = "Rechner"
                        )
                    },
                    label = { Text("Rechner") },
                    modifier = Modifier.testTag("nav_calculator")
                )

                NavigationBarItem(
                    selected = activeDisplayPage == 1,
                    onClick = { navigateToTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 1) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "KI-Schätzer"
                        )
                    },
                    label = { Text("KI-Schätzer") },
                    modifier = Modifier.testTag("nav_ai_estimator")
                )

                NavigationBarItem(
                    selected = activeDisplayPage == 2,
                    onClick = { navigateToTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 2) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Tagebuch"
                        )
                    },
                    label = { Text("Tagebuch") },
                    modifier = Modifier.testTag("nav_logbook")
                )

                NavigationBarItem(
                    selected = activeDisplayPage == 3,
                    onClick = { navigateToTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> CalculatorScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToAiEstimator = { navigateToTab(1) }
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
