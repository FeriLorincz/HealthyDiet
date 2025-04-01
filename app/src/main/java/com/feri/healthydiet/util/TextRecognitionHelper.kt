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
                Log.d("TextRecognitionHelper", "Starting text recognition from URI: $imageUri")

                // Creează imaginea de input din URI
                val image = InputImage.fromFilePath(context, imageUri)
                Log.d("TextRecognitionHelper", "Created input image, width: ${image.width}, height: ${image.height}")

                // Creează un recunoascător de text
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                // Procesează imaginea pentru a extrage textul
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        Log.d("TextRecognitionHelper", "Text recognition successful: ${recognizedText.length} characters")
                        if (recognizedText.isBlank()) {
                            Log.w("TextRecognitionHelper", "Recognized text is blank!")
                        } else {
                            Log.d("TextRecognitionHelper", "First 100 chars: ${recognizedText.take(100)}...")
                        }

                        // Trimite rezultatul în coroutine
                        continuation.resume(recognizedText)

                        // Eliberează resursele
                        recognizer.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextRecognitionHelper", "Text recognition failed: ${e.message}", e)
                        continuation.resumeWithException(e)

                        // Eliberează resursele
                        recognizer.close()
                    }

                // Configurează ce se întâmplă dacă coroutina este anulată
                continuation.invokeOnCancellation {
                    Log.d("TextRecognitionHelper", "Text recognition cancelled")
                    recognizer.close()
                }
            } catch (e: Exception) {
                Log.e("TextRecognitionHelper", "Error in text recognition: ${e.message}", e)
                continuation.resumeWithException(e)
            }
        }
    }
}