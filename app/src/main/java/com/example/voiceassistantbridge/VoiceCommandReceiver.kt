package com.example.voiceassistantbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class VoiceCommandReceiver(private val service: VoiceAutomationService) : BroadcastReceiver() {

    companion object {
        // Common actions to be used
        const val ACTION_OPEN_WATCH_LATER = "com.example.voiceassistantbridge.OPEN_WATCH_LATER"
        const val ACTION_CLICK_TEXT = "com.example.voiceassistantbridge.CLICK_TEXT"
        const val ACTION_RUN_TEST = "com.example.voiceassistantbridge.RUN_TEST"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("VoiceCommandReceiver", "Received intent action: $action")

        when (action) {
            ACTION_OPEN_WATCH_LATER -> {
                service.openYouTubeWatchLaterHandsFree()
            }
            ACTION_CLICK_TEXT -> {
                val targetText = intent.getStringExtra("target_text") ?: ""
                if (targetText.isNotEmpty()) {
                    service.findAndClickButtonByText(targetText)
                }
            }
            // Run a quick test response
            ACTION_RUN_TEST -> {
                service.executeSystemDiagnosticsTest()
            }
        }
    }
}