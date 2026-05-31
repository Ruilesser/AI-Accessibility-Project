package com.example.voiceassistantbridge

import android.accessibilityservice.AccessibilityService
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import java.util.Locale

class VoiceAutomationService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        // Initialize TTS so the app can talk back eyes-free
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isTtsReady = true
        }
    }

    // This triggers out loud whenever the app needs to announce an action
    private fun speak(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AutomationID")
        }
        Log.d("VoiceAutomation", "TTS Spoke: $text")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Detect when a user opens an app
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            Log.d("VoiceAutomation", "User switched to app: $packageName")
            
            // Automatically trigger an action when a specific app opens:
            if (packageName.contains("ubereats") || packageName.contains("skipthedishes")) {
                speak("Opened food delivery app. Looking for checkout targets.")
            }
        }
    }

    /**
     * Call this method when your custom voice command parser (or an incoming Intent) 
     * tells the app to click something specific on the screen.
     */
    fun findAndClickButtonByText(targetText: String) {
        // Get the root window node currently visible on the phone screen
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (rootNode == null) {
            speak("Screen content is not available.")
            return
        }

        // Search the UI layout tree for matching text (case-insensitive search)
        val list: List<AccessibilityNodeInfo> = 
            rootNode.findAccessibilityNodeInfosByText(targetText)

        if (list.isNotEmpty()) {
            for (node in list) {
                // Ensure the node or its parent container is actually clickable
                if (performClickAction(node)) {
                    speak("Successfully clicked $targetText.")
                    return
                }
            }
        } else {
            speak("Could not find a button labeled $targetText on this screen.")
        }
    }

    private fun performClickAction(node: AccessibilityNodeInfo?): Boolean {
        var currentNode = node
        while (currentNode != null) {
            if (currentNode.isClickable) {
                // Execute the physical virtual click
                currentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            // If the text node isn't clickable, check its parent layout block
            currentNode = currentNode.parent
        }
        return false
    }

    override fun onInterrupt() {
        Log.e("VoiceAutomation", "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}