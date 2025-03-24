package com.feri.healthydiet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Recommended", "Moderate", "Avoid"
    val protein: String,
    val carbs: String,
    val fats: String,
    val healthImpacts: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)