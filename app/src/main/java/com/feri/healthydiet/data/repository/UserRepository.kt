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
        Log.d("UserRepository", "Getting user with ID: $userId")

        try {
            val userFlow = userDao.getUserById(userId)
            val user = userFlow.first()
            Log.d("UserRepository", "Retrieved user from DB: $user")
            return@withContext user ?: createDefaultUser()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user: ${e.message}", e)
            return@withContext createDefaultUser()
        }
    }

    suspend fun getUserHealthProfile(): HealthProfile = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
        try {
            val profile = healthProfileDao.getProfileForUser(userId).first()
            return@withContext profile ?: createDefaultHealthProfile(userId)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting health profile: ${e.message}", e)
            return@withContext createDefaultHealthProfile(userId)
        }
    }

    fun getCurrentUserId(): String {
        val userId = preferences.getString(Constants.PREF_CURRENT_USER_ID, null)
        Log.d("UserRepository", "Current user ID from preferences: $userId")
        return userId ?: getDefaultUserId()
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

    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
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
            return@withContext true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving user: ${e.message}", e)
            return@withContext false
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
        try {
            userDao.update(user)
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user: ${e.message}", e)
            false
        }
    }

    suspend fun updateHealthProfile(healthProfile: HealthProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Updating health profile: $healthProfile")
            healthProfileDao.update(healthProfile)
            return@withContext true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating health profile: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun saveHealthProfile(healthProfile: HealthProfile) = withContext(Dispatchers.IO) {
        healthProfileDao.insert(healthProfile)
    }

    suspend fun testDatabaseFunctionality() {
        withContext(Dispatchers.IO) {
            try {
                val testUser = User(
                    id = "test-${System.currentTimeMillis()}",
                    name = "Test User",
                    email = "test@example.com",
                    createdAt = System.currentTimeMillis()
                )

                Log.d("UserRepository", "Testing DB: Inserting test user")
                userDao.insert(testUser)

                val retrievedUser = userDao.getUserById(testUser.id).first()
                Log.d("UserRepository", "Testing DB: Retrieved user: $retrievedUser")

                // Testează și HealthProfile
                val testProfile = HealthProfile(
                    id = "test-profile-${System.currentTimeMillis()}",
                    userId = testUser.id,
                    hasDiabetes = true,
                    updatedAt = System.currentTimeMillis()
                )

                Log.d("UserRepository", "Testing DB: Inserting test health profile")
                healthProfileDao.insert(testProfile)

                val retrievedProfile = healthProfileDao.getProfileForUser(testUser.id).first()
                Log.d("UserRepository", "Testing DB: Retrieved profile: $retrievedProfile")
            } catch (e: Exception) {
                Log.e("UserRepository", "Database test failed: ${e.message}", e)
            }
        }
    }
}