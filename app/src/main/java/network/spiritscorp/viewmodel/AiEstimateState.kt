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

import network.spiritscorp.ai.MealEstimateResult

/**
 * Sealed interface representing the asynchronous state of AI meal estimation requests.
 */
sealed interface AiEstimateState {
    /** Idle state when no AI estimation is currently running. */
    object Idle : AiEstimateState
    /** Loading state while communicating with the Gemini AI model. */
    object Loading : AiEstimateState
    /** Success state containing the parsed [MealEstimateResult]. */
    data class Success(val result: MealEstimateResult) : AiEstimateState
    /** Error state containing failure details. */
    data class Error(val message: String) : AiEstimateState
}