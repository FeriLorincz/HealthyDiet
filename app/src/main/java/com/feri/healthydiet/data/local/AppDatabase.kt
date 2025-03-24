package com.feri.healthydiet.data.local

import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.model.User

@Database(
    entities = [
        User::class,
        HealthProfile::class,
        //FoodItem::class,
        AnalysisHistory::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun healthProfileDao(): HealthProfileDao
    //abstract fun foodItemDao(): FoodItemDao
    abstract fun analysisHistoryDao(): AnalysisHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthy_diet_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}