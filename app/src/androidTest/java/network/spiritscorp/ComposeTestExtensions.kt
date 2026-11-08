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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains

/**
 * Extension function on [SemanticsNodeInteraction] that wraps [assertIsDisplayed]
 * with a custom English failure message to provide clear diagnostics in UI test logs.
 *
 * @param message Descriptive error message to report if the node is not displayed.
 */
fun SemanticsNodeInteraction.assertIsDisplayed(message: String): SemanticsNodeInteraction {
    try {
        return assertIsDisplayed()
    } catch (e: AssertionError) {
        throw AssertionError("$message: ${e.message}", e)
    }
}

/**
 * Extension function on [SemanticsNodeInteraction] that wraps [assertDoesNotExist]
 * with a custom English failure message to provide clear diagnostics in UI test logs.
 *
 * @param message Descriptive error message to report if the node unexpected exists.
 */
fun SemanticsNodeInteraction.assertDoesNotExist(message: String) {
    try {
        assertDoesNotExist()
    } catch (e: AssertionError) {
        throw AssertionError("$message: ${e.message}", e)
    }
}

/**
 * Extension function on [SemanticsNodeInteraction] that wraps [assertTextContains]
 * with a custom English failure message to provide clear diagnostics in UI test logs.
 *
 * @param substring Expected substring to check for.
 * @param message Descriptive error message to report if the node does not contain the expected text.
 */
fun SemanticsNodeInteraction.assertTextContains(substring: String, message: String): SemanticsNodeInteraction {
    try {
        return assertTextContains(substring)
    } catch (e: AssertionError) {
        throw AssertionError("$message: ${e.message}", e)
    }
}
