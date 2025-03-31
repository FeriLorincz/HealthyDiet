package com.feri.healthydiet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feri.healthydiet.data.model.AnalysisHistory

@Dao
interface AnalysisHistoryDao {
    @Query("SELECT * FROM analysis_history WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getHistoryForUser(userId: String): List<AnalysisHistory>

    @Query("SELECT * FROM analysis_history WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentHistoryForUser(userId: String, limit: Int): List<AnalysisHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysisHistory: AnalysisHistory)

    @Query("DELETE FROM analysis_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM analysis_history WHERE userId = :userId")
    suspend fun clearUserHistory(userId: String)
}