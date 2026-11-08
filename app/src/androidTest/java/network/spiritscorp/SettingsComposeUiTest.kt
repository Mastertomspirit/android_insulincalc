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

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import network.spiritscorp.data.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Jetpack Compose UI Test Suite focusing on Settings screen workflows.
 *
 * Exercises Settings workflows:
 * - Navigating to the Settings screen and verifying configuration sections.
 * - Expanding therapy factors and backup sections and clicking buttons.
 * - Resetting all data confirmation dialog (Confirm & Cancel flows).
 * - Saving updated settings to Room.
 */
@RunWith(AndroidJUnit4::class)
class SettingsComposeUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val TAG_NAV_SETTINGS = "nav_settings"
        private const val TAG_SETTINGS_FACTORS_CARD = "settings_factors_card"
        private const val TAG_INCREASE_MORNING_FACTOR = "increase_morning_factor"
        private const val TAG_DECREASE_MORNING_FACTOR = "decrease_morning_factor"
        private const val TAG_BACKUP_SECTION_CARD = "settings_backup_card"
        private const val TAG_RESET_ALL_DATA_BUTTON = "reset_all_data_button"
        private const val TAG_CONFIRM_RESET_ALL_DATA = "confirm_reset_all_data_button"
        private const val TAG_CANCEL_RESET_ALL_DATA = "cancel_reset_all_data_button"
        private const val TAG_SAVE_SETTINGS_BUTTON = "save_settings_button"
    }

    @Before
    fun setUp() {
        // Clear all calculation logs before each test to guarantee a pristine database state
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(context)
        db.calculationLogDao().clearAllLogs()
    }

    @After
    fun tearDown() {
        // Optional post-test cleanup
    }

    private fun navigateToSettings() {
        composeTestRule.onNodeWithTag(TAG_NAV_SETTINGS)
            .assertIsDisplayed("Settings bottom navigation item should be visible")
            .performClick()
    }

    @Test
    fun testNavigateToSettingsTabAndVerifySaveButtonDisplayed() {
        navigateToSettings()

        composeTestRule.onNodeWithTag(TAG_SAVE_SETTINGS_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Save Settings button should be visible on the Settings screen")
    }

    @Test
    fun testTherapyFactorsExpandAndAdjust() {
        navigateToSettings()

        // Expand Therapy Factors section card
        composeTestRule.onNodeWithTag(TAG_SETTINGS_FACTORS_CARD)
            .performScrollTo()
            .assertIsDisplayed("Therapy factors expandable section card should be visible")
            .performClick()

        // Test incrementing morning factor
        composeTestRule.onNodeWithTag(TAG_INCREASE_MORNING_FACTOR)
            .performScrollTo()
            .assertIsDisplayed("Increase morning factor button should be visible when factors section is expanded")
            .performClick()

        // Test decrementing morning factor
        composeTestRule.onNodeWithTag(TAG_DECREASE_MORNING_FACTOR)
            .performScrollTo()
            .assertIsDisplayed("Decrease morning factor button should be visible when factors section is expanded")
            .performClick()
    }

    @Test
    fun testResetAllDataDialogCancelFlow() {
        navigateToSettings()

        // Expand Backup & Restore section card first so reset button becomes visible
        composeTestRule.onNodeWithTag(TAG_BACKUP_SECTION_CARD)
            .performScrollTo()
            .assertIsDisplayed("Backup & Restore expandable section card should be visible")
            .performClick()

        // Click Reset All Data button
        composeTestRule.onNodeWithTag(TAG_RESET_ALL_DATA_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Reset all data button should be visible in settings")
            .performClick()

        // Verify confirmation dialog options are displayed and click cancel
        composeTestRule.onNodeWithTag(TAG_CONFIRM_RESET_ALL_DATA)
            .assertIsDisplayed("Confirm button in reset all data dialog should be visible")
        composeTestRule.onNodeWithTag(TAG_CANCEL_RESET_ALL_DATA)
            .assertIsDisplayed("Cancel button in reset all data dialog should be visible")
            .performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_CONFIRM_RESET_ALL_DATA)
            .assertDoesNotExist("Confirm button in reset all data dialog should be dismissed after clicking cancel")
        composeTestRule.onNodeWithTag(TAG_CANCEL_RESET_ALL_DATA)
            .assertDoesNotExist("Cancel button in reset all data dialog should be dismissed after clicking cancel")
    }

    @Test
    fun testResetAllDataDialogConfirmFlow() {
        navigateToSettings()

        // Expand Backup & Restore section card first so reset button becomes visible
        composeTestRule.onNodeWithTag(TAG_BACKUP_SECTION_CARD)
            .performScrollTo()
            .assertIsDisplayed("Backup & Restore expandable section card should be visible")
            .performClick()

        // Click Reset All Data button
        composeTestRule.onNodeWithTag(TAG_RESET_ALL_DATA_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Reset all data button should be visible in settings")
            .performClick()

        // Click confirm button in reset all data dialog
        composeTestRule.onNodeWithTag(TAG_CONFIRM_RESET_ALL_DATA)
            .assertIsDisplayed("Confirm button in reset all data dialog should be visible")
            .performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_CONFIRM_RESET_ALL_DATA)
            .assertDoesNotExist("Confirm button in reset all data dialog should be dismissed after confirmation")
    }

    @Test
    fun testSaveSettingsAction() {
        navigateToSettings()

        // Click Save Settings button
        composeTestRule.onNodeWithTag(TAG_SAVE_SETTINGS_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Save Settings button should be visible")
            .performClick()

        // Verify Save Settings button remains visible after save action
        composeTestRule.onNodeWithTag(TAG_SAVE_SETTINGS_BUTTON)
            .assertIsDisplayed("Save Settings button should remain displayed after saving settings")
    }
}
