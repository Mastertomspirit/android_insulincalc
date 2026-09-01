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
    GEMINI_3_5_FLASH(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash (Empfohlen)",
        description = "Schnell, präzise und optimiert für Nährwertanalysen"
    ),
    GEMINI_3_1_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash-Lite",
        description = "Superschnell & extrem sparsam im Tokenverbrauch"
    ),
    GEMINI_3_1_PRO(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        description = "Erweiterte Analyse für komplexe Menüs & Rezepte"
    ),
    GEMINI_2_5_FLASH(
        modelId = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        description = "Bewährtes, stabiles Standard-Modell"
    );

    companion object {
        fun fromModelId(id: String?): GeminiAiModel {
            if (id.isNullOrBlank()) return GEMINI_3_5_FLASH
            return entries.find { it.modelId.equals(id.trim(), ignoreCase = true) }
                ?: when {
                    id.contains("lite", ignoreCase = true) -> GEMINI_3_1_FLASH_LITE
                    id.contains("pro", ignoreCase = true) -> GEMINI_3_1_PRO
                    id.contains("2.5", ignoreCase = true) -> GEMINI_2_5_FLASH
                    else -> GEMINI_3_5_FLASH
                }
        }
    }
}

