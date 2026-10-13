package network.spiritscorp.ai

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

import androidx.annotation.StringRes
import network.spiritscorp.R

/**
 * Supported Gemini AI Models for carb & meal estimation.
 *
 * Defines the available Google Gemini models that users can select within user settings
 * to estimate carbohydrates, fats, proteins, and insulin requirements from natural language food descriptions.
 * Each entry provides a unique model API identifier string along with localized string resource identifiers
 * for display titles and descriptive explanations.
 *
 * @property modelId The exact API model string identifier used in REST/gRPC endpoints (e.g., `"gemini-3.5-flash"`).
 * @property displayName The string resource ID for the human-readable model title.
 * @property description The string resource ID for the descriptive details of the model characteristics.
 */
enum class GeminiAiModel(
    val modelId: String,
    @StringRes val displayName: Int,
    @StringRes val description: Int
) {
    /**
     * Gemini 3.1 Flash Lite model optimized for ultra-fast, lightweight mobile estimations.
     */
    GEMINI_3_1_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        displayName = R.string.gemini_model_3_1_flash_lite_title,
        description = R.string.gemini_model_3_1_flash_lite_desc
    ),

    /**
     * Gemini 3.5 Flash Lite model offering balanced speed and efficiency.
     */
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = R.string.gemini_model_3_5_flash_lite_title,
        description = R.string.gemini_model_3_5_flash_lite_desc
    ),

    /**
     * Gemini 3.5 Flash standard model providing reliable multimodal and text understanding.
     */
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = R.string.gemini_model_3_5_flash_title,
        description = R.string.gemini_model_3_5_flash_desc
    ),

    /**
     * Gemini 3.6 Flash model with improved reasoning capabilities.
     */
    GEMINI_3_6_FLASH(
        modelId = "gemini-3.6-flash",
        displayName = R.string.gemini_model_3_6_flash_title,
        description = R.string.gemini_model_3_6_flash_desc
    ),

    /**
     * Gemini 3.7 Flash advanced high-performance model.
     */
    GEMINI_3_7_FLASH(
        modelId = "gemini-3.7-flash",
        displayName = R.string.gemini_model_3_7_flash_title,
        description = R.string.gemini_model_3_7_flash_desc
    ),

    /**
     * Gemini 3.8 Flash top-tier state-of-the-art flash model.
     */
    GEMINI_3_8_FLASH(
        modelId = "gemini-3.8-flash",
        displayName = R.string.gemini_model_3_8_flash_title,
        description = R.string.gemini_model_3_8_flash_desc
    );

    companion object {
        /**
         * Resolves a [GeminiAiModel] from a given model identifier string.
         *
         * Performs case-insensitive exact matching against known [modelId] values. If no exact match is found,
         * falls back to partial version matching or defaults to [GEMINI_3_5_FLASH_LITE] when blank or unrecognized.
         *
         * @param id The model ID string to resolve.
         * @return The matching [GeminiAiModel], or [GEMINI_3_5_FLASH_LITE] as the default fallback.
         */
        fun fromModelId(id: String?): GeminiAiModel {
            if (id.isNullOrBlank()) return GEMINI_3_5_FLASH_LITE
            return entries.find { it.modelId.equals(id.trim(), ignoreCase = true) }
                ?: when {
                    id.contains("3.8", ignoreCase = true) -> GEMINI_3_8_FLASH
                    id.contains("3.7", ignoreCase = true) -> GEMINI_3_7_FLASH
                    id.contains("3.6", ignoreCase = true) -> GEMINI_3_6_FLASH
                    id.contains("3.5-flash-lite", ignoreCase = true) -> GEMINI_3_5_FLASH_LITE
                    id.contains("3.5", ignoreCase = true) -> GEMINI_3_5_FLASH
                    id.contains("3.1", ignoreCase = true) -> GEMINI_3_1_FLASH_LITE
                    else -> GEMINI_3_5_FLASH_LITE
                }
        }
    }
}