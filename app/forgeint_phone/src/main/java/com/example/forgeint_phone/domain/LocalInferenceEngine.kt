package com.example.forgeint_phone.domain

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalInferenceEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalInferenceEngine"
        // We removed the hardcoded MODEL_NAME here because the Path is passed dynamically

        init {
            try {
                System.loadLibrary("forgeint_local")
                Log.d(TAG, "Native library loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }

    // --- Native Methods ---
    external fun loadModel(modelPath: String): Boolean
    external fun generateResponse(prompt: String): String
    external fun unloadModel()

    /**
     * Simplified initialization.
     * It no longer attempts to copy from assets (which would crash for a 2B model).
     * It simply verifies the file exists and passes the path to C++.
     */
    suspend fun loadModelFile(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)

            if (!file.exists()) {
                Log.e(TAG, "Model file found at $filePath")
                return@withContext false
            }

            Log.d(TAG, "Loading model from: ${file.absolutePath}")
            return@withContext loadModel(file.absolutePath)
        }
    }
}