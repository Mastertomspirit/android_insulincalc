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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.spiritscorp.R
import network.spiritscorp.ai.GeminiMealService
import network.spiritscorp.ai.GeminiAiModel
import network.spiritscorp.model.CarbUnit
import java.util.Locale

/**
 * ViewModel dedicated to handling AI-related tasks, specifically meal estimation using Gemini.
 *
 * This ViewModel interacts with [GeminiMealService] to process food descriptions and
 * provides state management for the UI via [aiState]. It also handles AI configuration
 * updates (API key and model selection) and bridging AI results to the main calculator.
 *
 * @param application The Android application instance, required to access system resources.
 * @param geminiAiModel The currently selected [GeminiAiModel] for AI requests.
 * @param geminiApiKey  The stored Gemini API key string (empty if not yet configured).
 * @param onUpdateSettings Callback function to persists the new AI configuration.
 * @param onApplyEstimateToCalculator Callback function to push AI estimation results into the calculator fields.
 */
internal class AiViewModel(
    application: Application,
    private var geminiAiModel: GeminiAiModel,
    private var geminiApiKey: String,
    private val onUpdateSettings: (geminiAiModel: GeminiAiModel, geminiApiKey: String) -> Unit,
    private val onApplyEstimateToCalculator: (formattedCarbs: String, mealName: String, notesText: String) -> Unit,
    ) : AndroidViewModel(application) {

    // Service to handle communication with the Gemini API
    private val geminiMealService = GeminiMealService(application)

    // Internal state flow to track the progress and results of AI estimations
    private val _aiState = MutableStateFlow<AiEstimateState>(AiEstimateState.Idle)

    /** External read-only state flow to track the progress and results of AI estimations */
    internal val aiState: StateFlow<AiEstimateState> = _aiState.asStateFlow()

    /**
     * Triggers the AI-powered carbohydrate estimation process.
     *
     * Sets the state to [AiEstimateState.Loading] while the network request is in progress,
     * then updates the state to [AiEstimateState.Success] or [AiEstimateState.Error]
     * based on the service result.
     *
     * @param description A textual description of the meal.
     */
    internal fun estimateMeal(description: String) {
        if (description.isBlank()) return
        _aiState.value = AiEstimateState.Loading
        viewModelScope.launch {
            val result = geminiMealService.estimateCarbsFromDescription(
                foodDescription = description,
                customApiKey = geminiApiKey,
                selectedModel = geminiAiModel
            )
            result.onSuccess { data ->
                _aiState.value = AiEstimateState.Success(data)
            }.onFailure { err ->
                _aiState.value = AiEstimateState.Error( err.message ?: getApplication<Application>().getString(R.string.view_model_ai_error_generic) )
            }
        }
    }

    /**
     * Saves Gemini AI API key and model configuration to user settings.
     *
     * @param geminiApiKey API key string.
     * @param geminiAiModel Selected [GeminiAiModel].
     */
    internal fun saveAiConfiguration(geminiApiKey: String, geminiAiModel: GeminiAiModel ) {
        this.geminiAiModel = geminiAiModel
        this.geminiApiKey = geminiApiKey
        onUpdateSettings(geminiAiModel, geminiApiKey.trim())
    }

    /**
     * Applies estimated carbohydrates and meal details from AI estimation directly into the calculator inputs
     * and switches back to the calculator tab.
     *
     * @param carbsGrams Estimated carbohydrate amount in grams.
     * @param mealName Suggested meal title.
     * @param notesText Detailed breakdown notes.
     */
    internal fun applyEstimatedCarbsToCalculator(
        carbsGrams: Double,
        mealName: String,
        notesText: String,
        currentUnit: CarbUnit,
    ) {
        val converted = currentUnit.fromGrams(carbsGrams)
        val formatted = if (currentUnit == CarbUnit.GRAMS) {
            converted.toInt().toString()
        } else {
            "%.1f".format(Locale.getDefault(), converted)
        }
        onApplyEstimateToCalculator( formatted, mealName, notesText)
    }
}
