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

/**
 * Supported Gemini AI Models for carb & meal estimation.
 */
enum class GeminiAiModel(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite (Standard)",
        description = "Superschnell & extrem sparsam im Tokenverbrauch"
    ),
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Optimale Balance aus Schnelligkeit und Präzision"
    ),
    GEMINI_3_5_PRO(
        modelId = "gemini-3.6-flash",
        displayName = "Gemini 3.6 Flash",
        description = "Erweiterte Analyse für komplexe Gerichte & Rezepte"
    ),
    GEMINI_3_7_FLASH(
        modelId = "gemini-3.7-flash",
        displayName = "Gemini 3.7 Flash (Hybrid-Thinking)",
        description = "Neuestes Modell mit optionaler tiefer Denkfähigkeit"
    );

    companion object {
        fun fromModelId(id: String?): GeminiAiModel {
            return entries.find { it.modelId.equals(id, ignoreCase = true) } ?: GeminiAiModel.GEMINI_3_5_FLASH_LITE
        }
    }
}
