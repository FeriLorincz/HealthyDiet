package com.feri.healthydiet.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feri.healthydiet.data.model.User
import com.feri.healthydiet.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthViewModel"

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            // Verifică dacă utilizatorul există
                            val localUser = userRepository.getUserByEmail(email)

                            if (localUser == null) {
                                // Dacă nu există, creează un utilizator nou
                                val newUser = User(
                                    id = firebaseUser.uid,
                                    name = firebaseUser.displayName ?: "User",
                                    email = firebaseUser.email ?: email,
                                    profilePhotoUrl = firebaseUser.photoUrl?.toString(),
                                    createdAt = System.currentTimeMillis()
                                )
                                userRepository.saveUser(newUser)
                            }

                            // Setează userId-ul curent
                            userRepository.setCurrentUserId(firebaseUser.uid)

                            withContext(Dispatchers.Main) {
                                _authState.value = AuthState.Success
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing with local database", e)
                            withContext(Dispatchers.Main) {
                                _authState.value = AuthState.Error("Login successful but failed to sync with local database")
                            }
                        }
                    }
                } else {
                    _authState.value = AuthState.Error("Login failed: User is null")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Login failed", e)
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
    }

    fun register(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Registration successful with Firebase")
                    val firebaseUser = auth.currentUser

                    // Actualizează numele utilizatorului în Firebase
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    firebaseUser?.updateProfile(profileUpdates)
                        ?.addOnSuccessListener {
                            Log.d(TAG, "User profile updated successfully")
                            // Salvează utilizatorul în baza de date
                            saveUserToLocalDatabase(firebaseUser, name, email)
                        }
                        ?.addOnFailureListener { e ->
                            Log.e(TAG, "Failed to update profile", e)
                            // Chiar dacă actualizarea profilului eșuează, încercăm să salvăm utilizatorul
                            saveUserToLocalDatabase(firebaseUser, name, email)
                        }
                } else {
                    Log.e(TAG, "Registration failed", task.exception)
                    val errorMessage = task.exception?.message ?: "Registration failed"

                    // Gestionare specifică a erorilor reCAPTCHA
                    if (errorMessage.contains("CONFIGURATION_NOT_FOUND")) {
                        _authState.value = AuthState.Error("Registration failed: reCAPTCHA issue. Check Firebase settings.")
                    } else {
                        _authState.value = AuthState.Error(errorMessage)
                    }
                }
            }
    }

    // Metodă separată pentru a salva utilizatorul în baza de date locală
    private fun saveUserToLocalDatabase(firebaseUser: com.google.firebase.auth.FirebaseUser?, name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = firebaseUser?.uid ?: UUID.randomUUID().toString()

                // Creează noul utilizator
                val newUser = User(
                    id = userId,
                    name = name,
                    email = email,
                    profilePhotoUrl = firebaseUser?.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis()
                )

                // Salvează utilizatorul și gestionează rezultatul
                val saved = userRepository.saveUser(newUser)

                if (saved) {
                    // Setează ID-ul utilizatorului curent doar dacă salvarea a reușit
                    userRepository.setCurrentUserId(userId)

                    withContext(Dispatchers.Main) {
                        _authState.value = AuthState.Success
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _authState.value = AuthState.Error("Registration successful but failed to save user data")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user to local database", e)
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("Error saving user data: ${e.message}")
                }
            }
        }
    }


    private fun handleUserSave(firebaseUser: FirebaseUser?, name: String, email: String) {
        viewModelScope.launch {
            try {
                val userId = firebaseUser?.uid ?: UUID.randomUUID().toString()

                // Verifică dacă utilizatorul există deja
                val existingUser = userRepository.getUserByEmail(email)

                // Creează sau actualizează utilizatorul
                val user = existingUser?.copy(
                    name = name,
                    profilePhotoUrl = firebaseUser?.photoUrl?.toString()
                ) ?: User(
                    id = userId,
                    name = name,
                    email = email,
                    profilePhotoUrl = firebaseUser?.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis()
                )

                // Salvează utilizatorul și verifică rezultatul
                val saveSuccess = userRepository.saveUser(user)

                if (saveSuccess) {
                    // Setează userId-ul curent
                    userRepository.setCurrentUserId(userId)
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Failed to save user data")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user: ${e.message}", e)
                _authState.value = AuthState.Error("Error saving user data: ${e.message}")
            }
        }
    }


    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
                userRepository.clearCurrentUserId()
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                _authState.value = AuthState.Error("Logout failed: ${e.message}")
            }
        }
    }

    fun checkAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                userRepository.setCurrentUserId(currentUser.uid)
                _authState.value = AuthState.Success
            }
        } else {
            _authState.value = AuthState.Idle
        }
    }
}
