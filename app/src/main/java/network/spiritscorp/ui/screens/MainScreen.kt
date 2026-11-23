package network.spiritscorp.ui.screens

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

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import network.spiritscorp.R
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.ui.components.DatabaseRecoveryDialog
import network.spiritscorp.ui.screens.ai.AiMealEstimatorScreen
import network.spiritscorp.ui.screens.calculator.CalculatorScreen
import network.spiritscorp.ui.screens.logbook.LogbookScreen
import network.spiritscorp.ui.screens.settings.SettingsScreen
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import kotlin.math.abs

/**
 * Root composable hosting the primary layout structure of the application.
 *
 * This screen manages the navigation between main features using a [HorizontalPager]
 * controlled by a bottom navigation bar. It observes the [InsulinCalculatorViewModel]
 * for state updates, handles database recovery scenarios, and coordinates
 * screen-level lifecycle events.
 *
 * @param viewModel The primary [InsulinCalculatorViewModel] that holds the application state,
 * business logic, and sub-viewModels.
 */
@Composable
internal fun MainScreen(viewModel: InsulinCalculatorViewModel) {
    // Collect UI state, AI state, history logs, settings and DB error status from ViewModel
    val mainUiState by viewModel.mainUiState.collectAsStateWithLifecycle()
    val calculatorUiState by viewModel.calculatorMainView.calculatorUiState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiViewModel.aiState.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isDatabaseError by viewModel.databaseError.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    // Pager state to manage tab swipes and navigation
    val pagerState = rememberPagerState(initialPage = mainUiState.activeTab, pageCount = { 4 })
    val context = LocalContext.current

    // Launcher for file selection to import database backups
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val backupManager = DatabaseBackupManager(context)
            val content = backupManager.readTextFromUri(context, it)
            if (content != null) {
                coroutineScope.launch {
                    viewModel.importBackupContent(content)
                }
            }
        }
    }

    // Display recovery dialog if a critical database error occurs
    if (isDatabaseError) {
        DatabaseRecoveryDialog(
            onImportBackup = {
                viewModel.resetDatabase()
                backupImportLauncher.launch("*/*")
            },
            onResetDatabase = {
                viewModel.resetDatabase()
            },
            onCloseApp = {
                (context as? Activity)?.finish()
            }
        )
    }

    // Handle lifecycle events to notify ViewModel when the app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.calculatorMainView.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show toast messages triggered by UI events
    LaunchedEffect(mainUiState.toastMessage) {
        mainUiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Sync tab selection from ViewModel to pager
    LaunchedEffect(mainUiState.activeTab) {
        if (pagerState.currentPage != mainUiState.activeTab) {
            val distance = abs(pagerState.currentPage - mainUiState.activeTab)
            if (distance <= 1) {
                pagerState.animateScrollToPage(mainUiState.activeTab)
            } else {
                pagerState.scrollToPage(mainUiState.activeTab)
            }
        }
    }

    // Sync current page from pager back to ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (mainUiState.activeTab != settledPage) {
                viewModel.setTab(settledPage)
            }
        }
    }

    // Clear focus (keyboard) when scrolling starts
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // Tab navigation logic
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

    // Determine top bar title based on current active page
    val topBarTitle = when (activeDisplayPage) {
        0 -> stringResource(R.string.main_top_bar_calculator)
        1 -> stringResource(R.string.main_top_bar_ai_estimator)
        2 -> stringResource(R.string.main_top_bar_logbook)
        3 -> stringResource(R.string.main_top_bar_settings)
        else -> stringResource(R.string.main_top_bar_calculator)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        contentWindowInsets = WindowInsets.systemBars,
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
                            contentDescription = stringResource(R.string.main_nav_calculator_label)
                        )
                    },
                    label = { Text(stringResource(R.string.main_nav_calculator_label)) },
                    modifier = Modifier.testTag("nav_calculator")
                )

                NavigationBarItem(
                    selected = activeDisplayPage == 1,
                    onClick = { navigateToTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 1) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = stringResource(R.string.main_nav_ai_estimator_label)
                        )
                    },
                    label = { Text(stringResource(R.string.main_nav_ai_estimator_label)) },
                    modifier = Modifier.testTag("nav_ai_estimator")
                )

                NavigationBarItem(
                    selected = activeDisplayPage == 2,
                    onClick = { navigateToTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 2) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.main_nav_logbook_label)
                        )
                    },
                    label = { Text(stringResource(R.string.main_nav_logbook_label)) },
                    modifier = Modifier.testTag("nav_logbook")
                )

                NavigationBarItem(
                    selected = activeDisplayPage == 3,
                    onClick = { navigateToTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (activeDisplayPage == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.main_nav_settings_label)
                        )
                    },
                    label = { Text(stringResource(R.string.main_nav_settings_label)) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> CalculatorScreen(
                        viewModel = viewModel,
                        calculatorMainView = viewModel.calculatorMainView,
                        logMainView = viewModel.logMainView,
                        uiState = calculatorUiState,
                        onNavigateToAiEstimator = { navigateToTab(1) }
                    )
                    1 -> AiMealEstimatorScreen(
                        aiViewModel = viewModel.aiViewModel,
                        aiState = aiState,
                        geminiAiModel = userSettings.selectedAiModel,
                        geminiApiKey = userSettings.geminiApiKey
                    )
                    2 -> LogbookScreen(
                        logMainView = viewModel.logMainView,
                        logs = historyLogs,
                        currentGlucoseUnit = { userSettings.glucoseUnit }
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        logMainView = viewModel.logMainView,
                        settings = userSettings
                    )
                }
            }
        }
    }
}
