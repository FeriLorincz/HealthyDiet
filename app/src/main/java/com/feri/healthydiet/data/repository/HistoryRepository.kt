package com.feri.healthydiet.data.repository

import android.util.Log
import com.feri.healthydiet.data.local.AnalysisHistoryDao
import com.feri.healthydiet.data.model.AnalysisHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val analysisHistoryDao: AnalysisHistoryDao,
    private val userRepository: UserRepository  // Adaugă dependența către UserRepository
) {

    suspend fun getUserAnalysisHistory(): List<AnalysisHistory> = withContext(Dispatchers.IO) {
        try {
            // Obține ID-ul utilizatorului autentificat din UserRepository
            val userId = userRepository.getCurrentUserId()
            Log.d("HistoryRepository", "Getting history for user ID: $userId")
            val historyItems = analysisHistoryDao.getHistoryForUser(userId)
            Log.d("HistoryRepository", "Found ${historyItems.size} history items")
            return@withContext historyItems
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error getting user history: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun saveAnalysisHistory(analysisHistory: AnalysisHistory) = withContext(Dispatchers.IO) {
        try {
            Log.d("HistoryRepository", "Saving analysis history: ${analysisHistory.id}, type: ${analysisHistory.type}, userId: ${analysisHistory.userId}")
            analysisHistoryDao.insert(analysisHistory)
            Log.d("HistoryRepository", "Analysis history saved successfully")
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error saving analysis history: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteHistoryById(historyId: String) = withContext(Dispatchers.IO) {
        analysisHistoryDao.deleteById(historyId)
    }

    suspend fun clearUserHistory(userId: String) = withContext(Dispatchers.IO) {
        analysisHistoryDao.clearUserHistory(userId)
    }
}