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
import kotlin.system.exitProcess

class VoiceAutomationService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var commandReceiver: VoiceCommandReceiver
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var watchLaterRetryCount = 0
    private val maxRetries = 6 // Will scan the screen for up to 3 seconds (6*500ms)

    override fun onCreate() {
        super.onCreate()
        // Initialize TTS so the app can talk back eyes-free
        tts = TextToSpeech(this, this)

        // Initialize and register the radio receiver
        commandReceiver = VoiceCommandReceiver(this)
        val filter = android.content.IntentFilter().apply {
            addAction(VoiceCommandReceiver.ACTION_OPEN_WATCH_LATER)
            addAction(VoiceCommandReceiver.ACTION_CLICK_TEXT)
            addAction(VoiceCommandReceiver.ACTION_RUN_TEST)
        }
        registerReceiver(commandReceiver, filter, RECEIVER_EXPORTED)

        // GLOBAL FAILSAFE: Intercepts all crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("VoiceAutomationFailsafe", "CRITICAL CRASH DETECTED on thread ${thread.name}", throwable)

            // Build an intent to trigger a fresh reboot of your service
            val restartIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            if (restartIntent != null) {
                startActivity(restartIntent)
            }

            // Terminate the broken process
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isTtsReady = true
        }
    }

    // Triggers out loud whenever the app needs to announce an action
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
     * Simple test function
     * Verifies that Gemini voice triggers are executing and communicating with the app
     */
    fun executeSystemDiagnosticsTest() {
        Log.d("VoiceAutomationTest", "Diagnostics triggered successfully via Intent broadcast.")

        // Immediate spoken verification out of the phone speaker
        speak("Testing the voice assistant bridge.")
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

            // Wait 2.75 seconds for the app launch animation before starting UI scans
            Handler(Looper.getMainLooper()).postDelayed({
                executeYouTubeNavigationSequence()
            }, 2750)

        } catch (e: Exception) {
            speak("YouTube application is not installed or could not be opened.")
        }
    }

    /**
     * Dynamically searches for and clicks the "You" or "Library" main profile menu.
     * Patched to bypass the top-left "YouTube" branding header text match.
     */
    private fun executeYouTubeNavigationSequence() {
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (rootNode == null) {
            speak("Unable to scan YouTube screen layout.")
            return
        }

        // Check if "Watch later" is already visible (e.g., if YouTube was already open)
        val directWatchLater = findNodeByTextAlternative(rootNode, "Watch later")
        if (directWatchLater != null && performClickAction(directWatchLater)) {
            speak("Opening your watch later queue directly.")
            return
        }

        // TRY REFINED SEARCH: Look for a node that has "You" or "Library"
        var targetNode = findBottomTabNode(rootNode, "You")
            ?: findBottomTabNode(rootNode, "Library")

        // FALLBACK SEARCH: If the refined navigation scanner misses it, use the original text alternative helper.
        if (targetNode == null) {
            targetNode = findNodeByTextAlternative(rootNode, "You")
                ?: findNodeByTextAlternative(rootNode, "Library")
        }

        if (targetNode != null && performClickAction(targetNode)) {
            speak("Navigating to profile.")
            watchLaterRetryCount = 0

            Handler(Looper.getMainLooper()).postDelayed({
                clickWatchLaterSubMenu()
            }, 1750)
        } else {
            clickWatchLaterSubMenu()
        }
    }

    /**
     * Helper to locate bottom bar tabs by verifying they are actually clickable
     * or marked as selection items, ignoring top-left text branding and contextual tabs.
     */
    private fun findBottomTabNode(root: AccessibilityNodeInfo, targetText: String): AccessibilityNodeInfo? {
        val matches = root.findAccessibilityNodeInfosByText(targetText)
        for (node in matches) {
            // Filter 1: Ensure the node is usable
            if (node.isClickable || (node.parent != null && node.parent.isClickable)) {

                val contentDesc = node.contentDescription?.toString() ?: ""
                val textStr = node.text?.toString() ?: ""

                // Filter 2: ignore top-left branding headers
                if (contentDesc.contains("YouTube", ignoreCase = true) ||
                    textStr.equals("YouTube", ignoreCase = true)) {
                    continue // Skip to the next match
                }

                // Filter 3: ignore the "New to you"
                if (contentDesc.contains("New to you", ignoreCase = true) ||
                    textStr.contains("New to you", ignoreCase = true)) {
                    continue // Skip to the next match
                }

                return node
            }
        }
        return null
    }

    /**
     * Searches layout for the specific "Watch later" item with automatic retry logic.
     * If it fails after retrying, expands search into Playlists sub-menu
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
            watchLaterRetryCount = 0 // Clear loop counter when success
        } else {
            retryWatchLaterNavigation()
        }
    }

    /**
     * Manages retries for finding the "Watch later" button directly.
     * If it exhausts all retries, it triggers the secondary fallback to open Playlists.
     */
    private fun retryWatchLaterNavigation() {
        if (watchLaterRetryCount < maxRetries) {
            watchLaterRetryCount++
            Log.d("VoiceAutomation", "Watch later not found yet. Retrying attempt $watchLaterRetryCount")

            Handler(Looper.getMainLooper()).postDelayed({
                clickWatchLaterSubMenu()
            }, 750)
        } else {
            // FALLBACK: "Watch later" isn't visible on the screen.
            // Find the Playlists or "See all" button
            Log.d("VoiceAutomation", "Watch later not visible. Attempting to expand Playlists menu.")
            watchLaterRetryCount = 0 // Reset counter for the next phase
            navigateToPlaylistsSection()
        }
    }

    /**
     * Finds and clicks "Playlists" or "See all" to expand hidden lists.
     */
    private fun navigateToPlaylistsSection() {
        val freshRootNode: AccessibilityNodeInfo = rootInActiveWindow ?: return

        // freshRootNode is guaranteed non-null
        val expandNode = findNodeByTextAlternative(freshRootNode, "Playlists")
            ?: findNodeByTextAlternative(freshRootNode, "See all")

        if (expandNode != null && performClickAction(expandNode)) {
            speak("Expanding playlists views.")

            // Give the sub-menu 1.75 seconds to open, then run final sweep
            Handler(Looper.getMainLooper()).postDelayed({
                lookForWatchLaterInPlaylists()
            }, 1750)
        } else {
            speak("Failed to find Watch Later or Playlists menu.")
        }
    }

    /**
     * Step 5 (Final Scan): Final sweep inside the expanded playlist window.
     */
    private fun lookForWatchLaterInPlaylists() {
        val finalRootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (finalRootNode == null) {
            speak("Screen content lost during playlist expansion.")
            return
        }

        val watchLaterNode = findNodeByTextAlternative(finalRootNode, "Watch later")
        if (watchLaterNode != null && performClickAction(watchLaterNode)) {
            speak("Your watch later queue is now open.")
        } else {
            speak("Could not find your Watch Later list anywhere on this screen.")
        }
    }

    /**
     * VERY IMPORTANT
     * Processes macro routing variables, then falls back to searching
     * interactive windows if a structural command string is absent.
     */
    fun findAndClickButtonByText(targetText: String) {
        // Clean up text boundaries
        val query = targetText.trim()
        
        if (query.contains("test", ignoreCase = true) || query.contains("diagnostics", ignoreCase = true)) {
            executeSystemDiagnosticsTest()
            return
        }

        if (query.contains("later", ignoreCase = true) || query.contains("watch later", ignoreCase = true)) {
            openYouTubeWatchLaterHandsFree()
            return
        }
        
        val windows = windows
        if (windows.isEmpty()) {
            val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
            if (rootNode != null) {
                searchAndClickInNode(rootNode, query)
            } else {
                speak("Screen content is not available.")
            }
            return
        }

        var clickedSuccessfully = false

        for (window in windows) {
            val rootNode = window.root
            if (rootNode != null) {
                if (searchAndClickInNode(rootNode, query)) {
                    clickedSuccessfully = true
                    break
                }
            }
        }

        if (!clickedSuccessfully) {
            speak("Could not find a button labeled $query on this screen.")
        }
    }

    // Helper method to break out the clicking logic
    private fun searchAndClickInNode(rootNode: AccessibilityNodeInfo, targetText: String): Boolean {
        val list: List<AccessibilityNodeInfo> = rootNode.findAccessibilityNodeInfosByText(targetText)
        if (list.isNotEmpty()) {
            for (node in list) {
                if (performClickAction(node)) {
                    speak("Successfully clicked $targetText.")
                    return true
                }
            }
        }
        return false
    }

    // Function to do the click
    // Added safetyDepth precaution so infinite loop is addressed
    private fun performClickAction(node: AccessibilityNodeInfo?): Boolean {
        var currentNode = node ?: return false
        var safetyDepthCounter = 0
        val maxLayoutDepth = 25 // Android layouts rarely exceed 15-20 layers deep

        while (currentNode != null && safetyDepthCounter < maxLayoutDepth) {
            if (currentNode.isClickable) {
                // Execute the physical virtual click
                currentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            currentNode = currentNode.parent
            safetyDepthCounter++ // Increment every time we climb up a layer
        }

        // If we hit null OR exceeded 25 layers, safely give up
        Log.w("VoiceAutomation", "Click failed: Reached root or hit safety depth limit.")
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
        // Unregister so it doesn't leak memory when turned off
        unregisterReceiver(commandReceiver)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}