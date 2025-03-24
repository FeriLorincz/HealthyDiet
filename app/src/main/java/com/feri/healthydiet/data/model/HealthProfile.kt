package com.feri.healthydiet.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_profiles",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HealthProfile(

    @PrimaryKey val id: String,
    val userId: String,
    val hasDiabetes: Boolean = false,
    val hasLiverSteatosis: Boolean = false,
    val hasHypertension: Boolean = false,
    val hasHighCholesterol: Boolean = false,
    val hasCeliac: Boolean = false,
    val customConditions: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
