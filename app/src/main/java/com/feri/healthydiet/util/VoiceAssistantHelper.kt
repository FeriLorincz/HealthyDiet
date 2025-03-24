package com.feri.healthydiet.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class VoiceAssistantHelper(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val TAG = "VoiceAssistantHelper"

    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.setSpeechRate(0.9f) // Slightly slower for better clarity
                Log.d(TAG, "TextToSpeech initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech: $status")
            }
            continuation.resume(isInitialized)
        }
    }

    suspend fun speak(text: String): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isInitialized) {
            Log.e(TAG, "TextToSpeech not initialized")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Speech started")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Speech completed")
                continuation.resume(true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Speech error occurred")
                continuation.resume(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Speech error: $errorCode")
                continuation.resume(false)
            }
        })

        val result = textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,  // Bundle nu este necesar aici
            utteranceId
        ) ?: TextToSpeech.ERROR

        if (result == TextToSpeech.ERROR) {
            continuation.resume(false)
        }
    }

    fun stop() {
        textToSpeech?.stop()
        Log.d(TAG, "Speech stopped")
    }

    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        Log.d(TAG, "TextToSpeech shut down")
    }
}