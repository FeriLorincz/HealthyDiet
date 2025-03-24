package com.feri.healthydiet.data.remote

import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.ui.menuscan.FoodRecommendation
import com.feri.healthydiet.ui.menuscan.MenuAnalysisResult
import com.feri.healthydiet.util.Constants
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AnthropicService {

    @Headers(
        "Content-Type: application/json",
        "x-api-key: ${Constants.ANTHROPIC_API_KEY}",
        "anthropic-version: 2023-06-01"
    )
    @POST("v1/messages")
    suspend fun analyzeFood(@Body request: AnthropicRequest): AnthropicResponse
}

class AnthropicApiClient {
    companion object {
        fun create(): AnthropicService {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(Constants.ANTHROPIC_API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AnthropicService::class.java)
        }
    }
}

class AnthropicAnalyzer(private val service: AnthropicService) {
    private val gson = Gson()

    suspend fun analyzeFoodItem(foodName: String, healthProfile: HealthProfile): FoodAnalysisResult {
        val prompt = createFoodItemPrompt(foodName, healthProfile)

        val request = AnthropicRequest(
            model = Constants.DEFAULT_MODEL,
            messages = listOf(Message("user", prompt)),
            max_tokens = 1000
        )

        val response = service.analyzeFood(request)
        return parseFoodAnalysisResponse(response)
    }

    suspend fun analyzeMenu(menuText: String, healthProfile: HealthProfile): MenuAnalysisResult {
        val prompt = createMenuAnalysisPrompt(menuText, healthProfile)

        val request = AnthropicRequest(
            model = Constants.DETAILED_MODEL,
            messages = listOf(Message("user", prompt)),
            max_tokens = 2000
        )

        val response = service.analyzeFood(request)
        return parseMenuAnalysisResponse(response)
    }

    private fun createFoodItemPrompt(foodName: String, profile: HealthProfile): String {
        val healthConditions = buildHealthConditionsList(profile)

        return """
            I need a nutritional analysis of '$foodName' for someone with ${healthConditions.joinToString(", ")}.
            
            Please provide:
            1. A category (Recommended, Moderate, or Avoid)
            2. Estimated nutritional composition (protein, carbs, fats in grams)
            3. Health impacts for someone with these conditions
            
            Provide the response in JSON format with the following structure:
            {
              "category": "Recommended/Moderate/Avoid",
              "protein": "20",
              "carbs": "30",
              "fats": "10",
              "healthImpacts": ["impact1", "impact2", "impact3"]
            }
        """.trimIndent()
    }

    private fun createMenuAnalysisPrompt(menuText: String, profile: HealthProfile): String {
        val healthConditions = buildHealthConditionsList(profile)

        return """
            I need help analyzing a restaurant menu for someone with ${healthConditions.joinToString(", ")}. 
            Here is the menu text:
            
            $menuText
            
            Please analyze this menu and provide:
            1. Top 3 recommended dishes that are healthiest for someone with these conditions
            2. Top 3 dishes to avoid completely
            3. Dishes that can be consumed in moderation
            
            For each dish, briefly explain why it's recommended, should be avoided, or can be consumed in moderation.
            Provide the response in JSON format with three arrays: "recommended", "avoid", and "moderate".
            Each item should have "name" and "reason" fields.
            
            Example format:
            {
              "recommended": [
                {"name": "Grilled Salmon", "reason": "High in omega-3, low in saturated fat"}
              ],
              "avoid": [
                {"name": "Fried Chicken", "reason": "High in unhealthy fats"}
              ],
              "moderate": [
                {"name": "Pasta", "reason": "Moderate carb content"}
              ]
            }
        """.trimIndent()
    }

    private fun buildHealthConditionsList(profile: HealthProfile): List<String> {
        val healthConditions = mutableListOf<String>()

        if (profile.hasDiabetes) healthConditions.add("diabetes")
        if (profile.hasLiverSteatosis) healthConditions.add("non-alcoholic fatty liver disease")
        if (profile.hasHypertension) healthConditions.add("hypertension")
        if (profile.hasHighCholesterol) healthConditions.add("high cholesterol")
        if (profile.hasCeliac) healthConditions.add("celiac disease")
        healthConditions.addAll(profile.customConditions)

        return healthConditions
    }

    private fun parseFoodAnalysisResponse(response: AnthropicResponse): FoodAnalysisResult {
        try {
            val responseText = response.content.first().text
            val jsonStr = extractJsonFromText(responseText)
            return gson.fromJson(jsonStr, FoodAnalysisResult::class.java)
        } catch (e: Exception) {
            return FoodAnalysisResult(
                category = "Unknown",
                protein = "0",
                carbs = "0",
                fats = "0",
                healthImpacts = listOf("Could not analyze this food item")
            )
        }
    }

    private fun parseMenuAnalysisResponse(response: AnthropicResponse): MenuAnalysisResult {
        try {
            val responseText = response.content.first().text
            val jsonStr = extractJsonFromText(responseText)
            return gson.fromJson(jsonStr, MenuAnalysisResult::class.java)
        } catch (e: Exception) {
            return MenuAnalysisResult(
                recommended = listOf(FoodRecommendation("Parsing error", "Could not parse AI response")),
                avoid = emptyList(),
                moderate = emptyList()
            )
        }
    }

    private fun extractJsonFromText(text: String): String {
        val jsonPattern = """\{[\s\S]*\}""".toRegex()
        val jsonMatch = jsonPattern.find(text)
        return jsonMatch?.value ?: throw Exception("No JSON found in response")
    }
}

data class FoodAnalysisResult(
    val category: String,
    val protein: String,
    val carbs: String,
    val fats: String,
    val healthImpacts: List<String>
)

data class AnthropicRequest(
    val model: String = Constants.DEFAULT_MODEL,
    val messages: List<Message>,
    val max_tokens: Int = 1000
)

data class Message(
    val role: String, // "user" sau "assistant"
    val content: String
)

data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String
)

data class ContentBlock(
    val type: String, // "text"
    val text: String
)