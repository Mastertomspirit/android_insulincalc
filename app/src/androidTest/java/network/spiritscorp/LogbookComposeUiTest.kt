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

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import network.spiritscorp.data.AppDatabase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Jetpack Compose UI Test Suite focusing on Logbook screen management.
 *
 * Exercises Logbook workflows:
 * - Single log deletion confirmation dialogs (Confirm & Cancel flows).
 * - Clear all logs confirmation dialogs (Confirm & Cancel flows).
 * - Logbook filter chip switching and time slice navigation (< and >).
 */
@RunWith(AndroidJUnit4::class)
class LogbookComposeUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val CARB_VALUE_45 = "45"

        private const val TITLE_CANCEL_SINGLE_LOG = "Cancel Single Log Test"
        private const val TITLE_DELETE_SINGLE_LOG = "Delete Single Log Test"
        private const val TITLE_ENTRY_1 = "Entry 1"
        private const val TITLE_ENTRY_2 = "Entry 2"
        private const val TITLE_SLICE_NAVIGATION = "Slice Navigation Meal"

        private const val TAG_CARB_INPUT = "carb_input_field"
        private const val TAG_MEAL_NOTES_CARD = "meal_notes_card"
        private const val TAG_MEAL_TITLE_INPUT = "meal_title_input_field"
        private const val TAG_MEAL_NOTES_INPUT = "meal_notes_input_field"
        private const val TAG_SAVE_BUTTON = "save_to_logbook_button"
        private const val TAG_NAV_LOGBOOK = "nav_logbook"
        private const val TAG_FILTER_ALL = "filter_chip_all"
        private const val TAG_FILTER_TODAY = "filter_chip_today"
        private const val TAG_PREVIOUS_SLICE = "filter_previous_slice_button"
        private const val TAG_NEXT_SLICE = "filter_next_slice_button"
        private const val TAG_CLEAR_ALL_BUTTON = "clear_all_logs_button"
        private const val TAG_CONFIRM_SINGLE_DELETE = "confirm_delete_single_log_button"
        private const val TAG_CANCEL_SINGLE_DELETE = "cancel_delete_single_log_button"
        private const val TAG_CONFIRM_CLEAR_ALL = "confirm_clear_all_logs_button"
        private const val TAG_CANCEL_CLEAR_ALL = "cancel_clear_all_logs_button"
    }

    @Before
    fun setUp() {
        // Clear all calculation logs before each test to guarantee a pristine database state
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(context)
        db.calculationLogDao().clearAllLogs()
    }

    private fun saveLogbookEntry(carbs: String, title: String, notes: String? = null) {
        composeTestRule.onNodeWithTag(TAG_CARB_INPUT)
            .assertIsDisplayed("Carbohydrate input field should be visible on Calculator screen")
            .performTextInput(carbs)

        if (title.isNotEmpty() || !notes.isNullOrEmpty()) {
            composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_CARD)
                .performScrollTo()
                .assertIsDisplayed("Meal notes expandable card should be visible")
                .performClick()

            if (title.isNotEmpty()) {
                composeTestRule.onNodeWithTag(TAG_MEAL_TITLE_INPUT)
                    .performScrollTo()
                    .assertIsDisplayed("Meal title input field should be visible")
                    .performTextInput(title)
            }

            if (!notes.isNullOrEmpty()) {
                composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_INPUT)
                    .performScrollTo()
                    .assertIsDisplayed("Meal notes input field should be visible")
                    .performTextInput(notes)
            }
        }

        composeTestRule.onNodeWithTag(TAG_SAVE_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Save to Logbook button should be visible")
            .performClick()
    }

    @Test
    fun testSingleLogDeletionCancelFlow() {
        saveLogbookEntry(CARB_VALUE_45, TITLE_CANCEL_SINGLE_LOG)

        // Navigate to Logbook tab
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK)
            .assertIsDisplayed("Logbook bottom navigation item should be visible")
            .performClick()

        // Select ALL filter chip
        composeTestRule.onNodeWithTag(TAG_FILTER_ALL)
            .assertIsDisplayed("Filter chip 'All' should be visible")
            .performClick()

        // Wait for entry to appear
        composeTestRule.waitUntil(
            conditionDescription = "Log entry '$TITLE_CANCEL_SINGLE_LOG' to appear in logbook",
            timeoutMillis = 5000
        ) {
            composeTestRule.onNode(hasText(TITLE_CANCEL_SINGLE_LOG)).isDisplayed()
        }

        // Trigger single log delete dialog
        val deleteDesc = composeTestRule.activity.getString(R.string.logbook_item_card_delete_desc)
        composeTestRule.onNode(hasContentDescription(deleteDesc))
            .assertIsDisplayed("Delete icon button on log entry item should be visible")
            .performClick()

        // Verify single log dialog buttons are visible and click cancel
        composeTestRule.onNodeWithTag(TAG_CONFIRM_SINGLE_DELETE)
            .assertIsDisplayed("Confirm button in single log deletion dialog should be visible")
        composeTestRule.onNodeWithTag(TAG_CANCEL_SINGLE_DELETE)
            .assertIsDisplayed("Cancel button in single log deletion dialog should be visible")
            .performClick()

        // Verify single log dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_CONFIRM_SINGLE_DELETE)
            .assertDoesNotExist("Confirm button in single log deletion dialog should be dismissed after clicking cancel")
        composeTestRule.onNodeWithTag(TAG_CANCEL_SINGLE_DELETE)
            .assertDoesNotExist("Cancel button in single log deletion dialog should be dismissed after clicking cancel")

        // Verify log entry is still present
        composeTestRule.onNode(hasText(TITLE_CANCEL_SINGLE_LOG))
            .assertIsDisplayed("Log entry '$TITLE_CANCEL_SINGLE_LOG' should remain displayed after cancelling deletion")
    }

    @Test
    fun testSingleLogDeletionConfirmFlow() {
        saveLogbookEntry(CARB_VALUE_45, TITLE_DELETE_SINGLE_LOG)

        // Navigate to Logbook tab
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK)
            .assertIsDisplayed("Logbook bottom navigation item should be visible")
            .performClick()

        // Select ALL filter chip
        composeTestRule.onNodeWithTag(TAG_FILTER_ALL)
            .assertIsDisplayed("Filter chip 'All' should be visible")
            .performClick()

        // Wait for entry to appear
        composeTestRule.waitUntil(
            conditionDescription = "Log entry '$TITLE_DELETE_SINGLE_LOG' to appear in logbook",
            timeoutMillis = 5000
        ) {
            composeTestRule.onNode(hasText(TITLE_DELETE_SINGLE_LOG)).isDisplayed()
        }

        // Trigger single log delete dialog
        val deleteDesc = composeTestRule.activity.getString(R.string.logbook_item_card_delete_desc)
        composeTestRule.onNode(hasContentDescription(deleteDesc))
            .assertIsDisplayed("Delete icon button on log entry item should be visible")
            .performClick()

        // Click confirm deletion button
        composeTestRule.onNodeWithTag(TAG_CONFIRM_SINGLE_DELETE)
            .assertIsDisplayed("Confirm button in single log deletion dialog should be visible")
            .performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_CONFIRM_SINGLE_DELETE)
            .assertDoesNotExist("Confirm button in single log deletion dialog should be dismissed after confirmation")

        // Verify deleted log entry is no longer displayed
        composeTestRule.onNode(hasText(TITLE_DELETE_SINGLE_LOG))
            .assertDoesNotExist("Deleted log entry '$TITLE_DELETE_SINGLE_LOG' should no longer be displayed")
    }

    @Test
    fun testClearAllLogsCancelFlow() {
        saveLogbookEntry(CARB_VALUE_45, TITLE_ENTRY_1)
        saveLogbookEntry(CARB_VALUE_45, TITLE_ENTRY_2)

        // Navigate to Logbook tab
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK)
            .assertIsDisplayed("Logbook bottom navigation item should be visible")
            .performClick()

        // Select ALL filter chip
        composeTestRule.onNodeWithTag(TAG_FILTER_ALL)
            .assertIsDisplayed("Filter chip 'All' should be visible")
            .performClick()

        // Wait for entries to appear
        composeTestRule.waitUntil(
            conditionDescription = "First log entry '$TITLE_ENTRY_1' to appear in logbook",
            timeoutMillis = 5000
        ) {
            composeTestRule.onNode(hasText(TITLE_ENTRY_1)).isDisplayed()
        }

        // Click Clear All Logs top bar icon button
        composeTestRule.onNodeWithTag(TAG_CLEAR_ALL_BUTTON)
            .assertIsDisplayed("Clear all logs top app bar icon button should be visible")
            .performClick()

        // Verify Clear All dialog is displayed and click cancel
        composeTestRule.onNodeWithTag(TAG_CONFIRM_CLEAR_ALL)
            .assertIsDisplayed("Confirm button in Clear All dialog should be visible")
        composeTestRule.onNodeWithTag(TAG_CANCEL_CLEAR_ALL)
            .assertIsDisplayed("Cancel button in Clear All dialog should be visible")
            .performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_CONFIRM_CLEAR_ALL)
            .assertDoesNotExist("Confirm button in Clear All dialog should be dismissed after clicking cancel")

        // Verify entries are still intact in Logbook
        composeTestRule.onNode(hasText(TITLE_ENTRY_1))
            .assertIsDisplayed("Log entry '$TITLE_ENTRY_1' should remain displayed after cancelling Clear All")
        composeTestRule.onNode(hasText(TITLE_ENTRY_2))
            .assertIsDisplayed("Log entry '$TITLE_ENTRY_2' should remain displayed after cancelling Clear All")
    }

    @Test
    fun testClearAllLogsConfirmFlow() {
        saveLogbookEntry(CARB_VALUE_45, TITLE_ENTRY_1)
        saveLogbookEntry(CARB_VALUE_45, TITLE_ENTRY_2)

        // Navigate to Logbook tab
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK)
            .assertIsDisplayed("Logbook bottom navigation item should be visible")
            .performClick()

        // Select ALL filter chip
        composeTestRule.onNodeWithTag(TAG_FILTER_ALL)
            .assertIsDisplayed("Filter chip 'All' should be visible")
            .performClick()

        // Wait for entries to appear
        composeTestRule.waitUntil(
            conditionDescription = "Second log entry '$TITLE_ENTRY_2' to appear in logbook",
            timeoutMillis = 5000
        ) {
            composeTestRule.onNode(hasText(TITLE_ENTRY_2)).isDisplayed()
        }

        // Click Clear All Logs top bar icon button
        composeTestRule.onNodeWithTag(TAG_CLEAR_ALL_BUTTON)
            .assertIsDisplayed("Clear all logs top app bar icon button should be visible")
            .performClick()

        // Confirm clearing all logs
        composeTestRule.onNodeWithTag(TAG_CONFIRM_CLEAR_ALL)
            .assertIsDisplayed("Confirm button in Clear All dialog should be visible")
            .performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_CONFIRM_CLEAR_ALL)
            .assertDoesNotExist("Confirm button in Clear All dialog should be dismissed after confirmation")

        // Verify log entries no longer exist
        composeTestRule.onNode(hasText(TITLE_ENTRY_1))
            .assertDoesNotExist("Log entry '$TITLE_ENTRY_1' should no longer exist after clearing all logs")
        composeTestRule.onNode(hasText(TITLE_ENTRY_2))
            .assertDoesNotExist("Log entry '$TITLE_ENTRY_2' should no longer exist after clearing all logs")

        // Verify empty state title is displayed
        val emptyTitle = composeTestRule.activity.getString(R.string.logbook_empty_title_no_logs)
        composeTestRule.onNode(hasText(emptyTitle))
            .assertIsDisplayed("Empty state title '$emptyTitle' should be displayed after clearing all logs")
    }

    @Test
    fun testLogbookFilterChipsAndSliceNavigation() {
        saveLogbookEntry(CARB_VALUE_45, TITLE_SLICE_NAVIGATION)

        // Navigate to Logbook tab
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK)
            .assertIsDisplayed("Logbook bottom navigation item should be visible")
            .performClick()

        // Select TODAY filter chip
        composeTestRule.onNodeWithTag(TAG_FILTER_TODAY)
            .assertIsDisplayed("Filter chip 'Today' should be visible")
            .performClick()

        // Entry logged today should be displayed
        composeTestRule.waitUntil(
            conditionDescription = "Log entry '$TITLE_SLICE_NAVIGATION' to appear under 'Today' filter",
            timeoutMillis = 5000
        ) {
            composeTestRule.onNode(hasText(TITLE_SLICE_NAVIGATION)).isDisplayed()
        }

        // Click previous slice button (<) to go to yesterday
        composeTestRule.onNodeWithTag(TAG_PREVIOUS_SLICE)
            .assertIsDisplayed("Previous time slice button '<' should be visible")
            .performClick()

        // Entry logged today should not be displayed in yesterday's slice
        composeTestRule.onNode(hasText(TITLE_SLICE_NAVIGATION))
            .assertDoesNotExist("Log entry '$TITLE_SLICE_NAVIGATION' should not be displayed in yesterday's time slice")

        // Click next slice button (>) to return to today
        composeTestRule.onNodeWithTag(TAG_NEXT_SLICE)
            .assertIsDisplayed("Next time slice button '>' should be visible")
            .performClick()

        // Entry should be visible again under today's slice
        composeTestRule.onNode(hasText(TITLE_SLICE_NAVIGATION))
            .assertIsDisplayed("Log entry '$TITLE_SLICE_NAVIGATION' should be displayed again after returning to today's slice")
    }
}
