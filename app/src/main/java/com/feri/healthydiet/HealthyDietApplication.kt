package com.feri.healthydiet

import android.app.Application
import com.feri.healthydiet.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HealthyDietApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@HealthyDietApplication)
            modules(appModule)
        }
    }
}