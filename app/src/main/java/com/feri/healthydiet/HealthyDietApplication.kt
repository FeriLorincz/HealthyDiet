package com.feri.healthydiet

import android.app.Application
import android.util.Log
import com.feri.healthydiet.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HealthyDietApplication : Application() {
    override fun onCreate() {
        try {
            Log.d("HealthyDietApplication", "onCreate: Start")
            super.onCreate()
            Log.d("HealthyDietApplication", "onCreate: After super.onCreate")

            startKoin {
                Log.d("HealthyDietApplication", "startKoin: Configuring")
                androidLogger()
                androidContext(this@HealthyDietApplication)
                modules(appModule)
                Log.d("HealthyDietApplication", "startKoin: Configured successfully")
            }
            Log.d("HealthyDietApplication", "onCreate: End")
        } catch (e: Exception) {
            Log.e("HealthyDietApplication", "Error in application initialization: ${e.message}", e)
        }
    }
}