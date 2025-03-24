package com.feri.healthydiet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                val healthProfile = userRepository.getUserHealthProfile()

                _uiState.value = _uiState.value.copy(
                    userName = user.name,
                    healthConditions = getHealthConditionsList(healthProfile),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error loading user data: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun getHealthConditionsList(profile: HealthProfile): List<String> {
        val conditions = mutableListOf<String>()

        if (profile.hasDiabetes) conditions.add("Diabetes")
        if (profile.hasLiverSteatosis) conditions.add("Fatty Liver Disease")
        if (profile.hasHypertension) conditions.add("Hypertension")
        if (profile.hasHighCholesterol) conditions.add("High Cholesterol")
        if (profile.hasCeliac) conditions.add("Celiac Disease")
        conditions.addAll(profile.customConditions)

        return conditions
    }
}

data class DashboardUiState(
    val userName: String = "",
    val healthConditions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)