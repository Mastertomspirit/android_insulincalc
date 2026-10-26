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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying the actual parsing logic inside GeminiMealService.
 * Tests full Gemini API HTTP response structure, Markdown backtick extraction,
 * weight, calories, carbohydrate/fat/protein macro parsing, automatic fallback sums, and default handling.
 */
public class GeminiMealServiceParserTest {

    private static final double DELTA = 0.001;
    private GeminiMealService service;

    @Before
    public void setUp() {
        service = new GeminiMealService();
    }

    @Test
    public void testParseFullGeminiApiCandidateResponse() {
        String fullApiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\n  \\"mealTitle\\": \\"Haferflocken mit Beeren und Mandeln\\",\\n  \\"totalWeightGrams\\": 180.0,\\n  \\"totalCarbsGrams\\": 55.0,\\n  \\"totalFatGrams\\": 12.0,\\n  \\"totalProteinGrams\\": 15.0,\\n  \\"totalCalories\\": 392,\\n  \\"items\\": [\\n    {\\n      \\"name\\": \\"Haferflocken\\",\\n      \\"portion\\": \\"60g\\",\\n      \\"weightGrams\\": 60.0,\\n      \\"carbsGrams\\": 40.0,\\n      \\"fatGrams\\": 4.0,\\n      \\"proteinGrams\\": 8.0,\\n      \\"calories\\": 220,\\n      \\"notes\\": \\"Komplexe Kohlenhydrate\\"\\n    },\\n    {\\n      \\"name\\": \\"Blaubeeren\\",\\n      \\"portion\\": \\"100g\\",\\n      \\"weightGrams\\": 100.0,\\n      \\"carbsGrams\\": 12.0,\\n      \\"fatGrams\\": 0.5,\\n      \\"proteinGrams\\": 1.0,\\n      \\"calories\\": 57,\\n      \\"notes\\": \\"Reich an Antioxidantien\\"\\n    },\\n    {\\n      \\"name\\": \\"Mandeln\\",\\n      \\"portion\\": \\"20g\\",\\n      \\"weightGrams\\": 20.0,\\n      \\"carbsGrams\\": 3.0,\\n      \\"fatGrams\\": 7.5,\\n      \\"proteinGrams\\": 6.0,\\n      \\"calories\\": 115,\\n      \\"notes\\": \\"Gesunde Fette\\"\\n    }\\n  ],\\n  \\"explanation\\": \\"Haferflocken liefern den Großteil der Kohlenhydrate.\\",\\n  \\"insulinTip\\": \\"Durch Ballaststoffe und Fette langsamere Resorption.\\"\\n}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }""";

        MealEstimateResult result = service.parseGeminiApiResponse(
                fullApiResponse,
                "Haferflocken",
                "gemini-3.8-flash"
        );

        assertNotNull(result);
        assertEquals("Haferflocken mit Beeren und Mandeln", result.getMealTitle());
        assertEquals(180.0, result.getTotalWeightGrams(), DELTA);
        assertEquals(55.0, result.getTotalCarbsGrams(), DELTA);
        assertEquals(12.0, result.getTotalFatGrams(), DELTA);
        assertEquals(15.0, result.getTotalProteinGrams(), DELTA);
        assertEquals(392, result.getTotalCalories());
        assertEquals("gemini-3.8-flash", result.getModelUsed());
        assertFalse(result.isOfflineEstimate());

        assertEquals(3, result.getItems().size());

        MealItemDetail item1 = result.getItems().getFirst();
        assertEquals("Haferflocken", item1.getName());
        assertEquals("60g", item1.getPortion());
        assertEquals(60.0, item1.getWeightGrams(), DELTA);
        assertEquals(40.0, item1.getCarbsGrams(), DELTA);
        assertEquals(4.0, item1.getFatGrams(), DELTA);
        assertEquals(8.0, item1.getProteinGrams(), DELTA);
        assertEquals(220, item1.getCalories());
        assertEquals("Komplexe Kohlenhydrate", item1.getNotes());

        MealItemDetail item3 = result.getItems().get(2);
        assertEquals("Mandeln", item3.getName());
        assertEquals(20.0, item3.getWeightGrams(), DELTA);
        assertEquals(3.0, item3.getCarbsGrams(), DELTA);
        assertEquals(7.5, item3.getFatGrams(), DELTA);
        assertEquals(6.0, item3.getProteinGrams(), DELTA);

        assertEquals("Haferflocken liefern den Großteil der Kohlenhydrate.", result.getExplanation());
        assertEquals("Durch Ballaststoffe und Fette langsamere Resorption.", result.getInsulinTip());
    }

    @Test
    public void testParseDirectJsonPayloadWithoutCandidateWrapper() {
        String directJson = """
                {
                  "mealTitle": "Spaghetti Bolognese",
                  "totalCarbsGrams": 75.0,
                  "totalFatGrams": 18.0,
                  "totalProteinGrams": 24.0,
                  "items": [
                    {
                      "name": "Spaghetti",
                      "portion": "200g",
                      "weightGrams": 200.0,
                      "carbsGrams": 70.0,
                      "fatGrams": 2.0,
                      "proteinGrams": 8.0,
                      "calories": 310
                    },
                    {
                      "name": "Bolognese Sauce",
                      "portion": "150g",
                      "weightGrams": 150.0,
                      "carbsGrams": 5.0,
                      "fatGrams": 16.0,
                      "proteinGrams": 16.0,
                      "calories": 230
                    }
                  ],
                  "explanation": "Pasta Gericht",
                  "insulinTip": "Dual-Bolus erwägen"
                }""";

        MealEstimateResult result = service.parseGeminiApiResponse(directJson, "Spaghetti", "gemini-3.5-flash");

        assertEquals("Spaghetti Bolognese", result.getMealTitle());
        assertEquals(350.0, result.getTotalWeightGrams(), DELTA); // Calculated from sum of item weights (200 + 150)
        assertEquals(540, result.getTotalCalories()); // Calculated from sum of item calories (310 + 230)
        assertEquals(75.0, result.getTotalCarbsGrams(), DELTA);
        assertEquals(18.0, result.getTotalFatGrams(), DELTA);
        assertEquals(24.0, result.getTotalProteinGrams(), DELTA);
        assertEquals(2, result.getItems().size());
        assertEquals("Dual-Bolus erwägen", result.getInsulinTip());
    }

    @Test
    public void testParseSparseJsonWithFallbackTitleAndDefaults() {
        String sparseJson = """
                {
                  "totalCarbsGrams": 30.5
                }""";

        MealEstimateResult result = service.parseGeminiApiResponse(
                sparseJson,
                "Apfel & Nüsse",
                "gemini-2.5-flash-lite"
        );

        assertNotNull(result);
        assertEquals("Apfel & Nüsse", result.getMealTitle());
        assertEquals(0.0, result.getTotalWeightGrams(), DELTA); // Does not default to 100!
        assertEquals(0, result.getTotalCalories());
        assertEquals(30.5, result.getTotalCarbsGrams(), DELTA);
        assertEquals(0.0, result.getTotalFatGrams(), DELTA);
        assertEquals(0.0, result.getTotalProteinGrams(), DELTA);
        assertTrue(result.getItems().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmptyResponseThrowsIllegalArgumentException() {
        service.parseGeminiApiResponse("", "Test", "model");
    }
}
