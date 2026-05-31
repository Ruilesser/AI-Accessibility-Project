package com.example.voiceassistantbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class VoiceCommandReceiver(private val service: VoiceAutomationService) : BroadcastReceiver() {

    companion object {
        // These are the custom "frequencies" or Action strings we listen for
        const val ACTION_OPEN_WATCH_LATER = "com.example.voiceassistantbridge.OPEN_WATCH_LATER"
        const val ACTION_CLICK_TEXT = "com.example.voiceassistantbridge.CLICK_TEXT"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("VoiceCommandReceiver", "Received intent action: $action")

        when (action) {
            ACTION_OPEN_WATCH_LATER -> {
                // Call your YouTube function directly on the running service
                service.openYouTubeWatchLaterHandsFree()
            }
            ACTION_CLICK_TEXT -> {
                // Allows Gemini to pass arbitrary button names to your clicker tool
                val targetText = intent.getStringExtra("target_text") ?: ""
                if (targetText.isNotEmpty()) {
                    service.findAndClickButtonByText(targetText)
                }
            }
        }
    }
}