package com.feri.healthydiet.ui.foodanalyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.AnalysisType
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.remote.AnthropicRequest
import com.feri.healthydiet.data.remote.AnthropicService
import com.feri.healthydiet.data.remote.Message
import com.feri.healthydiet.data.repository.HistoryRepository
import com.feri.healthydiet.data.repository.UserRepository
import com.feri.healthydiet.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class FoodAnalyzerViewModel(
    private val anthropicService: AnthropicService,
    private val historyRepository: HistoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodAnalyzerUiState())
    val uiState: StateFlow<FoodAnalyzerUiState> = _uiState.asStateFlow()

    fun analyzeFoodItem(foodName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Get user health profile
                val healthProfile = userRepository.getUserHealthProfile()

                // Create prompt for AI
                val prompt = createFoodAnalysisPrompt(foodName, healthProfile)

                // Call AI service
                val response = anthropicService.analyzeFood(
                    AnthropicRequest(
                        model = Constants.DEFAULT_MODEL,
                        messages = listOf(Message("user", prompt))
                    )
                )

                // Parse response
                val analysisResult = parseAIResponse(response.content.first().text)

                // Save to history
                saveToHistory(foodName, analysisResult)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    foodAnalysis = analysisResult
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }

    private fun createFoodAnalysisPrompt(foodName: String, profile: HealthProfile): String {
        val healthConditions = mutableListOf<String>()

        if (profile.hasDiabetes) healthConditions.add("diabetes")
        if (profile.hasLiverSteatosis) healthConditions.add("non-alcoholic fatty liver disease")
        if (profile.hasHypertension) healthConditions.add("hypertension")
        if (profile.hasHighCholesterol) healthConditions.add("high cholesterol")
        if (profile.hasCeliac) healthConditions.add("celiac disease")
        healthConditions.addAll(profile.customConditions)

        return """
            I need a nutritional analysis of '$foodName' for someone with ${healthConditions.joinToString(", ")}.
            
            Please provide:
            1. A category (Recommended, Moderate, or Avoid)
            2. Estimated nutritional composition (protein, carbs, fats)
            3. Health impacts for someone with these conditions
            
            Provide the response in JSON format with the following structure:
            {
              "category": "Recommended/Moderate/Avoid",
              "protein": "amount in grams",
              "carbs": "amount in grams",
              "fats": "amount in grams",
              "healthImpacts": ["impact1", "impact2", "impact3"]
            }
        """.trimIndent()
    }

    private fun parseAIResponse(responseText: String): FoodAnalysis {
        try {
            // Basic parsing - in a real app, handle this more robustly
            val jsonStr = responseText.substringAfter("```json").substringBefore("```")
            return Gson().fromJson(jsonStr, FoodAnalysis::class.java)
        } catch (e: Exception) {
            // Fallback parsing if JSON extraction fails
            return createFallbackResult(responseText)
        }
    }

    private fun createFallbackResult(text: String): FoodAnalysis {
        // Create a basic result when parsing fails
        return FoodAnalysis(
            category = "Unknown",
            protein = "0",
            carbs = "0",
            fats = "0",
            healthImpacts = listOf("Could not analyze this food item")
        )
    }

    private fun saveToHistory(foodName: String, analysis: FoodAnalysis) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            val historyItem = AnalysisHistory(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = AnalysisType.FOOD_ITEM,
                name = foodName,
                content = Gson().toJson(analysis),
                createdAt = System.currentTimeMillis()
            )
            historyRepository.saveAnalysisHistory(historyItem)
        }
    }
}

data class FoodAnalyzerUiState(
    val isLoading: Boolean = false,
    val foodAnalysis: FoodAnalysis? = null,
    val error: String? = null
)

data class FoodAnalysis(
    val category: String,
    val protein: String,
    val carbs: String,
    val fats: String,
    val healthImpacts: List<String>
)