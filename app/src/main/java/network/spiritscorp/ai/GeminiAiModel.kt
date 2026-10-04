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
 */
enum class GeminiAiModel(
    val modelId: String,
    @StringRes val displayName: Int,
    @StringRes val description: Int
) {
    GEMINI_FLASH_LATEST(
        modelId = "gemini-flash-latest",
        displayName = R.string.gemini_model_flash_latest_title,
        description = R.string.gemini_model_flash_latest_desc
    ),
    GEMINI_FLASH_LITE_LATEST(
        modelId = "gemini-flash-lite-latest",
        displayName = R.string.gemini_model_flash_lite_latest_title,
        description = R.string.gemini_model_flash_lite_latest_desc
    ),
    GEMINI_3_8_FLASH(
        modelId = "gemini-3.8-flash",
        displayName = R.string.gemini_model_3_8_flash_title,
        description = R.string.gemini_model_3_8_flash_desc
    ),
    GEMINI_3_7_FLASH(
        modelId = "gemini-3.7-flash",
        displayName = R.string.gemini_model_3_7_flash_title,
        description = R.string.gemini_model_3_7_flash_desc
    ),
    GEMINI_3_6_FLASH(
        modelId = "gemini-3.6-flash",
        displayName = R.string.gemini_model_3_6_flash_title,
        description = R.string.gemini_model_3_6_flash_desc
    ),
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = R.string.gemini_model_3_5_flash_title,
        description = R.string.gemini_model_3_5_flash_desc
    ),
    GEMINI_3_1_PRO(
        modelId = "gemini-3.1-pro-preview",
        displayName = R.string.gemini_model_3_1_pro_title,
        description = R.string.gemini_model_3_1_pro_desc
    );

    companion object {
        fun fromModelId(id: String?): GeminiAiModel {
            if (id.isNullOrBlank()) return GEMINI_FLASH_LITE_LATEST
            return entries.find { it.modelId.equals(id.trim(), ignoreCase = true) }
                ?: when {
                    id.contains("flash-latest", ignoreCase = true) -> GEMINI_FLASH_LATEST
                    id.contains("pro", ignoreCase = true) -> GEMINI_3_1_PRO
                    id.contains("3.8", ignoreCase = true) -> GEMINI_3_8_FLASH
                    id.contains("3.7", ignoreCase = true) -> GEMINI_3_7_FLASH
                    id.contains("3.6", ignoreCase = true) -> GEMINI_3_6_FLASH
                    id.contains("3.5", ignoreCase = true) -> GEMINI_3_5_FLASH
                    else -> GEMINI_FLASH_LITE_LATEST
                }
        }
    }
}