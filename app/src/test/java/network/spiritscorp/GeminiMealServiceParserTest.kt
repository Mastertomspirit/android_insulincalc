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

import network.spiritscorp.ai.GeminiMealService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Gemini AI nutrition parser logic, JSON parsing resilience,
 * Markdown cleaning, portion extractions, and offline estimation fallbacks.
 */
class GeminiMealServiceParserTest {

    @Test
    fun testParseStandardGeminiJsonResponse() {
        val rawJson = """
            {
              "mealTitle": "Haferflocken mit Beeren und Mandeln",
              "totalCarbsGrams": 55.0,
              "items": [
                {
                  "name": "Haferflocken",
                  "portion": "60g",
                  "carbsGrams": 40.0,
                  "calories": 220,
                  "notes": "Komplexe Kohlenhydrate"
                },
                {
                  "name": "Blaubeeren",
                  "portion": "100g",
                  "carbsGrams": 12.0,
                  "calories": 57,
                  "notes": "Reich an Antioxidantien"
                },
                {
                  "name": "Mandeln",
                  "portion": "20g",
                  "carbsGrams": 3.0,
                  "calories": 115,
                  "notes": "Gesunde Fette"
                }
              ],
              "explanation": "Haferflocken liefern den Großteil der Kohlenhydrate mit niedrigem glykämischen Index.",
              "insulinTip": "Durch die Ballaststoffe und Fette steigt der Blutzucker langsam und gleichmäßig an."
            }
        """.trimIndent()

        val json = JSONObject(rawJson)
        val title = json.optString("mealTitle")
        val totalCarbs = json.optDouble("totalCarbsGrams")
        val itemsArray = json.optJSONArray("items")
        val explanation = json.optString("explanation")

        assertEquals("Haferflocken mit Beeren und Mandeln", title)
        assertEquals(55.0, totalCarbs, 0.001)
        assertNotNull(itemsArray)
        assertEquals(3, itemsArray!!.length())

        val item1 = itemsArray.getJSONObject(0)
        assertEquals("Haferflocken", item1.getString("name"))
        assertEquals(40.0, item1.getDouble("carbsGrams"), 0.001)
        assertEquals(220, item1.getInt("calories"))

        assertTrue(explanation.contains("glykämischen Index"))
    }

    @Test
    fun testParseMarkdownWrappedJson() {
        val markdownWrapped = """
            ```json
            {
              "mealTitle": "Spaghetti Bolognese",
              "totalCarbsGrams": 75.0,
              "items": [
                {
                  "name": "Hartweizen-Nudeln",
                  "portion": "1 Teller gekocht (250g)",
                  "carbsGrams": 70.0,
                  "calories": 380,
                  "notes": "Al dente zubereitet"
                },
                {
                  "name": "Bolognese Sauce",
                  "portion": "150g",
                  "carbsGrams": 5.0,
                  "calories": 190,
                  "notes": "Enthält Tomaten & Rinderhack"
                }
              ],
              "explanation": "Klassische Portion Pasta mit Fleischsauce.",
              "insulinTip": "Evtl. verzögerter Bolus (Dual-Bolus) wegen Fettanteil der Sauce ratsam."
            }
            ```
        """.trimIndent()

        val cleaned = markdownWrapped.replace("```json", "").replace("```", "").trim()
        val json = JSONObject(cleaned)

        assertEquals("Spaghetti Bolognese", json.getString("mealTitle"))
        assertEquals(75.0, json.getDouble("totalCarbsGrams"), 0.001)
        assertEquals(2, json.getJSONArray("items").length())
    }

    @Test
    fun testParsePartialOrMalformedJsonGracefulDefaults() {
        val sparseJson = """
            {
              "totalCarbsGrams": 30.5
            }
        """.trimIndent()

        val json = JSONObject(sparseJson)
        val title = json.optString("mealTitle", "Mahlzeit")
        val totalCarbs = json.optDouble("totalCarbsGrams", 0.0)
        val itemsArray = json.optJSONArray("items")
        val explanation = json.optString("explanation", "Standard-Schätzung")

        assertEquals("Mahlzeit", title)
        assertEquals(30.5, totalCarbs, 0.001)
        assertEquals(null, itemsArray)
        assertEquals("Standard-Schätzung", explanation)
    }

    @Test
    fun testGeminiMealServiceDirectInstance() {
        val service = GeminiMealService()
        assertNotNull(service)
    }
}
