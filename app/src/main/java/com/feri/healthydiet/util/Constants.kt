package com.feri.healthydiet.util

import com.feri.healthydiet.BuildConfig

object Constants {
    // API Constants
    const val ANTHROPIC_API_BASE_URL = "https://api.anthropic.com/"
    const val ANTHROPIC_API_KEY = BuildConfig.ANTHROPIC_API_KEY // Înlocuiește cu cheia ta API

    // Model names
    const val DEFAULT_MODEL = "claude-3-haiku-20240307"
    const val DETAILED_MODEL = "claude-3-sonnet-20240229"

    // Database constants
    const val DATABASE_NAME = "healthy_diet_database"

    // Preferences constants
    const val PREF_CURRENT_USER_ID = "current_user_id"
    const val PREF_NAME = "healthy_diet_preferences"

    // Permissions
    const val PERMISSION_CAMERA_REQUEST_CODE = 100
    const val PERMISSION_STORAGE_REQUEST_CODE = 101
}