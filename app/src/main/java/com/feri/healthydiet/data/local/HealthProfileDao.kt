package com.feri.healthydiet.data.local

import androidx.room.*
import com.feri.healthydiet.data.model.HealthProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(healthProfile: HealthProfile)

    @Update
    suspend fun update(healthProfile: HealthProfile)

    @Delete
    suspend fun delete(healthProfile: HealthProfile)

    @Query("SELECT * FROM health_profiles WHERE userId = :userId")
    fun getProfileForUser(userId: String): Flow<HealthProfile?>
}