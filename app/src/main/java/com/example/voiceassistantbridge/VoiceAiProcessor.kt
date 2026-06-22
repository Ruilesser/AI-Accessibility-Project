package com.example.voiceassistantbridge

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VoiceAiProcessor {

    // Initialize the Gemini Developer API backend service exactly as configured in your project
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = "gemini-3.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json" // Force Gemini to reply in pure JSON
            },
            systemInstruction = content {
                text("You are an Android accessibility bridge engine. " +
                        "Analyze the user's spoken request and map it to one of these actions: " +
                        "1. ACTION_TEST (if they want to run a test/diagnostic) " +
                        "2. ACTION_YOUTUBE_LATER (if they want to see, watch, or open their watch later playlist) " +
                        "3. ACTION_CLICK (if they want to click a visible screen item or button) " +
                        "Your response must be a strict JSON object with two fields: " +
                        " 'action': string (one of the three choices above) " +
                        " 'target': string (the literal text string of the button they want clicked, empty string otherwise).")
            }
        )

    /**
     * Sends the raw text string to Gemini and extracts the structured JSON command payload.
     */
    suspend fun processVoiceIntent(rawSpokenText: String): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "User said: \"$rawSpokenText\""
                val response = model.generateContent(prompt)
                val jsonText = response.text ?: ""

                // Manual JSON parsing
                val actionRegex = "\"action\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val targetRegex = "\"target\"\\s*:\\s*\"([^\"]+)\"".toRegex()

                val action = actionRegex.find(jsonText)?.groupValues?.get(1) ?: "ACTION_CLICK"
                val target = targetRegex.find(jsonText)?.groupValues?.get(1) ?: rawSpokenText

                Pair(action, target)
            } catch (e: Exception) {
                // Fallback to a basic click command if the network fails or API throws an exception
                Pair("ACTION_CLICK", rawSpokenText)
            }
        }
    }
}