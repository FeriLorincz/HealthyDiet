package com.feri.healthydiet.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.model.User
import com.feri.healthydiet.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserWithHealthProfile?>()
    val userProfile: LiveData<UserWithHealthProfile?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userRepository.getCurrentUser()
                val healthProfile = userRepository.getUserHealthProfile()

                _userProfile.value = UserWithHealthProfile(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    healthProfile = healthProfile
                )
            } catch (e: Exception) {
                // Handle error - create a new user profile if none exists
                createNewUserProfile()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveUserProfile(
        name: String,
        email: String,
        hasDiabetes: Boolean,
        hasLiverSteatosis: Boolean,
        hasHypertension: Boolean,
        hasHighCholesterol: Boolean,
        hasCeliac: Boolean,
        customConditions: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProfile = _userProfile.value

                if (currentProfile != null) {
                    // Update existing profile
                    val updatedUser = User(
                        id = currentProfile.id,
                        name = name,
                        email = email
                    )

                    val updatedHealthProfile = HealthProfile(
                        id = currentProfile.healthProfile.id,
                        userId = currentProfile.id,
                        hasDiabetes = hasDiabetes,
                        hasLiverSteatosis = hasLiverSteatosis,
                        hasHypertension = hasHypertension,
                        hasHighCholesterol = hasHighCholesterol,
                        hasCeliac = hasCeliac,
                        customConditions = customConditions
                    )

                    userRepository.updateUser(updatedUser)
                    userRepository.updateHealthProfile(updatedHealthProfile)

                    _userProfile.value = UserWithHealthProfile(
                        id = updatedUser.id,
                        name = updatedUser.name,
                        email = updatedUser.email,
                        healthProfile = updatedHealthProfile
                    )
                } else {
                    // Create new profile
                    val userId = UUID.randomUUID().toString()
                    val profileId = UUID.randomUUID().toString()

                    val newUser = User(
                        id = userId,
                        name = name,
                        email = email
                    )

                    val newHealthProfile = HealthProfile(
                        id = profileId,
                        userId = userId,
                        hasDiabetes = hasDiabetes,
                        hasLiverSteatosis = hasLiverSteatosis,
                        hasHypertension = hasHypertension,
                        hasHighCholesterol = hasHighCholesterol,
                        hasCeliac = hasCeliac,
                        customConditions = customConditions
                    )

                    userRepository.saveUser(newUser)
                    userRepository.saveHealthProfile(newHealthProfile)

                    _userProfile.value = UserWithHealthProfile(
                        id = newUser.id,
                        name = newUser.name,
                        email = newUser.email,
                        healthProfile = newHealthProfile
                    )
                }

                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createNewUserProfile() {
        _userProfile.value = UserWithHealthProfile(
            id = "",
            name = "",
            email = "",
            healthProfile = HealthProfile(
                id = "",
                userId = "",
                hasDiabetes = false,
                hasLiverSteatosis = false,
                hasHypertension = false,
                hasHighCholesterol = false,
                hasCeliac = false,
                customConditions = emptyList()
            )
        )
    }
}

data class UserWithHealthProfile(
    val id: String,
    val name: String,
    val email: String,
    val healthProfile: HealthProfile
)