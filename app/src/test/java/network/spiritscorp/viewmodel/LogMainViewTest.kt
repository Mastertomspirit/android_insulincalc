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
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import network.spiritscorp.R
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.CalculationSummary
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.TimeOfDay
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import kotlin.collections.mutableListOf
import kotlin.time.Duration.Companion.milliseconds

/**
 * Comprehensive unit test class for [LogMainView].
 * Verifies all variants of the 3 main methods (`saveCalculationToLog`, `deleteLog`, `clearAllLogs`)
 * for both active (non-null) and null repository states, initial state instantiation,
 * correct toast notifications, and repository interactions using Mockito ArgumentCaptor and JUnit 5 assertions.
 */
@RunWith(RobolectricTestRunner::class)
internal class LogMainViewTest {

    private lateinit var app: Application
    private lateinit var repository: InsulinRepository
    private lateinit var toastMessages: MutableList<String>

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        repository = mock(InsulinRepository::class.java)

        toastMessages = mutableListOf()

        `when`(repository.clearLogs()).thenReturn(true)
        `when`(repository.deleteLog(any(Long::class.java))).thenReturn(true)
        `when`(repository.saveCalculation(any(CalculationLog::class.java))).thenReturn(22L)
    }

    /**
     * Tests initial instantiation of [LogMainView] and verifies that the initial state
     * is created successfully without triggering any unexpected toast notifications.
     */
    @Test
    fun testInitialInstantiationAndState() {
        // When instantiating LogMainView
        val logMainView = LogMainView(
            application = app,
            repository = { repository },
            calculatorUiState = { createInitialCalculatorUiState() },
            onShowToast = { toastMessages.add(it) }
        )

        ShadowLooper.idleMainLooper()

        // Then verify the view model initializes correctly without premature side effects
        assertAll(
            "Verify LogMainView initial instantiation state",
            { assertNotNull(logMainView, "LogMainView instance must not be null upon creation") },
            { assertTrue(toastMessages.isEmpty(), "No toast messages should be shown during initial instantiation") }
        )
    }

    /**
     * Tests [LogMainView.saveCalculationToLog] when a valid (non-null) repository is provided.
     * Verifies that the repository successfully persists the calculated log entry with correct properties
     * (using Mockito ArgumentCaptor) and that the diary saved toast message is correctly triggered.
     */
    @Test
    fun testSaveCalculationToLogWithValidRepository() = runBlocking {
        // Given a valid repository and custom calculation state
        // CalculationSummary(carbGrams, keValue, beValue, factorUsed, mealInsulin, bloodGlucoseInput, targetGlucose, correctionInsulin, rawTotalInsulin, roundedTotalInsulin, roundingStep, isHypoRisk, advisoryNote)
        val summary = CalculationSummary(
            50.0,
            5.0,
            4.1,
            1.2,
            6.0,
            140.0,
            120.0,
            1.0,
            7.0,
            7.0,
            0.5,
            false,
            ""
        )
        val uiState = MutableStateFlow(createInitialCalculatorUiState())
        uiState.update {
            it.copy(
                carbInput = "50",
                selectedUnit = CarbUnit.GRAMS,
                selectedTimeOfDay = TimeOfDay.NOON,
                mealTitle = "Test Lunch",
                notes = "Test notes",
                calculationSummary = summary
            )
        }
        val logMainView = LogMainView(
            application = app,
            repository = { repository },
            calculatorUiState = { uiState.value },
            onShowToast = { toastMessages.add(it) }
        )

        // When saving calculation with an override notes string
        logMainView.saveCalculationToLog("Overridden Notes")
        // Wait for background IO dispatcher coroutine completion and pump main looper
        delay(100.milliseconds)
        ShadowLooper.idleMainLooper()

        // Then capture and verify the exact expected CalculationLog result properties and success toast
        val logCaptor = ArgumentCaptor.forClass(CalculationLog::class.java)
        val expectedToast = app.getString(R.string.view_model_diary_saved_toast)

        assertAll(
            "Verify saveCalculationToLog successfully interacts with non-null repository, passes expected log data, and triggers toast",
            { verify(repository).saveCalculation(logCaptor.capture()) },
            { assertEquals("Test Lunch", logCaptor.value.mealTitle, "Saved log meal title must match expected uiState title") },
            { assertEquals(50.0, logCaptor.value.carbGrams, "Saved log carb grams must match expected summary") },
            { assertEquals("Overridden Notes", logCaptor.value.notes, "Saved log notes must match override value") },
            { assertEquals(1, toastMessages.size, "Exactly one toast message must be emitted upon saving calculation") },
            { assertEquals(expectedToast, toastMessages[0], "Toast message must match R.string.view_model_diary_saved_toast") }
        )
    }

    /**
     * Tests [LogMainView.saveCalculationToLog] when the repository provider returns null.
     * Verifies that repository persistence calls are avoided (`never()`) and that no toast
     * message is shown when DB recovery / null repository state occurs.
     */
    @Test
    fun testSaveCalculationToLogWithNullRepository() = runBlocking {
        val logMainView = LogMainView(
            application = app,
            repository = { null },
            calculatorUiState = { createInitialCalculatorUiState() },
            onShowToast = { toastMessages.add(it) }
        )

        // When saving calculation with a null repository
        logMainView.saveCalculationToLog()
        delay(100.milliseconds)
        ShadowLooper.idleMainLooper()

        // Then verify repository is never called and no toast is displayed when repository is null
        assertAll(
            "Verify saveCalculationToLog handles null repository gracefully without crashing and suppresses toast",
            { verify(repository, never()).saveCalculation(any(CalculationLog::class.java)) },
            { assertTrue(toastMessages.isEmpty(), "Toast message must NOT be displayed when repository is null") }
        )
    }

    /**
     * Tests [LogMainView.deleteLog] when a valid (non-null) repository is provided.
     * Verifies that the repository successfully deletes the log entry by ID and triggers
     * the logbook entry deleted toast notification.
     */
    @Test
    fun testDeleteLogWithValidRepository() = runBlocking {
        // Given a valid repository and a specific log ID to delete
        val logId = 98765L
        val logMainView = LogMainView(
            application = app,
            repository = { repository },
            calculatorUiState = { createInitialCalculatorUiState() },
            onShowToast = { toastMessages.add(it) }
        )

        // When deleting the log entry
        logMainView.deleteLog(logId)
        delay(100.milliseconds)
        ShadowLooper.idleMainLooper()

        // Then verify repository deleteLog was invoked with the correct ID and deletion toast was shown
        val expectedToast = app.getString(R.string.logbook_entry_deleted_toast)
        assertAll(
            "Verify deleteLog successfully invokes repository delete and triggers toast",
            { verify(repository).deleteLog(logId) },
            { assertEquals(1, toastMessages.size, "Exactly one toast message must be emitted upon deleting log") },
            { assertEquals(expectedToast, toastMessages[0], "Toast message must match R.string.logbook_entry_deleted_toast") }
        )
    }

    /**
     * Tests [LogMainView.deleteLog] when the repository provider returns null.
     * Verifies that repository deletion calls are avoided (`never()`) and that no toast
     * message is shown when DB recovery / null repository state occurs.
     */
    @Test
    fun testDeleteLogWithNullRepository() = runBlocking {
        // Given a null repository provider and log ID
        val logId = 98765L
        val logMainView = LogMainView(
            application = app,
            repository = { null },
            calculatorUiState = { createInitialCalculatorUiState() },
            onShowToast = { toastMessages.add(it) }
        )

        // When deleting log with null repository
        logMainView.deleteLog(logId)
        delay(100.milliseconds)
        ShadowLooper.idleMainLooper()

        // Then verify repository deleteLog is never invoked and no toast is displayed
        assertAll(
            "Verify deleteLog handles null repository gracefully and suppresses toast",
            { verify(repository, never()).deleteLog(any(Long::class.java)) },
            { assertTrue(toastMessages.isEmpty(), "Toast message must NOT be displayed when repository is null") }
        )
    }

    /**
     * Tests [LogMainView.clearAllLogs] when a valid (non-null) repository is provided.
     * Verifies that the repository successfully clears all logs and triggers
     * the logbook cleared toast notification.
     */
    @Test
    fun testClearAllLogsWithValidRepository() = runBlocking {
        // Given a valid repository
        val logMainView = LogMainView(
            application = app,
            repository = { repository },
            calculatorUiState = { createInitialCalculatorUiState() },
            onShowToast = { toastMessages.add(it) }
        )

        // When clearing all logs
        logMainView.clearAllLogs()
        delay(100.milliseconds)
        ShadowLooper.idleMainLooper()

        // Then verify repository clearLogs was invoked and cleared toast was shown
        val expectedToast = app.getString(R.string.view_model_logbook_cleared)
        assertAll(
            "Verify clearAllLogs successfully invokes repository clearLogs and triggers toast",
            { verify(repository).clearLogs() },
            { assertEquals(1, toastMessages.size, "Exact one toast message must be emitted upon clearing logs") },
            { assertEquals(expectedToast, toastMessages[0], "Toast message must match R.string.view_model_logbook_cleared") }
        )
    }

    /**
     * Tests [LogMainView.clearAllLogs] when the repository provider returns null.
     * Verifies that repository clear calls are avoided (`never()`) and that no toast
     * message is shown when DB recovery / null repository state occurs.
     */
    @Test
    fun testClearAllLogsWithNullRepository() = runBlocking {
        // Given a null repository provider
        val logMainView = LogMainView(
            application = app,
            repository = { null },
            calculatorUiState = { createInitialCalculatorUiState() },
            onShowToast = { toastMessages.add(it) }
        )

        // When clearing all logs with null repository
        logMainView.clearAllLogs()
        delay(100.milliseconds)
        ShadowLooper.idleMainLooper()

        // Then verify repository clearLogs is never invoked and no toast is displayed
        assertAll(
            "Verify clearAllLogs handles null repository gracefully and suppresses toast",
            { verify(repository, never()).clearLogs() },
            { assertTrue(toastMessages.isEmpty(), "Toast message must NOT be displayed when repository is null") }
        )
    }
}
