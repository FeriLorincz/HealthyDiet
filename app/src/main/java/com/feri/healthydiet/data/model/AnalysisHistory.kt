package com.feri.healthydiet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "analysis_history")
data class AnalysisHistory(
    @PrimaryKey val id: String,
    val userId: String,
    val type: AnalysisType,
    val name: String,
    val content: String, // JSON-encoded content
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

enum class AnalysisType {
    MENU, FOOD_ITEM
}
