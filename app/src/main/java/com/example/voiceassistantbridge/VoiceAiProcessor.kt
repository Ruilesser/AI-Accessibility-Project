package com.example.voiceassistantbridge

import android.util.Log
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
                responseMimeType = "application/json"
            },
            systemInstruction = content {
                text("You are an Android accessibility routing engine. " +
                        "Analyze the user's spoken request and map it to exactly one of these actions:\n" +
                        "1. ACTION_TEST - if they mention testing, diagnostics, checking if things work.\n" +
                        "2. ACTION_YOUTUBE_LATER - if they want to see, watch, open, or look at their watch later playlist/queue on YouTube.\n" +
                        "3. ACTION_CLICK - if they want to physically click or tap a specific visible button/text on screen.\n\n" +
                        "CRITICAL RULES:\n" +
                        "- Ignore conversational filler greetings like 'hey', 'hi', 'hello', 'can you please'. Do not treat them as targets.\n" +
                        "- For ACTION_CLICK, extract ONLY the exact, literal name of the button to click in the 'target' field.\n" +
                        "- For ACTION_TEST and ACTION_YOUTUBE_LATER, leave the 'target' field as an empty string (\"\").\n\n" +
                        "Your response must be a single, strict JSON object with fields 'action' and 'target'.")
            }
        )

    suspend fun processVoiceIntent(rawSpokenText: String): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "User said: \"$rawSpokenText\""
                val response = model.generateContent(prompt)

                var jsonText = response.text ?: ""
                jsonText = jsonText.replace("```json", "")
                jsonText = jsonText.replace("```", "").trim()

                Log.d("VoiceAiProcessor", "Raw JSON clean output from Gemini: $jsonText")

                val actionRegex = "\"action\"\\s*:\\s*\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
                val targetRegex = "\"target\"\\s*:\\s*\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)

                val actionMatch = actionRegex.find(jsonText)?.groupValues?.get(1)?.trim()
                val targetMatch = targetRegex.find(jsonText)?.groupValues?.get(1)?.trim()

                val finalAction = actionMatch ?: "ACTION_CLICK"
                val finalTarget = targetMatch ?: rawSpokenText

                Pair(finalAction, finalTarget)
            } catch (e: Exception) {
                Log.e("VoiceAiProcessor", "AI Engine network processing failure! Using local fallback regex.", e)

                // LOCAL FALLBACK: If the network or Firebase fails, clean up the text manually so it doesn't break
                val cleanedFallbackTarget = rawSpokenText
                    .replace("hey", "", ignoreCase = true)
                    .replace("hi", "", ignoreCase = true)
                    .replace("hello", "", ignoreCase = true)
                    .replace("can you please", "", ignoreCase = true)
                    .replace("click", "", ignoreCase = true)
                    .replace("tap", "", ignoreCase = true)
                    .trim()

                // If the user's intent was obviously a macro, handle it locally too
                val finalAction = when {
                    rawSpokenText.contains("test", ignoreCase = true) || rawSpokenText.contains("diagnostics", ignoreCase = true) -> "ACTION_TEST"
                    rawSpokenText.contains("watch later", ignoreCase = true) -> "ACTION_YOUTUBE_LATER"
                    else -> "ACTION_CLICK"
                }

                Pair(finalAction, cleanedFallbackTarget)
            }
        }
    }
}