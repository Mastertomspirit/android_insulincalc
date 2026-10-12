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

/**
 * Represents detailed nutritional information for a single food item parsed from a meal estimation.
 *
 * @property name The name of the food item (e.g., "Mischbrot").
 * @property portion The description of the portion size (e.g., "1 Scheibe (ca. 50g)").
 * @property carbsGrams The total carbohydrate content in grams.
 * @property fatGrams The total fat content in grams.
 * @property proteinGrams The total protein content in grams.
 * @property calories The estimated energy content in kilocalories (kcal). Defaults to 0.
 * @property notes Additional dietary notes, glycemic index remarks, or observations. Defaults to empty string.
 */
data class MealItemDetail(
    val name: String,
    val portion: String,
    val carbsGrams: Double,
    val fatGrams: Double,
    val proteinGrams: Double,
    val calories: Int = 0,
    val notes: String = ""
)

/**
 * Represents the comprehensive aggregated result of a meal estimation, either from Gemini AI or offline fallback.
 *
 * @property mealTitle A concise descriptive title for the estimated meal.
 * @property totalCarbsGrams The sum of carbohydrates across all meal items in grams.
 * @property totalFatGrams The sum of fats across all meal items in grams.
 * @property totalProteinGrams The sum of proteins across all meal items in grams.
 * @property items A list of individual [MealItemDetail] components comprising the meal.
 * @property explanation A dietary explanation or nutritional breakdown of the meal.
 * @property insulinTip An advisory tip regarding insulin dosing considerations (e.g., fat-protein unit handling).
 * @property rawThinking Optional internal reasoning or chain-of-thought text from the AI model. Defaults to empty string.
 * @property isOfflineEstimate Flag indicating whether this estimate was generated offline using [StandardFoodDatabase]. Defaults to `false`.
 * @property modelUsed The identifier of the AI model used to generate the estimate, or offline source name. Defaults to empty string.
 */
data class MealEstimateResult(
    val mealTitle: String,
    val totalCarbsGrams: Double,
    val totalFatGrams: Double,
    val totalProteinGrams: Double,
    val items: List<MealItemDetail>,
    val explanation: String,
    val insulinTip: String,
    val rawThinking: String = "",
    val isOfflineEstimate: Boolean = false,
    val modelUsed: String = ""
)

/**
 * Service responsible for estimating meal composition and carbohydrate counts using the Google Gemini AI REST API
 * or falling back to the local [StandardFoodDatabase] when API keys are unavailable.
 *
 * @property context Optional Android context used for resolving localized strings.
 * @property baseUrl The base URL endpoint for Gemini API requests. Defaults to `"https://generativelanguage.googleapis.com"`.
 */
class GeminiMealService(
    private val context: Context? = null,
    private val baseUrl: String = "https://generativelanguage.googleapis.com"
) {

    /**
     * OkHttpClient configured with generous timeouts for AI inference calls.
     */
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Safely retrieves a localized string resource using the provided context, or returns an empty string if context is null.
     *
     * @param resId The string resource identifier.
     * @param formatArgs Format arguments for the string template.
     * @return The formatted localized string, or empty string if context is unavailable.
     */
    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return context?.getString(resId, *formatArgs) ?: ""
    }

    /**
     * Estimates carbohydrates, macronutrients, and insulin tips for a given natural language food description.
     *
     * Prioritizes user-provided custom API keys, then build-config API keys. If no valid API key is available,
     * attempts to generate an intelligent offline estimate via [StandardFoodDatabase].
     *
     * @param foodDescription Natural language description of the meal or food items.
     * @param customApiKey Optional user-supplied custom Gemini API key.
     * @param selectedModel The [GeminiAiModel] to use for remote inference.
     * @return A [Result] containing [MealEstimateResult] on success, or an exception on failure.
     */
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
        @Suppress("KotlinConstantConditions")
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
            val url = "$baseUrl/v1/models/$effectiveModel:generateContent"

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

            val parsedEstimate = parseGeminiApiResponse(
                responseBody = responseBody,
                fallbackTitle = foodDescription.take(30),
                modelUsed = effectiveModel
            )

            Result.success(parsedEstimate)
        } catch (e: Exception) {
            Log.e("GeminiMealService", "Error during Gemini estimation", e)
            Result.failure(Exception(getString(R.string.gemini_meal_service_network_error, e.localizedMessage)))
        }
    }

    /**
     * Parses the raw JSON response returned by the Gemini AI API into a structured [MealEstimateResult].
     *
     * Extracts candidates, content parts, markdown code fences, and JSON properties including meal title,
     * total macronutrients, item breakdown array, explanations, and insulin tips.
     *
     * @param responseBody The raw string response body from the API call.
     * @param fallbackTitle Fallback title to use if the response omits a meal title.
     * @param modelUsed The identifier of the AI model that generated the response.
     * @return The parsed [MealEstimateResult].
     * @throws IllegalArgumentException if the response text is blank or unparseable.
     */
    fun parseGeminiApiResponse(
        responseBody: String,
        fallbackTitle: String = "",
        modelUsed: String = ""
    ): MealEstimateResult {
        var jsonText = ""
        try {
            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val text = part?.optString("text", "") ?: ""
                    if (text.isNotBlank()) {
                        jsonText += text
                    }
                }
            } else {
                jsonText = responseBody
            }
        } catch (_: Exception) {
            jsonText = responseBody
        }

        if (jsonText.isBlank()) {
            throw IllegalArgumentException(getString(R.string.gemini_meal_service_no_response))
        }

        val cleaned = jsonText.replace("```json", "").replace("```", "").trim()
        val parsedResult = JSONObject(cleaned)

        val mealTitle = parsedResult.optString("mealTitle", fallbackTitle.take(30)).trim()
        val totalCarbs = parsedResult.optDouble("totalCarbsGrams", 0.0)
        val totalFat = parsedResult.optDouble("totalFatGrams", 0.0)
        val totalProtein = parsedResult.optDouble("totalProteinGrams", 0.0)
        val explanation = parsedResult.optString("explanation", getString(R.string.gemini_meal_service_default_explanation)).trim()
        val insulinTip = parsedResult.optString("insulinTip", getString(R.string.gemini_meal_service_default_insulin_tip)).trim()

        val itemsList = mutableListOf<MealItemDetail>()
        val itemsJsonArray = parsedResult.optJSONArray("items")

        if (itemsJsonArray != null) {
            for (i in 0 until itemsJsonArray.length()) {
                val itemObj = itemsJsonArray.optJSONObject(i)
                if (itemObj != null) {
                    val fat = itemObj.optDouble("fatGrams", 0.0)
                    val protein = itemObj.optDouble("proteinGrams", 0.0)

                    itemsList.add(
                        MealItemDetail(
                            name = itemObj.optString("name", getString(R.string.gemini_meal_service_default_item_name)).trim(),
                            portion = itemObj.optString("portion", getString(R.string.gemini_meal_service_default_item_portion)).trim(),
                            carbsGrams = itemObj.optDouble("carbsGrams", 0.0),
                            fatGrams = fat,
                            proteinGrams = protein,
                            calories = itemObj.optInt("calories", 0),
                            notes = itemObj.optString("notes", "").trim()
                        )
                    )
                }
            }
        }

        return MealEstimateResult(
            mealTitle = mealTitle,
            totalCarbsGrams = totalCarbs,
            totalFatGrams = totalFat,
            totalProteinGrams = totalProtein,
            items = itemsList,
            explanation = explanation,
            insulinTip = insulinTip,
            isOfflineEstimate = false,
            modelUsed = modelUsed
        )
    }

    /**
     * Generates an offline fallback estimate by matching food description keywords against the [StandardFoodDatabase].
     *
     * @param foodDescription Natural language food description to match.
     * @return A [MealEstimateResult] representing the offline estimation, or `null` if no matching food items were found.
     */
    private fun createOfflineEstimation(foodDescription: String): MealEstimateResult? {
        val matches = StandardFoodDatabase.findMatches(foodDescription)
        if (matches.isEmpty()) {
            return null
        }
        val items = mutableListOf<MealItemDetail>()
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalProtein = 0.0

        // TODO create fat and protein in offline db
        for (food in matches) {
            items.add(
                MealItemDetail(
                    name = food.germanName,
                    portion = food.standardPortionText,
                    carbsGrams = food.carbsPerPortion,
                    calories = food.caloriesPerPortion,
                    notes = food.glycemicIndexNote,
                    fatGrams = 0.0,
                    proteinGrams = 0.0,

                    )
            )
            totalCarbs += food.carbsPerPortion
            totalFat += 0.0
            totalProtein += 0.0
        }

        return MealEstimateResult(
            mealTitle = foodDescription.take(35).trim(),
            totalCarbsGrams = totalCarbs,
            totalFatGrams = totalFat,
            totalProteinGrams = totalProtein,
            items = items,
            // TODO check if this is correct
            explanation = context?.resources?.getQuantityString(R.plurals.gemini_meal_service_offline_explanation, items.size, items.size) ?: "",
            insulinTip = getString(R.string.gemini_meal_service_default_insulin_tip).trim(),
            isOfflineEstimate = true,
            modelUsed = getString(R.string.gemini_meal_service_offline_model_name)
        )
    }
}
