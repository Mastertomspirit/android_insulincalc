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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Jetpack Compose UI Test Suite focusing on Calculator screen features.
 *
 * Exercises Calculator workflows:
 * - Entering carbohydrates, meal titles, notes and saving to the logbook.
 * - Glucose correction input and insulin calculation verification.
 * - Clearing carbohydrate inputs.
 * - Meal titles and notes input fields.
 */
@RunWith(AndroidJUnit4::class)
class CalculatorComposeUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val CARB_INPUT_50 = "50"
        private const val CARB_INPUT_40 = "40"
        private const val CARB_INPUT_65 = "65"
        private const val DEFAULT_CARB_TEXT = "0"

        private const val MEAL_TITLE_SPECIAL_DINNER = "My Special Dinner"
        private const val MEAL_NOTES_AVOCADO = "Low carb with avocado"

        private const val MEAL_TITLE_TEST_LUNCH = "Test Lunch"
        private const val MEAL_NOTES_SALAD = "Low-carb salad with olive oil"

        private const val GLUCOSE_CURRENT = "180"
        private const val GLUCOSE_TARGET = "100"
        private const val GLUCOSE_CORRECTION_FACTOR = "40"

        private const val TAG_CARB_INPUT = "carb_input_field"
        private const val TAG_CLEAR_CARB_BUTTON = "clear_carb_button"
        private const val TAG_MEAL_NOTES_CARD = "meal_notes_card"
        private const val TAG_MEAL_TITLE_INPUT = "meal_title_input_field"
        private const val TAG_MEAL_NOTES_INPUT = "meal_notes_input_field"
        private const val TAG_GLUCOSE_CORRECTION_CARD = "glucose_correction_card"
        private const val TAG_CURRENT_GLUCOSE = "current_glucose_field"
        private const val TAG_TARGET_GLUCOSE = "target_glucose_field"
        private const val TAG_CORRECTION_FACTOR = "correction_factor_field"
        private const val TAG_INSULIN_RESULT_CARD = "insulin_result_card"
        private const val TAG_SAVE_BUTTON = "save_to_logbook_button"
        private const val TAG_NAV_LOGBOOK = "nav_logbook"
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

    @Test
    fun testSaveCarbsAndCustomDetailsToLogbookAndVerify() {
        val carbs = CARB_INPUT_50
        val title = MEAL_TITLE_SPECIAL_DINNER
        val notes = MEAL_NOTES_AVOCADO

        // 1. Enter carbohydrates
        composeTestRule.onNodeWithTag(TAG_CARB_INPUT)
            .assertIsDisplayed("Carbohydrate input field should be visible")
            .performTextInput(carbs)

        // 2. Expand Meal Notes card
        composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_CARD)
            .performScrollTo()
            .assertIsDisplayed("Meal notes expandable card should be visible")
            .performClick()

        // 3. Enter custom meal title
        composeTestRule.onNodeWithTag(TAG_MEAL_TITLE_INPUT)
            .performScrollTo()
            .assertIsDisplayed("Meal title input field should be visible")
            .performTextInput(title)

        // 4. Enter custom notes
        composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_INPUT)
            .performScrollTo()
            .assertIsDisplayed("Meal notes input field should be visible")
            .performTextInput(notes)

        // 5. Click Save to Logbook
        composeTestRule.onNodeWithTag(TAG_SAVE_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Save to Logbook button should be visible")
            .performClick()

        // 6. Navigate to Logbook tab
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK)
            .assertIsDisplayed("Logbook bottom navigation item should be visible")
            .performClick()

        // 7. Wait for the saved entry to appear in the logbook list
        composeTestRule.waitUntil(
            conditionDescription = "Saved meal entry '$title' to appear in the logbook list",
            timeoutMillis = 5000
        ) {
            composeTestRule.onNode(hasText(title)).isDisplayed()
        }

        // 8. Verify both custom title and notes are displayed in the logbook
        composeTestRule.onNode(hasText(title))
            .assertIsDisplayed("Saved meal title '$title' should be displayed in the logbook")
        composeTestRule.onNode(hasText(notes))
            .assertIsDisplayed("Saved meal notes '$notes' should be displayed in the logbook")
    }

    @Test
    fun testGlucoseCorrectionAndInsulinCalculation() {
        val carbs = CARB_INPUT_40
        val currentGlucose = GLUCOSE_CURRENT
        val targetGlucose = GLUCOSE_TARGET
        val correctionFactor = GLUCOSE_CORRECTION_FACTOR

        // 1. Enter carbohydrates
        composeTestRule.onNodeWithTag(TAG_CARB_INPUT)
            .assertIsDisplayed("Carbohydrate input field should be visible")
            .performTextInput(carbs)

        // 2. Expand the Glucose Correction card
        composeTestRule.onNodeWithTag(TAG_GLUCOSE_CORRECTION_CARD)
            .performScrollTo()
            .assertIsDisplayed("Glucose correction expandable card should be visible")
            .performClick()

        // 3. Enter glucose correction values
        composeTestRule.onNodeWithTag(TAG_CURRENT_GLUCOSE)
            .performScrollTo()
            .assertIsDisplayed("Current glucose field should be visible")
            .performTextInput(currentGlucose)

        composeTestRule.onNodeWithTag(TAG_TARGET_GLUCOSE)
            .performScrollTo()
            .assertIsDisplayed("Target glucose field should be visible")
            .performTextInput(targetGlucose)

        composeTestRule.onNodeWithTag(TAG_CORRECTION_FACTOR)
            .performScrollTo()
            .assertIsDisplayed("Correction factor field should be visible")
            .performTextInput(correctionFactor)

        // 4. Verify that the insulin result card updates and is displayed
        composeTestRule.onNodeWithTag(TAG_INSULIN_RESULT_CARD)
            .performScrollTo()
            .assertIsDisplayed("Insulin calculation result card should be visible")
    }

    @Test
    fun testClearCarbInputFunctionality() {
        val carbs = CARB_INPUT_65

        // 1. Enter carb input
        composeTestRule.onNodeWithTag(TAG_CARB_INPUT)
            .assertIsDisplayed("Carbohydrate input field should be visible")
            .performTextInput(carbs)

        // 2. Click clear carb button
        composeTestRule.onNodeWithTag(TAG_CLEAR_CARB_BUTTON)
            .assertIsDisplayed("Clear carb icon button should be visible")
            .performClick()

        // 3. Verify carb input field is reset to "0" and still displayed
        composeTestRule.onNodeWithTag(TAG_CARB_INPUT)
            .assertTextContains(DEFAULT_CARB_TEXT, "Carbohydrate input field should display default value '0' after clearing")
            .assertIsDisplayed("Carbohydrate input field should remain displayed after clearing")
    }

    @Test
    fun testMealTitleAndNotesInputFunctionality() {
        val title = MEAL_TITLE_TEST_LUNCH
        val notes = MEAL_NOTES_SALAD

        // 1. Expand the Meal Notes card
        composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_CARD)
            .performScrollTo()
            .assertIsDisplayed("Meal notes expandable card should be visible")
            .performClick()

        // 2. Enter meal title
        composeTestRule.onNodeWithTag(TAG_MEAL_TITLE_INPUT)
            .performScrollTo()
            .assertIsDisplayed("Meal title input field should be visible")
            .performTextInput(title)

        // 3. Enter meal notes
        composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_INPUT)
            .performScrollTo()
            .assertIsDisplayed("Meal notes input field should be visible")
            .performTextInput(notes)

        // 4. Verify both fields remain displayed
        composeTestRule.onNodeWithTag(TAG_MEAL_TITLE_INPUT)
            .assertIsDisplayed("Meal title input field should remain displayed")
        composeTestRule.onNodeWithTag(TAG_MEAL_NOTES_INPUT)
            .assertIsDisplayed("Meal notes input field should remain displayed")
    }
}
