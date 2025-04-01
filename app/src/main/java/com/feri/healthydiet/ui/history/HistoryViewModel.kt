package com.feri.healthydiet.ui.history

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.repository.HistoryRepository
import kotlinx.coroutines.launch
import android.util.Log

class HistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            Log.d("HistoryViewModel", "Loading history...")
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val items = historyRepository.getUserAnalysisHistory()
                Log.d("HistoryViewModel", "Loaded ${items.size} history items")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    historyItems = items.sortedByDescending { it.createdAt }
                )
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error loading history: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading history: ${e.message}"
                )
            }
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            try {
                Log.d("HistoryViewModel", "Deleting history item: $id")
                historyRepository.deleteHistoryById(id)
                loadHistory()
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to delete item: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete item: ${e.message}"
                )
            }
        }
    }
}

data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyItems: List<AnalysisHistory> = emptyList(),
    val error: String? = null
)