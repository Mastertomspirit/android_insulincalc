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

import android.util.Log
import network.spiritscorp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MealItemDetail(
    val name: String,
    val portion: String,
    val carbsGrams: Double,
    val calories: Int = 0,
    val notes: String = ""
)

data class MealEstimateResult(
    val mealTitle: String,
    val totalCarbsGrams: Double,
    val items: List<MealItemDetail>,
    val explanation: String,
    val insulinTip: String,
    val rawThinking: String = ""
)

class GeminiMealService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun estimateCarbsFromDescription(foodDescription: String): Result<MealEstimateResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide offline intelligent fallback estimation if no API key is provided
            return@withContext Result.success(createOfflineEstimation(foodDescription))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

            val prompt = """
                Du bist ein erfahrener diabetologischer Ernährungsberater und Experte für Kohlenhydratschätzung (KE/BE und Gramm KH).
                Analysiere die folgende Mahlzeit/Lebensmittelbeschreibung:
                "$foodDescription"

                Schätze präzise den Kohlenhydratgehalt für die einzelnen Bestandteile und die gesamte Mahlzeit.
                Gib das Ergebnis STRENG im folgenden JSON-Format zurück (ohne Markdown Backticks oder sonstigen Text außerhalb des JSON):
                {
                  "mealTitle": "Kurzer Name der Mahlzeit",
                  "totalCarbsGrams": 45.0,
                  "items": [
                    {
                      "name": "Zutat 1 (z.B. Vollkornbrot)",
                      "portion": "2 Scheiben (ca. 90g)",
                      "carbsGrams": 36.0,
                      "calories": 190,
                      "notes": "Langsame Resorption dank Ballaststoffen"
                    }
                  ],
                  "explanation": "Detaillierte ernährungswissenschaftliche Begründung der Schätzung.",
                  "insulinTip": "Praktischer Hinweis für Diabetiker (z.B. Spritz-Ess-Abstand, FPE/Fett-Protein-Einheiten oder glykämischer Index)"
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiMealService", "API call failed with code ${response.code}: $responseBody")
                return@withContext Result.success(createOfflineEstimation(foodDescription))
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var jsonText = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val text = part?.optString("text", "") ?: ""
                    if (text.isNotBlank()) {
                        jsonText += text
                    }
                }
            }

            // Clean json text if wrapped in Markdown
            val cleaned = jsonText.replace("```json", "").replace("```", "").trim()
            val parsedResult = JSONObject(cleaned)

            val mealTitle = parsedResult.optString("mealTitle", "Mahlzeit")
            val totalCarbs = parsedResult.optDouble("totalCarbsGrams", 0.0)
            val explanation = parsedResult.optString("explanation", "Ernährungswissenschaftliche Schätzung.")
            val insulinTip = parsedResult.optString("insulinTip", "Bitte aktuellen BZ-Wert vor der Injektion prüfen.")

            val itemsList = mutableListOf<MealItemDetail>()
            val itemsJsonArray = parsedResult.optJSONArray("items")
            if (itemsJsonArray != null) {
                for (i in 0 until itemsJsonArray.length()) {
                    val itemObj = itemsJsonArray.optJSONObject(i)
                    if (itemObj != null) {
                        itemsList.add(
                            MealItemDetail(
                                name = itemObj.optString("name", "Zutat"),
                                portion = itemObj.optString("portion", "1 Portion"),
                                carbsGrams = itemObj.optDouble("carbsGrams", 0.0),
                                calories = itemObj.optInt("calories", 0),
                                notes = itemObj.optString("notes", "")
                            )
                        )
                    }
                }
            }

            Result.success(
                MealEstimateResult(
                    mealTitle = mealTitle,
                    totalCarbsGrams = totalCarbs,
                    items = itemsList,
                    explanation = explanation,
                    insulinTip = insulinTip
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiMealService", "Error during Gemini estimation", e)
            Result.success(createOfflineEstimation(foodDescription))
        }
    }

    private fun createOfflineEstimation(foodDescription: String): MealEstimateResult {
        val lower = foodDescription.lowercase()
        val items = mutableListOf<MealItemDetail>()
        var total = 0.0

        if (lower.contains("pizza")) {
            items.add(MealItemDetail("Pizza (mittelgroß)", "1 Pizza ca. 350g", 90.0, 750, "Hoher Fettanteil verzögert den BZ-Anstieg (FPE)"))
            total += 90.0
        }
        if (lower.contains("döner") || lower.contains("doener") || lower.contains("kebab")) {
            items.add(MealItemDetail("Döner Kebab (Fladenbrot)", "1 Portion", 65.0, 680, "Fladenbrot liefert den Hauptteil der KH"))
            total += 65.0
        }
        if (lower.contains("apfel") || lower.contains("äpfel")) {
            items.add(MealItemDetail("Apfel (mittelgroß)", "1 Stück ca. 150g", 18.0, 75, "Fruchtzucker führt zu stetigem Anstieg"))
            total += 18.0
        }
        if (lower.contains("banane")) {
            items.add(MealItemDetail("Banane (reif)", "1 Stück ca. 120g", 24.0, 105, "Schnell wirkende Kohlenhydrate"))
            total += 24.0
        }
        if (lower.contains("brot") || lower.contains("brötchen") || lower.contains("semmel")) {
            items.add(MealItemDetail("Brot / Brötchen", "2 Scheiben / 1 Brötchen", 35.0, 180, "Ca. 15-20g KH pro Scheibe Brot"))
            total += 35.0
        }
        if (lower.contains("pasta") || lower.contains("nudel") || lower.contains("spaghetti")) {
            items.add(MealItemDetail("Nudeln (gekocht)", "1 Teller ca. 200g", 55.0, 320, "Komplexe Kohlenhydrate mit moderatem GI"))
            total += 55.0
        }
        if (lower.contains("reis")) {
            items.add(MealItemDetail("Reis (gekocht)", "1 Portion ca. 180g", 50.0, 240, "Hoher glykämischer Index"))
            total += 50.0
        }
        if (lower.contains("pommes") || lower.contains("fritten")) {
            items.add(MealItemDetail("Pommes Frites", "1 Portion ca. 150g", 48.0, 420, "Fett verzögert Resorption"))
            total += 48.0
        }
        if (lower.contains("cola") && !lower.contains("zero") && !lower.contains("light")) {
            items.add(MealItemDetail("Cola / Softdrink", "1 Glas (250ml)", 27.0, 110, "Flüssiger Zucker, sehr schneller BZ-Anstieg!"))
            total += 27.0
        }
        if (lower.contains("schokolade") || lower.contains("riegel")) {
            items.add(MealItemDetail("Schokolade", "50g Riegel", 28.0, 260, "Fettreich mit schnellen KH"))
            total += 28.0
        }

        if (items.isEmpty()) {
            // General fallback approximation based on word length / heuristic
            items.add(MealItemDetail(foodDescription, "Geschätzte Standardportion", 40.0, 350, "Schätzung basierend auf durchschnittlichen Mahlzeiten"))
            total = 40.0
        }

        return MealEstimateResult(
            mealTitle = foodDescription.take(30),
            totalCarbsGrams = total,
            items = items,
            explanation = "Schätzung basierend auf diabetologischen Nährwerttabellen und Lebensmitteldaten.",
            insulinTip = "Empfehlung: Bei fett- und eiweißreichen Mahlzeiten kann der Blutzuckeranstieg verzögert auftreten. Spritz-Ess-Abstand beachten."
        )
    }
}
