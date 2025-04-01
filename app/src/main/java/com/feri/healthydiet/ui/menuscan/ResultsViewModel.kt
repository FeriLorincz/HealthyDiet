package com.feri.healthydiet.ui.menuscan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.AnalysisType
import com.feri.healthydiet.data.repository.HistoryRepository
import com.feri.healthydiet.data.repository.UserRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ResultsViewModel(
    private val historyRepository: HistoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private var currentResult: MenuAnalysisResult? = null

    fun setResult(result: MenuAnalysisResult) {
        currentResult = result
    }

    fun saveToHistory() {
        viewModelScope.launch {
            try {
                val result = currentResult ?: return@launch

                Log.d("ResultsViewModel", "Saving analysis to history")
                // Forțează crearea utilizatorului înainte de a salva în istoric
                val user = userRepository.getCurrentUser()
                val userId = user.id

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

                historyRepository.saveAnalysisHistory(historyItem)
                Log.d("ResultsViewModel", "Analysis saved to history successfully")
            } catch (e: Exception) {
                Log.e("ResultsViewModel", "Error saving to history: ${e.message}", e)
            }
        }
    }
}