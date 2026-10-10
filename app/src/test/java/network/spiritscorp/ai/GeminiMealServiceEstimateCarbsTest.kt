package network.spiritscorp.ai

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiMealServiceEstimateCarbsTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: GeminiMealService

    private val testApiKey = "test-api-key-123"
    private val testFoodDescription = "A plate of spaghetti bolognese"
    private val testModel = GeminiAiModel.GEMINI_3_8_FLASH

    private val expectedMealTitle = "Test Meal"
    private val expectedTotalCarbs = 45.0
    private val expectedTotalFat = 15.0
    private val expectedTotalProtein = 20.0
    private val expectedErrorMessage = "Invalid request"
    private val expectedHttpErrorCode = 400

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        service = GeminiMealService(
            context = ApplicationProvider.getApplicationContext(),
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        )
    }

    @After
    fun tearDown() {
        mockWebServer.close()
    }

    // --- Request verification -------------------------------------------

    @Test
    fun estimateCarbsFromDescription_usesPostMethod() = runTest {
        val request = performRequestAndCapture()
        assertEquals("POST", request.method)
    }

    @Test
    fun estimateCarbsFromDescription_sendsApiKeyInHeader() = runTest {
        val request = performRequestAndCapture()
        assertEquals(testApiKey, request.headers["x-goog-api-key"])
    }

    @Test
    fun estimateCarbsFromDescription_sendsCorrectRequestPath() = runTest {
        val request = performRequestAndCapture()
        assertEquals("/v1/models/${testModel.modelId}:generateContent", request.target)
    }

    @Test
    fun estimateCarbsFromDescription_sendsFoodDescriptionInPrompt() = runTest {
        val request = performRequestAndCapture()
        assertTrue(extractSentPrompt(request).contains(testFoodDescription))
    }

    @Test
    fun estimateCarbsFromDescription_requestsJsonResponseFormat() = runTest {
        val request = performRequestAndCapture()
        val requestBody = JSONObject(request.body?.utf8().orEmpty())
        assertEquals(
            "application/json",
            requestBody.getJSONObject("generationConfig").getString("responseMimeType")
        )
    }

    // --- Result verification: real parsed output, not just success/failure ---

    @Test
    fun estimateCarbsFromDescription_returnsCorrectMealTitleOnSuccess() = runTest {
        mockWebServer.enqueue(MockResponse.Builder().code(200).body(validGeminiResponseJson()).build())

        val result = service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)

        assertEquals(expectedMealTitle, result.getOrNull()?.mealTitle)
    }

    @Test
    fun estimateCarbsFromDescription_returnsCorrectCarbsOnSuccess() = runTest {
        mockWebServer.enqueue(MockResponse.Builder().code(200).body(validGeminiResponseJson()).build())

        val result = service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)

        assertEquals(expectedTotalCarbs, result.getOrNull()?.totalCarbsGrams)
    }

    @Test
    fun estimateCarbsFromDescription_returnsUsedModelOnSuccess() = runTest {
        mockWebServer.enqueue(MockResponse.Builder().code(200).body(validGeminiResponseJson()).build())

        val result = service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)

        assertEquals(testModel.modelId, result.getOrNull()?.modelUsed)
    }

    @Test
    fun estimateCarbsFromDescription_marksResultAsNotOffline() = runTest {
        mockWebServer.enqueue(MockResponse.Builder().code(200).body(validGeminiResponseJson()).build())

        val result = service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)

        assertFalse(result.getOrNull()?.isOfflineEstimate ?: true)
    }

    @Test
    fun estimateCarbsFromDescription_failureMessageContainsHttpErrorDetails() = runTest {
        mockWebServer.enqueue(
            MockResponse.Builder()
                .code(expectedHttpErrorCode)
                .body("""{"error": {"message": "$expectedErrorMessage"}}""")
                .build()
        )

        val result = service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        // Verifies the real error-parsing logic actually extracted these values,
        // not just that *something* failed.
        assertTrue(message.contains(expectedHttpErrorCode.toString()))
        assertTrue(message.contains(expectedErrorMessage))
    }

    @Test
    fun estimateCarbsFromDescription_returnsFailureOnNetworkError() = runTest {
        mockWebServer.close() // server now unreachable -> real connection failure, no SocketPolicy needed

        val result = service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.isNotBlank() == true)
    }

    // --- Helpers -----------------------------------------------------------

    private suspend fun performRequestAndCapture(): RecordedRequest {
        mockWebServer.enqueue(MockResponse.Builder().code(200).body(validGeminiResponseJson()).build())
        service.estimateCarbsFromDescription(testFoodDescription, testApiKey, testModel)
        return mockWebServer.takeRequest(5, TimeUnit.SECONDS)
            ?: throw AssertionError("No request received within timeout")
    }

    private fun extractSentPrompt(request: RecordedRequest): String {
        val requestBody = JSONObject(request.body?.utf8().orEmpty())
        return requestBody
            .getJSONArray("contents").getJSONObject(0)
            .getJSONArray("parts").getJSONObject(0)
            .getString("text")
    }

    private fun validGeminiResponseJson(): String = """
        {
          "candidates": [{
            "content": {
              "parts": [{
                "text": "{\"mealTitle\":\"$expectedMealTitle\",\"totalCarbsGrams\":$expectedTotalCarbs,\"totalFatGrams\":$expectedTotalFat,\"totalProteinGrams\":$expectedTotalProtein,\"items\":[],\"explanation\":\"x\",\"insulinTip\":\"y\"}"
              }]
            }
          }]
        }
    """.trimIndent()
}