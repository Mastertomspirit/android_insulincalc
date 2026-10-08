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

import android.content.Context
import android.util.Log
import network.spiritscorp.BuildConfig
import network.spiritscorp.R
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
    val rawThinking: String = "",
    val isOfflineEstimate: Boolean = false,
    val modelUsed: String = ""
)

class GeminiMealService(private val context: Context? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return context?.getString(resId, *formatArgs) ?: ""
    }

    suspend fun estimateCarbsFromDescription(
        foodDescription: String,
        customApiKey: String? = null,
        selectedModel: GeminiAiModel
    ): Result<MealEstimateResult> = withContext(Dispatchers.IO) {
        val devKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        // Prioritize custom user key over BuildConfig dev key
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            devKey.isNotBlank() && devKey != "MY_GEMINI_API_KEY" -> devKey.trim()
            else -> ""
        }

        if (apiKey.isBlank()) {
            // Provide offline intelligent fallback estimation if no API key is provided
            val offline = createOfflineEstimation(foodDescription)
            return@withContext if (offline != null) {
                Result.success(offline)
            } else {
                Result.failure(Exception(getString(R.string.gemini_meal_service_offline_not_found, foodDescription)))
            }
        }

        // Always resolve to a valid Gemini model ID
        val effectiveModel = selectedModel.modelId

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent"

            val prompt = getString(R.string.gemini_meal_service_prompt_template, foodDescription).trimIndent()

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
                .header("x-goog-api-key", apiKey )
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body.string()

            if (!response.isSuccessful) {
                Log.e("GeminiMealService", "API call failed with code ${response.code}: $responseBody")
                val errorMessage = try {
                    val errorObj = JSONObject(responseBody).optJSONObject("error")
                    errorObj?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception(getString(R.string.gemini_meal_service_api_failed, response.code, errorMessage)))
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

            if (jsonText.isBlank()) {
                return@withContext Result.failure(Exception(getString(R.string.gemini_meal_service_no_response)))
            }

            // Clean json text if wrapped in Markdown
            val cleaned = jsonText.replace("```json", "").replace("```", "").trim()
            val parsedResult = JSONObject(cleaned)

            val mealTitle = parsedResult.optString("mealTitle", foodDescription.take(30))
            val totalCarbs = parsedResult.optDouble("totalCarbsGrams", 0.0)
            val explanation = parsedResult.optString("explanation", getString(R.string.gemini_meal_service_default_explanation))
            val insulinTip = parsedResult.optString("insulinTip", getString(R.string.gemini_meal_service_default_insulin_tip))

            val itemsList = mutableListOf<MealItemDetail>()
            val itemsJsonArray = parsedResult.optJSONArray("items")

            if (itemsJsonArray != null) {
                for (i in 0 until itemsJsonArray.length()) {
                    val itemObj = itemsJsonArray.optJSONObject(i)
                    if (itemObj != null) {
                        itemsList.add(
                            MealItemDetail(
                                name = itemObj.optString("name", getString(R.string.gemini_meal_service_default_item_name)),
                                portion = itemObj.optString("portion", getString(R.string.gemini_meal_service_default_item_portion)),
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
                    insulinTip = insulinTip,
                    isOfflineEstimate = false,
                    modelUsed = effectiveModel
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiMealService", "Error during Gemini estimation", e)
            Result.failure(Exception(getString(R.string.gemini_meal_service_network_error, e.localizedMessage)))
        }
    }

    private fun createOfflineEstimation(foodDescription: String): MealEstimateResult? {
        val matches = StandardFoodDatabase.findMatches(foodDescription)
        if (matches.isEmpty()) {
            return null
        }
        val items = mutableListOf<MealItemDetail>()
        var total = 0.0

        for (food in matches) {
            items.add(
                MealItemDetail(
                    name = food.germanName,
                    portion = food.standardPortionText,
                    carbsGrams = food.carbsPerPortion,
                    calories = food.caloriesPerPortion,
                    notes = food.glycemicIndexNote
                )
            )
            total += food.carbsPerPortion
        }

        return MealEstimateResult(
            mealTitle = foodDescription.take(35),
            totalCarbsGrams = total,
            items = items,
            explanation = getString(R.string.gemini_meal_service_offline_explanation, items.size),
            insulinTip = getString(R.string.gemini_meal_service_default_insulin_tip),
            isOfflineEstimate = true,
            modelUsed = getString(R.string.gemini_meal_service_offline_model_name)
        )
    }
}
