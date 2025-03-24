package com.feri.healthydiet.data.repository

import com.feri.healthydiet.data.local.AnalysisHistoryDao
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.AnalysisType
import com.feri.healthydiet.data.remote.FoodAnalysisResult
import com.feri.healthydiet.ui.menuscan.MenuAnalysisResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AnalysisRepository(
    private val analysisHistoryDao: AnalysisHistoryDao,
    private val userRepository: UserRepository
) {
    private val gson = Gson()

    suspend fun saveMenuAnalysis(menuText: String, result: MenuAnalysisResult) = withContext(Dispatchers.IO) {
        val userId = userRepository.getCurrentUserId()
        val analysisHistory = AnalysisHistory(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = AnalysisType.MENU,
            name = "Menu Analysis",
            content = gson.toJson(result),
            createdAt = System.currentTimeMillis()
        )
        analysisHistoryDao.insert(analysisHistory)
    }

    suspend fun saveFoodAnalysis(foodName: String, result: FoodAnalysisResult) = withContext(Dispatchers.IO) {
        val userId = userRepository.getCurrentUserId()
        val analysisHistory = AnalysisHistory(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = AnalysisType.FOOD_ITEM,
            name = foodName,
            content = gson.toJson(result),
            createdAt = System.currentTimeMillis()
        )
        analysisHistoryDao.insert(analysisHistory)
    }

    suspend fun getUserAnalysisHistory() = withContext(Dispatchers.IO) {
        val userId = userRepository.getCurrentUserId()
        analysisHistoryDao.getHistoryForUser(userId)
    }

    suspend fun getRecentAnalyses(limit: Int = 5) = withContext(Dispatchers.IO) {
        val userId = userRepository.getCurrentUserId()
        analysisHistoryDao.getRecentHistoryForUser(userId, limit)
    }

    suspend fun deleteAnalysis(id: String) = withContext(Dispatchers.IO) {
        analysisHistoryDao.deleteById(id)
    }
}