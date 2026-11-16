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
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Jetpack Compose UI Test Suite focusing on AI tab and configuration UI workflows.
 *
 * Exercises AI UI workflows:
 * - Switching to the AI Estimator tab.
 * - Expanding the AI configuration card.
 * - Entering a custom Gemini API key.
 * - Clicking the save configuration button.
 */
@RunWith(AndroidJUnit4::class)
class AiComposeUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val TAG_NAV_AI = "nav_ai_estimator"
        private const val TAG_AI_CONFIG_CARD = "ai_config_card"
        private const val TAG_AI_API_KEY_INPUT = "ai_api_key_input"
        private const val TAG_SAVE_CONFIG_BUTTON = "save_ai_config_button"
        private const val TEST_API_KEY = "AIzaSyTestApiKeyForTesting123456789"
    }

    @Test
    fun testNavigateToAiTabExpandConfigCardEnterApiKeyAndSave() {
        // 1. Switch to the AI tab
        composeTestRule.onNodeWithTag(TAG_NAV_AI)
            .assertIsDisplayed("AI Estimator bottom navigation item should be visible")
            .performClick()

        // 2. Expand the AI config card
        composeTestRule.onNodeWithTag(TAG_AI_CONFIG_CARD)
            .performScrollTo()
            .assertIsDisplayed("AI config card should be visible on AI screen")
            .performClick()

        // 3. Entering API key
        composeTestRule.onNodeWithTag(TAG_AI_API_KEY_INPUT)
            .performScrollTo()
            .assertIsDisplayed("AI API key input field should be visible when config card is expanded")
            .performTextInput(TEST_API_KEY)

        // 4. Save configuration
        composeTestRule.onNodeWithTag(TAG_SAVE_CONFIG_BUTTON)
            .performScrollTo()
            .assertIsDisplayed("Save AI configuration button should be visible")
            .performClick()
    }
}
