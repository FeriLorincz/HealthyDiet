package com.feri.healthydiet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.HealthProfile
import com.feri.healthydiet.data.model.User

@Database(
    entities = [
        User::class,
        HealthProfile::class,
        AnalysisHistory::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun healthProfileDao(): HealthProfileDao
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
                    // Adăugați aceste linii pentru a rezolva problema FOREIGN KEY
                    .setJournalMode(JournalMode.TRUNCATE)
                    // Execută un callback după crearea bazei de date pentru a dezactiva verificarea FK
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Dezactivează verificarea cheilor străine
                            db.execSQL("PRAGMA foreign_keys = OFF")
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Opțional: reactivați verificarea după operațiuni inițiale
                            // db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}