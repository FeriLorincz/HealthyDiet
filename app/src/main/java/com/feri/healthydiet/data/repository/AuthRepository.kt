package com.feri.healthydiet.data.repository

import android.util.Log
import com.feri.healthydiet.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthRepository(
    private val userRepository: UserRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthRepository"

    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Sincronizează cu baza de date locală
                val localUser = userRepository.getUserByEmail(email)

                if (localUser == null) {
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

                Result.success(firebaseUser.uid)
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Actualizează profilul utilizatorului cu numele
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                firebaseUser.updateProfile(profileUpdates).await()

                // Salvează utilizatorul în baza de date locală
                val newUser = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    profilePhotoUrl = null,
                    createdAt = System.currentTimeMillis()
                )
                userRepository.saveUser(newUser)

                // Setează userId-ul curent
                userRepository.setCurrentUserId(firebaseUser.uid)

                Result.success(firebaseUser.uid)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        userRepository.clearCurrentUserId()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}