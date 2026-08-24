package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AiMealEstimatorScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.LogbookScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.InsulinCalculatorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InsulinCalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val themeEnum = try {
                com.example.ui.theme.AppTheme.valueOf(userSettings?.selectedTheme ?: "MEDICAL_TEAL")
            } catch (e: Exception) {
                com.example.ui.theme.AppTheme.MEDICAL_TEAL
            }
            val isDark = when (userSettings?.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(
                selectedTheme = themeEnum,
                darkTheme = isDark
            ) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                    label = { Text("Faktoren") },
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
                transitionSpec = { fadeIn() togetherWith fadeOut() },
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
