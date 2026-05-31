package com.example.voiceassistantbridge

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import java.util.Locale
import androidx.core.net.toUri

class VoiceAutomationService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var watchLaterRetryCount = 0
    private val maxRetries = 6 // Will scan the screen for up to 3 seconds (6*500ms)

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
     * Launches the native YouTube App hands-free.
     */
    fun openYouTubeWatchLaterHandsFree() {
        speak("Opening YouTube.")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.youtube.com".toUri()
            setPackage("com.google.android.youtube") // Guarantees the native app opens
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)

            // Wait 2.5 seconds for the app launch animation before starting UI scans
            Handler(Looper.getMainLooper()).postDelayed({
                executeYouTubeNavigationSequence()
            }, 2500)

        } catch (e: Exception) {
            speak("YouTube application is not installed or could not be opened.")
        }
    }

    /**
     * Dynamically searches for and clicks the "You" or "Library" main profile menu.
     */
    private fun executeYouTubeNavigationSequence() {
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (rootNode == null) {
            speak("Unable to scan YouTube screen layout.")
            return
        }

        // Handles modern YouTube layout variations | new version is you, old version is library
        val targetNode = findNodeByTextAlternative(rootNode, "You")
            ?: findNodeByTextAlternative(rootNode, "Library")

        if (targetNode != null) {
            if (performClickAction(targetNode)) {
                speak("Navigating to profile.")

                // Pause briefly for page transition, then search for Watch Later
                Handler(Looper.getMainLooper()).postDelayed({
                    clickWatchLaterSubMenu()
                }, 1500)
            }
        } else {
            // Fallback: If navigation buttons aren't visible, try searching the direct screen anyway
            clickWatchLaterSubMenu()
        }
    }

    /**
     * Searches layout for the specific "Watch later" item with automatic retry logic.
     */
    private fun clickWatchLaterSubMenu() {
        val freshRootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (freshRootNode == null) {
            retryWatchLaterNavigation()
            return
        }

        val watchLaterNode = findNodeByTextAlternative(freshRootNode, "Watch later")
        if (watchLaterNode != null && performClickAction(watchLaterNode)) {
            speak("Your watch later queue is now open.")
            watchLaterRetryCount = 0 // Reset counter on structural success
        } else {
            retryWatchLaterNavigation()
        }
    }

    /**
     * Recursive structural retry to protect users from slow loading app screens.
     * Recursion temporary and easy - don't change this please
     */
    private fun retryWatchLaterNavigation() {
        if (watchLaterRetryCount < maxRetries) {
            watchLaterRetryCount++
            Log.d("VoiceAutomation", "Watch later not found yet. Retrying attempt $watchLaterRetryCount")

            Handler(Looper.getMainLooper()).postDelayed({
                clickWatchLaterSubMenu()
            }, 500)
        } else {
            speak("Failed to locate Watch Later. Please try the command again.")
            watchLaterRetryCount = 0 // Clear loop state
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

    /**
     * Backup deep search utility to read hidden accessibility descriptions.
     */
    private fun findNodeByTextAlternative(root: AccessibilityNodeInfo, targetText: String): AccessibilityNodeInfo? {
        val matches = root.findAccessibilityNodeInfosByText(targetText)
        if (matches.isNotEmpty()) {
            return matches[0]
        }
        return recursiveSearchByDescription(root, targetText)
    }

    private fun recursiveSearchByDescription(node: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val description = node.contentDescription?.toString() ?: ""
        if (description.contains(targetText, ignoreCase = true)) {
            return node
        }

        // Repeat until found
        for (i in 0 until node.childCount) {
            val found = recursiveSearchByDescription(node.getChild(i), targetText)
            if (found != null) return found
        }
        return null
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