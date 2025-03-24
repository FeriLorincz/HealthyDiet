package com.feri.healthydiet.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AIService {

    @Headers("Content-Type: application/json", "x-api-key: YOUR_API_KEY")
    @POST("v1/complete")
    suspend fun analyzeFoodItem(@Body request: AnalysisRequest): AnalysisResponse

    @Headers("Content-Type: application/json", "x-api-key: YOUR_API_KEY")
    @POST("v1/complete")
    suspend fun analyzeMenu(@Body request: MenuAnalysisRequest): MenuAnalysisResponse
}

data class AnalysisRequest(
    val prompt: String,
    val model: String = "claude-3-haiku-20240307",
    val max_tokens: Int = 1000
)

data class AnalysisResponse(
    val completion: String,
    val stop_reason: String
)

data class MenuAnalysisRequest(
    val prompt: String,
    val model: String = "claude-3-sonnet-20240229",
    val max_tokens: Int = 2000
)

data class MenuAnalysisResponse(
    val completion: String,
    val stop_reason: String
)