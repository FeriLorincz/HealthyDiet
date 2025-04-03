package com.feri.healthydiet.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.feri.healthydiet.data.local.HealthProfileDao
import com.feri.healthydiet.data.local.UserDao
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.model.User
import com.feri.healthydiet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val healthProfileDao: HealthProfileDao,
    private val context: Context
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME, Context.MODE_PRIVATE
    )

    // Metodă pentru a seta ID-ul utilizatorului curent
    fun setCurrentUserId(userId: String) {
        preferences.edit().putString(Constants.PREF_CURRENT_USER_ID, userId).apply()
    }

    // Metodă pentru a șterge ID-ul utilizatorului curent (la logout)
    fun clearCurrentUserId() {
        preferences.edit().remove(Constants.PREF_CURRENT_USER_ID).apply()
    }

    suspend fun getCurrentUser(): User = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
        val user = userDao.getUserById(userId).first()
        return@withContext user ?: createDefaultUser()
    }

    fun getCurrentUserId(): String {
        return preferences.getString(Constants.PREF_CURRENT_USER_ID, null) ?: getDefaultUserId()
    }

    private fun getDefaultUserId(): String {
        val defaultId = UUID.randomUUID().toString()
        setCurrentUserId(defaultId)
        return defaultId
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        return@withContext userDao.getUserByEmail(email)
    }

    private suspend fun createDefaultUser(): User {
        val userId = getCurrentUserId()
        val newUser = User(
            id = userId,
            name = "Guest User",
            email = "guest@example.com"
        )
        saveUser(newUser)
        createDefaultHealthProfile(userId)
        return newUser
    }

    suspend fun getUserHealthProfile(): HealthProfile = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
        val profile = healthProfileDao.getProfileForUser(userId).first()
        return@withContext profile ?: createDefaultHealthProfile(userId)
    }

    private suspend fun createDefaultHealthProfile(userId: String): HealthProfile {
        val newProfile = HealthProfile(
            id = UUID.randomUUID().toString(),
            userId = userId,
            hasDiabetes = false,
            hasLiverSteatosis = false,
            hasHypertension = false,
            hasHighCholesterol = false,
            hasCeliac = false,
            customConditions = emptyList()
        )
        saveHealthProfile(newProfile)
        return newProfile
    }

    suspend fun ensureUserExists(): String = withContext(Dispatchers.IO) {
        try {
            // Forțează crearea unui utilizator și a unui profil de sănătate dacă nu există
            val user = getCurrentUser() // Aceasta va crea utilizatorul dacă nu există
            return@withContext user.id
        } catch (e: Exception) {
            // În caz de eroare, creați manual utilizatorul
            val userId = UUID.randomUUID().toString()
            val newUser = User(
                id = userId,
                name = "Guest User",
                email = "guest@example.com",
                createdAt = System.currentTimeMillis()
            )

            try {
                userDao.insert(newUser)
            } catch (e: Exception) {
                // Ignorăm erorile de inserare, poate utilizatorul există deja
            }

            try {
                // Creăm și un profil de sănătate
                val profile = HealthProfile(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    hasDiabetes = false,
                    hasLiverSteatosis = false,
                    hasHypertension = false,
                    hasHighCholesterol = false,
                    hasCeliac = false,
                    customConditions = emptyList(),
                    updatedAt = System.currentTimeMillis()
                )
                healthProfileDao.insert(profile)
            } catch (e: Exception) {
                // Ignorăm erorile de inserare, poate profilul există deja
            }

            setCurrentUserId(userId)
            return@withContext userId
        }
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        try {
            // Verifică dacă utilizatorul există deja
            val existingUser = getUserByEmail(user.email)
            if (existingUser != null) {
                // Actualizează utilizatorul existent cu datele noi
                val updatedUser = existingUser.copy(
                    name = user.name,
                    profilePhotoUrl = user.profilePhotoUrl,
                    // Păstrăm createdAt original
                    createdAt = existingUser.createdAt
                )
                userDao.update(updatedUser)
            } else {
                // Inserăm utilizatorul nou
                userDao.insert(user)
            }
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving user: ${e.message}", e)
            false
        }
    }

    suspend fun hasUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userFlow = userDao.getUserById(userId)
            val user = userFlow.first() // Flow.first() returnează primul element sau aruncă NoSuchElementException
            return@withContext user != null
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking if user exists: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.update(user)
    }

    suspend fun saveHealthProfile(healthProfile: HealthProfile) = withContext(Dispatchers.IO) {
        healthProfileDao.insert(healthProfile)
    }

    suspend fun updateHealthProfile(healthProfile: HealthProfile) = withContext(Dispatchers.IO) {
        healthProfileDao.update(healthProfile)
    }
}