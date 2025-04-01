package com.feri.healthydiet.ui.menuscan

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.AnalysisType
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.remote.AnthropicRequest
import com.feri.healthydiet.data.remote.AnthropicService
import com.feri.healthydiet.data.remote.Message
import com.feri.healthydiet.data.repository.AnalysisRepository
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

    private val TAG = "MenuScanViewModel"
    private val _uiState = MutableStateFlow(MenuScanUiState())
    val uiState: StateFlow<MenuScanUiState> = _uiState.asStateFlow()

    fun analyzeMenuImage(imageUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d(TAG, "Starting text recognition from image")

                // Extract text from image
                val menuText = try {
                    textRecognitionHelper.recognizeTextFromImage(imageUri, context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error recognizing text: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Text recognition failed: ${e.message}"
                    )
                    return@launch
                }

                if (menuText.isBlank()) {
                    Log.w(TAG, "No text detected in the image")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No text detected in the image. Please try another photo with clearer text."
                    )
                    return@launch
                }

                Log.d(TAG, "Text recognition successful, getting health profile")

                // Get user health profile
                val healthProfile = userRepository.getUserHealthProfile()

                Log.d(TAG, "Creating AI analysis prompt")

                // Create prompt for AI
                val prompt = createMenuAnalysisPrompt(menuText, healthProfile)

                Log.d(TAG, "Calling AI service")

                // Call AI service
                val response = try {
                    anthropicService.analyzeFood(
                        AnthropicRequest(
                            model = Constants.DETAILED_MODEL,
                            messages = listOf(Message("user", prompt))
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error calling AI service: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "AI analysis failed: ${e.message}"
                    )
                    return@launch
                }

                Log.d(TAG, "AI response received, parsing result")

                // Parse response
                val analysisResult = try {
                    parseAIResponse(response.content.first().text)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing AI response: ${e.message}", e)
                    createFallbackResult("Failed to analyze menu: ${e.message}")
                }

                Log.d(TAG, "Saving analysis to history")

                // Save to history
                try {
                    saveToHistory(menuText, analysisResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving to history: ${e.message}", e)
                    // Continue even if history saving fails
                }

                Log.d(TAG, "Analysis complete, updating UI")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = analysisResult
                )

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in menu analysis: ${e.message}", e)
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

        val conditionsText = if (healthConditions.isEmpty()) "no specific health conditions"
        else healthConditions.joinToString(", ")

        return """
            I need help analyzing a restaurant menu for someone with $conditionsText.
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
            // Log the full response for debugging
            Log.d(TAG, "Raw AI response received: ${responseText.take(200)}...")

            // Parse JSON from AI response
            // Look for JSON content within the response
            val jsonPattern = """\{[\s\S]*\}""".toRegex()
            val jsonMatch = jsonPattern.find(responseText)

            if (jsonMatch != null) {
                val jsonStr = jsonMatch.value
                Log.d(TAG, "Found JSON in response: ${jsonStr.take(100)}...")

                return try {
                    val result = Gson().fromJson(jsonStr, MenuAnalysisResult::class.java)
                    // Verifică dacă rezultatul are date valide
                    if (result.recommended.isEmpty() && result.avoid.isEmpty() && result.moderate.isEmpty()) {
                        Log.w(TAG, "JSON parsed but all lists are empty")
                        createFallbackResult("AI returned empty analysis results")
                    } else {
                        result
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "JSON parsing error: ${e.message}", e)
                    createFallbackResult("Error parsing AI response: ${e.message}")
                }
            } else {
                // Încearcă să extragă un alt format de date structurate
                Log.e(TAG, "No JSON found in response, trying alternative format")

                // Exemplu de format alternativ fallback - poți personaliza conform nevoilor
                return MenuAnalysisResult(
                    recommended = listOf(
                        FoodRecommendation(
                            "Could not extract structured data",
                            "The AI response wasn't in the expected format. Try taking a clearer photo of the menu."
                        )
                    ),
                    avoid = listOf(
                        FoodRecommendation(
                            "Technical details",
                            "No JSON pattern found in response text that was ${responseText.length} characters long."
                        )
                    ),
                    moderate = emptyList(),
                    savedToHistory = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseAIResponse: ${e.message}", e)
            return createFallbackResult("Error processing AI response: ${e.message}")
        }
    }

    private fun createFallbackResult(errorMessage: String): MenuAnalysisResult {
        return MenuAnalysisResult(
            recommended = listOf(FoodRecommendation("Error processing menu", errorMessage)),
            avoid = emptyList(),
            moderate = emptyList()
        )
    }

    private fun saveToHistory(menuText: String, result: MenuAnalysisResult) {
        viewModelScope.launch {
            try {
                // Forțează crearea utilizatorului înainte de a salva în istoric
                //val userId = userRepository.ensureUserExists()

                // Încearcă să asiguri existența utilizatorului și folosește ID-ul rezultat
                val userId = try {
                    userRepository.ensureUserExists()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to ensure user exists: ${e.message}", e)
                    // Folosește un ID temporar dacă apare o eroare
                    UUID.randomUUID().toString()
                }

                val timestamp = System.currentTimeMillis()
                val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(timestamp))

                val historyItem = AnalysisHistory(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = AnalysisType.MENU,
                    name = "Menu Analysis $dateFormatted",
                    content = Gson().toJson(result),
                    createdAt = timestamp
                )

                try {
                    historyRepository.saveAnalysisHistory(historyItem)
                    Log.d(TAG, "Analysis saved to history successfully")
                    // Setăm flag-ul care indică că analiza a fost salvată
                    result.savedToHistory = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving analysis history: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveToHistory: ${e.message}", e)
            }
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
    val moderate: List<FoodRecommendation>,
    var savedToHistory: Boolean = false
) : Serializable

data class FoodRecommendation(
    val name: String,
    val reason: String
) : Serializable