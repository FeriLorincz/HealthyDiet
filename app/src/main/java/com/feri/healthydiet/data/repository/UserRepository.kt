package com.feri.healthydiet.data.repository

import com.feri.healthydiet.data.local.HealthProfileDao
import com.feri.healthydiet.data.local.UserDao
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val healthProfileDao: HealthProfileDao
) {
    // Simulăm un utilizator autentificat
    private var currentUserId: String? = null

    suspend fun getCurrentUser(): User = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: getDefaultUserId()
        val user = userDao.getUserById(userId).first()
        return@withContext user ?: createDefaultUser()
    }

    fun getCurrentUserId(): String {
        return currentUserId ?: getDefaultUserId()
    }

    private fun getDefaultUserId(): String {
        val storedId = currentUserId
        return if (storedId.isNullOrEmpty()) {
            UUID.randomUUID().toString().also { currentUserId = it }
        } else {
            storedId
        }
    }

    private suspend fun createDefaultUser(): User {
        val userId = getDefaultUserId()
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

            currentUserId = userId
            return@withContext userId
        }
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insert(user)
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