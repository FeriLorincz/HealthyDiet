package com.feri.healthydiet.di

import com.feri.healthydiet.data.local.AppDatabase
import com.feri.healthydiet.data.remote.AnthropicService
import com.feri.healthydiet.data.repository.AnalysisRepository
import com.feri.healthydiet.data.repository.HistoryRepository
import com.feri.healthydiet.data.repository.UserRepository
import com.feri.healthydiet.ui.dashboard.DashboardViewModel
import com.feri.healthydiet.ui.foodanalyzer.FoodAnalyzerViewModel
import com.feri.healthydiet.ui.history.HistoryViewModel
import com.feri.healthydiet.ui.menuscan.MenuScanViewModel
import com.feri.healthydiet.ui.profile.ProfileViewModel
import com.feri.healthydiet.util.Constants
import com.feri.healthydiet.util.SpeechRecognitionHelper
import com.feri.healthydiet.util.TextRecognitionHelper
import com.feri.healthydiet.util.VoiceAssistantHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    // Database
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().healthProfileDao() }
    single { get<AppDatabase>().analysisHistoryDao() }

    // Network
    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(Constants.ANTHROPIC_API_BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API Services
    single { get<Retrofit>().create(AnthropicService::class.java) }

    // Repositories
    single { UserRepository(get(), get()) }
    single { HistoryRepository(get()) }
    single { AnalysisRepository(get(), get()) }

    // Helpers
    single { TextRecognitionHelper() }
    single { VoiceAssistantHelper(androidContext()) }
    single { SpeechRecognitionHelper() }

    // ViewModels
    viewModel { DashboardViewModel(get()) }
    viewModel { MenuScanViewModel(get(), get(), get(), get()) }
    viewModel { FoodAnalyzerViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
}