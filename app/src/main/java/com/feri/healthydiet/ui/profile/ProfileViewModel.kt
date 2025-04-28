package com.feri.healthydiet.ui.profile

import android.util.Log
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

    init {
        // Testează baza de date la inițializarea ViewModel-ului
        viewModelScope.launch {
            try {
                userRepository.testDatabaseFunctionality()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Database test failed: ${e.message}", e)
            }
        }
        // Apoi încărcăm profilul
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("ProfileViewModel", "Starting to load user profile")
                val user = userRepository.getCurrentUser()
                Log.d("ProfileViewModel", "Loaded user: $user")

                val healthProfile = userRepository.getUserHealthProfile()
                Log.d("ProfileViewModel", "Loaded health profile: $healthProfile")

                _userProfile.value = UserWithHealthProfile(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    healthProfile = healthProfile
                )
                Log.d("ProfileViewModel", "User profile set to LiveData")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile: ${e.message}", e)
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
                Log.d("ProfileViewModel", "Saving profile with conditions: $customConditions, Diabetes: $hasDiabetes")
                val currentProfile = _userProfile.value

                if (currentProfile != null) {
                    val updatedUser = User(
                        id = currentProfile.id,
                        name = name,
                        email = email,
                        profilePhotoUrl = null, // sau currentProfile.profilePhotoUrl
                        createdAt = System.currentTimeMillis()
                    )

                    // Folosește id-ul profilului de sănătate existent
                    val healthProfileId = currentProfile.healthProfile.id

                    val updatedHealthProfile = HealthProfile(
                        id = healthProfileId,
                        userId = currentProfile.id,
                        hasDiabetes = hasDiabetes,
                        hasLiverSteatosis = hasLiverSteatosis,
                        hasHypertension = hasHypertension,
                        hasHighCholesterol = hasHighCholesterol,
                        hasCeliac = hasCeliac,
                        customConditions = customConditions,
                        updatedAt = System.currentTimeMillis()
                    )

                    Log.d("ProfileViewModel", "Updating health profile: $updatedHealthProfile")
                    try {
                        userRepository.updateUser(updatedUser)
                        userRepository.updateHealthProfile(updatedHealthProfile)
                        // Actualizează LiveData cu noile valori
                        _userProfile.value = UserWithHealthProfile(
                            id = updatedUser.id,
                            name = updatedUser.name,
                            email = updatedUser.email,
                            healthProfile = updatedHealthProfile
                        )

                        _saveSuccess.value = true
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Error updating profile: ${e.message}", e)
                        _saveSuccess.value = false
                    }
                } else {
                    // Create new profile if none exists
                    val userId = userRepository.getCurrentUserId()
                    val profileId = UUID.randomUUID().toString()

                    val newUser = User(
                        id = userId,
                        name = name,
                        email = email,
                        profilePhotoUrl = null,
                        createdAt = System.currentTimeMillis()
                    )

                    val newHealthProfile = HealthProfile(
                        id = profileId,
                        userId = userId,
                        hasDiabetes = hasDiabetes,
                        hasLiverSteatosis = hasLiverSteatosis,
                        hasHypertension = hasHypertension,
                        hasHighCholesterol = hasHighCholesterol,
                        hasCeliac = hasCeliac,
                        customConditions = customConditions,
                        updatedAt = System.currentTimeMillis()
                    )

                    try {
                        userRepository.saveUser(newUser)
                        userRepository.saveHealthProfile(newHealthProfile)

                        _userProfile.value = UserWithHealthProfile(
                            id = newUser.id,
                            name = newUser.name,
                            email = newUser.email,
                            healthProfile = newHealthProfile
                        )

                        _saveSuccess.value = true
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Error creating profile: ${e.message}", e)
                        _saveSuccess.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error in saveUserProfile: ${e.message}", e)
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