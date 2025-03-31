package com.feri.healthydiet.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TextRecognitionHelper {
    private val TAG = "TextRecognitionHelper"

    suspend fun recognizeTextFromImage(imageUri: Uri, context: Context): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Creează imaginea de input din URI
                val image = InputImage.fromFilePath(context, imageUri)

                // Creează un recunoascător de text
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                // Procesează imaginea pentru a extrage textul
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        Log.d(TAG, "Text recognition successful: ${visionText.text.length} characters")

                        // Trimite rezultatul în coroutine
                        continuation.resume(visionText.text)

                        // Eliberează resursele
                        recognizer.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Text recognition failed: ${e.message}", e)
                        continuation.resumeWithException(e)

                        // Eliberează resursele
                        recognizer.close()
                    }

                // Configurează ce se întâmplă dacă coroutina este anulată
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Text recognition cancelled")
                    recognizer.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in text recognition: ${e.message}", e)
                continuation.resumeWithException(e)
            }
        }
    }
}