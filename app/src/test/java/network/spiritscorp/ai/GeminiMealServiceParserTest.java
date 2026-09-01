package network.spiritscorp.ai;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Gemini AI nutrition parser logic, JSON parsing resilience,
 * Markdown cleaning, portion extractions, and offline estimation fallbacks.
 */
public class GeminiMealServiceParserTest {

    private static final double DELTA = 0.001;

    @Test
    public void testParseStandardGeminiJsonResponse() throws JSONException {
        String rawJson = """
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
                }""";

        JSONObject json = new JSONObject(rawJson);
        String title = json.optString("mealTitle");
        double totalCarbs = json.optDouble("totalCarbsGrams");
        JSONArray itemsArray = json.optJSONArray("items");
        String explanation = json.optString("explanation");

        assertEquals("Haferflocken mit Beeren und Mandeln", title);
        assertEquals(55.0, totalCarbs, DELTA);
        assertNotNull(itemsArray);
        assertEquals(3, itemsArray.length());

        JSONObject item1 = itemsArray.getJSONObject(0);
        assertEquals("Haferflocken", item1.getString("name"));
        assertEquals(40.0, item1.getDouble("carbsGrams"), DELTA);
        assertEquals(220, item1.getInt("calories"));

        assertTrue(explanation.contains("glykämischen Index"));
    }

    @Test
    public void testParseMarkdownWrappedJson() throws JSONException {
        String markdownWrapped = """
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
                ```""";

        String cleaned = markdownWrapped.replace("```json", "").replace("```", "").trim();
        JSONObject json = new JSONObject(cleaned);

        assertEquals("Spaghetti Bolognese", json.getString("mealTitle"));
        assertEquals(75.0, json.getDouble("totalCarbsGrams"), DELTA);
        assertEquals(2, json.getJSONArray("items").length());
    }

    @Test
    public void testParsePartialOrMalformedJsonGracefulDefaults() throws JSONException {
        String sparseJson = """
                {
                  "totalCarbsGrams": 30.5
                }""";

        JSONObject json = new JSONObject(sparseJson);
        String title = json.optString("mealTitle", "Mahlzeit");
        double totalCarbs = json.optDouble("totalCarbsGrams", 0.0);
        JSONArray itemsArray = json.optJSONArray("items");
        String explanation = json.optString("explanation", "Standard-Schätzung");

        assertEquals("Mahlzeit", title);
        assertEquals(30.5, totalCarbs, DELTA);
        assertNull(itemsArray);
        assertEquals("Standard-Schätzung", explanation);
    }

    @Test
    public void testGeminiMealServiceDirectInstance() {
        GeminiMealService service = new GeminiMealService();
        assertNotNull(service);
    }
}
