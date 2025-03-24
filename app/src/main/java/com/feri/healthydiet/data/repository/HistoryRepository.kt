package com.feri.healthydiet.data.repository

import com.feri.healthydiet.data.local.AnalysisHistoryDao
import com.feri.healthydiet.data.model.AnalysisHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val analysisHistoryDao: AnalysisHistoryDao
) {

    suspend fun getUserAnalysisHistory(): List<AnalysisHistory> = withContext(Dispatchers.IO) {
        // În aplicația reală, obține ID-ul utilizatorului autentificat
        val userId = "current-user-id"
        return@withContext analysisHistoryDao.getHistoryForUser(userId)
    }

    suspend fun saveAnalysisHistory(analysisHistory: AnalysisHistory) = withContext(Dispatchers.IO) {
        analysisHistoryDao.insert(analysisHistory)
    }

    suspend fun deleteHistoryById(historyId: String) = withContext(Dispatchers.IO) {
        analysisHistoryDao.deleteById(historyId)
    }

    suspend fun clearUserHistory(userId: String) = withContext(Dispatchers.IO) {
        analysisHistoryDao.clearUserHistory(userId)
    }
}