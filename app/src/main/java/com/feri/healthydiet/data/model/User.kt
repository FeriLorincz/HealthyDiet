package com.feri.healthydiet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val profilePhotoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
