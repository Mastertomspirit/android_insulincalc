package network.spiritscorp.viewmodel

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

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import network.spiritscorp.MainActivity
import network.spiritscorp.data.AppDatabase
import network.spiritscorp.data.CalculationLogDao
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.data.UserSettingsDao
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [InsulinCalculatorViewModel] and database error recovery handling,
 * verifying normal flow with repositories, error state management, and activity onCreate lifecycles.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseErrorRecoveryTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    /**
     * Verifies that the ViewModel correctly handles manual database error notification and clearance.
     */
    @Test
    fun testViewModelHandlesDatabaseErrorState() {
        // Given a ViewModel initialized with a null repository (database error state)
        val viewModel = InsulinCalculatorViewModel(app, null as InsulinRepository?)
        assertNotNull("ViewModel databaseError stateflow must not be null", viewModel.databaseMainView.databaseError)

        // Verify initial error state when created with null repository
        assertTrue("Database error state should be true initially", viewModel.databaseMainView.databaseError.value)

        // Clear error state
        viewModel.databaseMainView.clearDatabaseError()
        assertFalse("Database error state should be false after clearing", viewModel.databaseMainView.databaseError.value)

        // Trigger database error again
        viewModel.databaseMainView.notifyDatabaseError()
        assertTrue("Database error state should be true after manual notification", viewModel.databaseMainView.databaseError.value)
    }

    /**
     * Verifies that initializing the ViewModel with a null repository correctly sets the database error flag.
     */
    @Test
    fun testViewModelInitializationWithNullRepositorySetsDatabaseError() {
        // When creating ViewModel with null repository simulating database failure
        val viewModel = InsulinCalculatorViewModel(app, null as InsulinRepository?)

        // Then databaseError should be true to signal error recovery UI
        assertTrue("Database error state must be true when repository is null on creation", viewModel.databaseMainView.databaseError.value)
    }

    /**
     * Verifies the normal operating flow when a valid repository is provided to the ViewModel.
     */
    @Test
    fun testViewModelInitializationWithValidRepositoryClearsDatabaseError() {
        // Given mocked DAOs and a valid repository
        val mockLogDao = mock(CalculationLogDao::class.java)
        val mockSettingsDao = mock(UserSettingsDao::class.java)
        val defaultSettings = UserSettings()
        `when`(mockSettingsDao.getSettingsDirect()).thenReturn(defaultSettings)
        `when`(mockSettingsDao.getSettings()).thenReturn(flowOf(defaultSettings))
        `when`(mockLogDao.getAllLogs()).thenReturn(emptyFlow())

        val repo = InsulinRepository(mockLogDao, mockSettingsDao)

        // When ViewModel is initialized with a valid repository (normal flow)
        val viewModel = InsulinCalculatorViewModel(app, repo)

        // Then database error state must be false and stateflows active
        assertFalse("Database error state must be false when a valid repository is provided", viewModel.databaseMainView.databaseError.value)
        assertNotNull("User settings stateflow must not be null", viewModel.cachedSettings)
        assertNotNull("History logs stateflow must not be null", viewModel.databaseMainView.historyLogs)
    }

    /**
     * Verifies reactive StateFlow emissions during error state transitions and recovery flows.
     */
    @Test
    fun testDatabaseErrorStateFlowEmissionsAndRecoveryFlow() = runTest {
        // Given a ViewModel in error state
        val viewModel = InsulinCalculatorViewModel(app, null as InsulinRepository?)
        assertTrue("Initial state must indicate database error", viewModel.databaseMainView.databaseError.value)

        // When clearing database error
        viewModel.databaseMainView.clearDatabaseError()
        assertFalse("StateFlow must emit false after clearDatabaseError", viewModel.databaseMainView.databaseError.value)

        // When re-notifying database error
        viewModel.databaseMainView.notifyDatabaseError()
        assertTrue("StateFlow must emit true after notifyDatabaseError", viewModel.databaseMainView.databaseError.value)
    }

    /**
     * Verifies the complete activity lifecycle and onCreate flow, ensuring MainActivity
     * launches successfully, executes onCreate, and initializes the app UI and ViewModel.
     * With a valid database mock, the database error value must be false during normal startup.
     */
    @Test
    fun testMainActivityOnCreateAndLifecycleFlow() {
        val mockDb = mock(AppDatabase::class.java)
        val mockLogDao = mock(CalculationLogDao::class.java)
        val mockSettingsDao = mock(UserSettingsDao::class.java)
        val defaultSettings = UserSettings()
        `when`(mockDb.calculationLogDao()).thenReturn(mockLogDao)
        `when`(mockDb.userSettingsDao()).thenReturn(mockSettingsDao)
        `when`(mockSettingsDao.getSettingsDirect()).thenReturn(defaultSettings)
        `when`(mockSettingsDao.getSettings()).thenReturn(flowOf(defaultSettings))
        `when`(mockLogDao.getAllLogs()).thenReturn(emptyFlow())

        mockStatic(AppDatabase::class.java).use { mockedStatic ->
            mockedStatic.`when`<AppDatabase> { AppDatabase.getDatabase(any()) }.thenReturn(mockDb)

            // Launch MainActivity using Robolectric ActivityScenario to execute onCreate and setup
            val scenario = ActivityScenario.launch(MainActivity::class.java)

            scenario.onActivity { activity ->
                // Verify that the activity was created and is active/running
                assertNotNull("MainActivity instance must not be null after onCreate", activity)
                assertFalse("MainActivity should not be finishing immediately upon successful onCreate", activity.isFinishing)

                // Verify ViewModel and database error value at the end of onCreate / activity launch flow
                val viewModel = ViewModelProvider(activity)[InsulinCalculatorViewModel::class.java]
                assertNotNull("Activity ViewModel must not be null", viewModel)
                assertNotNull("ViewModel databaseError stateflow must not be null", viewModel.databaseMainView.databaseError)

                // Normal startup flow with a valid database must result in databaseError == false
                assertFalse("Database error value must be false during normal MainActivity startup flow", viewModel.databaseMainView.databaseError.value)
            }

            scenario.close()
        }
    }
}
