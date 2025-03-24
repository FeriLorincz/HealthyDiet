package com.feri.healthydiet.ui.menuscan

import android.content.Context
import android.net.Uri
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
import com.feri.healthydiet.util.TextRecognitionHelper
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MenuScanViewModel(
    private val anthropicService: AnthropicService,
    private val textRecognitionHelper: TextRecognitionHelper,
    private val historyRepository: HistoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuScanUiState())
    val uiState: StateFlow<MenuScanUiState> = _uiState.asStateFlow()

    fun analyzeMenuImage(imageUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Extract text from image
                val menuText = textRecognitionHelper.recognizeTextFromImage(imageUri, context)

                // Get user health profile
                val healthProfile = userRepository.getUserHealthProfile()

                // Create prompt for AI
                val prompt = createMenuAnalysisPrompt(menuText, healthProfile)

                // Call AI service
                val response = anthropicService.analyzeFood(
                    AnthropicRequest(
                        model = Constants.DETAILED_MODEL,
                        messages = listOf(Message("user", prompt))
                    )
                )

                // Parse response
                val analysisResult = parseAIResponse(response.content.first().text)

                // Save to history
                saveToHistory(menuText, analysisResult)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = analysisResult
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }

    private fun createMenuAnalysisPrompt(menuText: String, profile: HealthProfile): String {
        val healthConditions = mutableListOf<String>()

        if (profile.hasDiabetes) healthConditions.add("diabetes")
        if (profile.hasLiverSteatosis) healthConditions.add("non-alcoholic fatty liver disease")
        if (profile.hasHypertension) healthConditions.add("hypertension")
        if (profile.hasHighCholesterol) healthConditions.add("high cholesterol")
        if (profile.hasCeliac) healthConditions.add("celiac disease")
        healthConditions.addAll(profile.customConditions)

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
        """.trimIndent()
    }

    private fun parseAIResponse(responseText: String): MenuAnalysisResult {
        try {
            // Basic parsing - in a real app, handle this more robustly
            val jsonStr = responseText.substringAfter("```json").substringBefore("```")
            return Gson().fromJson(jsonStr, MenuAnalysisResult::class.java)
        } catch (e: Exception) {
            // Fallback parsing if JSON extraction fails
            return createFallbackResult(responseText)
        }
    }

    private fun createFallbackResult(text: String): MenuAnalysisResult {
        // Create a basic result when parsing fails
        return MenuAnalysisResult(
            recommended = listOf(FoodRecommendation("Parsing error", "Could not parse AI response")),
            avoid = emptyList(),
            moderate = emptyList()
        )
    }

    private fun saveToHistory(menuText: String, result: MenuAnalysisResult) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            val historyItem = AnalysisHistory(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = AnalysisType.MENU,
                name = "Menu Analysis ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
                content = Gson().toJson(result),
                createdAt = System.currentTimeMillis()
            )
            historyRepository.saveAnalysisHistory(historyItem)
        }
    }
}

data class MenuScanUiState(
    val isLoading: Boolean = false,
    val analysisResult: MenuAnalysisResult? = null,
    val error: String? = null
)

data class MenuAnalysisResult(
    val recommended: List<FoodRecommendation>,
    val avoid: List<FoodRecommendation>,
    val moderate: List<FoodRecommendation>
) : Serializable

data class FoodRecommendation(
    val name: String,
    val reason: String
) : Serializable