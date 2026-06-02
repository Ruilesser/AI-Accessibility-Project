
---

# AI-Accessibility-Project

An open-source Android Accessibility app designed to help individuals manage daily tasks hands-free not provided by Gemini. By bridging **Gemini's voice command layer** with a custom background **Android Accessibility Service**, this system converts spoken commands into screen actions, providing a bridge for users with limited mobility.

---

## Project Scope & Task Inventory

### Core Automation Infrastructure

* **System-Wide Intercepts:** Automated UI scanning across active system windows and multi-layered applications.
* **Eyes-Free Feedback:** Dedicated Text-to-Speech (TTS) engine that forces all confirmation responses.

* **Hands-Free Activation:** Eliminates the need to physically press or hold a "Record Speech" button by leveraging continuous "Hey Google" ambient wake-word integration.

* **Identity & Authentication:** Deep window indexing capable of overriding system isolation blocks to locate and select "Autofill", "Sign In", or "Continue" credential prompts.

* **Voice Messages:** Hands-free setup of outgoing text-to-speech voice messages.

### Supported Tasks & Target Ecosystems

* **Media & Entertainment:** * **YouTube Navigation:** Multi-step macro to automatically launch the native app, navigate past layout walls (the "You" profile page or expanded "Playlists" menus), and surface the **Watch Later** queue.

* **Food Delivery & Shopping:**
* Contextual intent monitoring and button targeting for **SkipTheDishes**, **UberEats**, and **Amazon Shopping** checkout pages.


### Provided by Gemini default:
* **Spotify Integration:** Specialized remote routing targeting an Android tablet acting as a dedicated music player.

* **Television:** Hands-free surfing of TV channels.


* **Scheduling & Productivity:**
* **Calendar Management:** Conversational scheduling engine triggered via sequential voice tokens (*"Add to calendar"* followed by event parameters).

* **Proactive Reminders:** Automated alerting layers configured for 1 week, 1 day, day-of, and 15-minute intervals.

* **ETA Coordination:** Dynamic lookup of current calendar status paired with automated message generation to broadcast ETAs.



---

## Technical Setup & Deployment Guide

Follow these steps to build the custom bridge application (`VoiceAssistantBridge`) and deploy it directly onto your physical testing device.

### Prerequisites
* Android Studio Jellyfish (or newer) installed on your machine.
* A physical Android smartphone running **Android 8.0 (API level 26)** or higher.
* A high-quality USB data cable.

### Step 1: Prepare the Device for Deployment
To install your custom background service, you must unlock developer privileges on your physical phone:
1. Open your phone's **Settings** and navigate to **About Phone**.
2. Find the **Build Number** row and tap it **7 times** until a popup says *"You are now a developer!"*
3. Go back to the main Settings menu, find **System > Developer Options**, and switch on **USB Debugging**.

### Step 2: Compile and Build the APK
1. Connect your phone to your computer via USB. If prompted on the phone screen, select **Allow USB Debugging**.
2. Open your project folder inside **Android Studio**.
3. Look at the top toolbar and verify your physical device's name is selected in the deployment dropdown menu.
4. Click the green **Run button (Play icon)** or press `Shift + F10`.
5. Android Studio will compile the Kotlin source code, package it, and push the file directly onto your phone.

### Step 3: Grant System Accessibility Authorization
Because this application acts directly on the interface, Android safely isolates its execution until you explicitly authorize it:
1. On your phone, open **Settings** and search for **Accessibility**.
2. Scroll down to the **Downloaded Services** or **Installed Apps** sub-section.
3. Select **Voice Automation Service** (labeled by your project framework).
4. Turn the toggle **ON**. Agree to the system warning dialog.

---

## Gemini Voice Automations Configuration (Via App Actions)

This project uses Android’s native **App Actions** ecosystem (`shortcuts.xml`). When Gemini or Google Assistant intercepts an explicit voice command referencing your app, it extracts the target phrasing, binds it to an internal static capability, and passes it directly down to your local application architecture.

### Step 1: Install the Local Voice Bridge Synchronizer
To make Gemini on your physical phone index your local code parameters, you need to use the official development assistant tool:
1. In Android Studio, go to **File > Settings** (or *Android Studio > Preferences* on macOS).
2. Select **Plugins**, search the **Marketplace** for `Google Assistant`, install it, and restart your IDE.
3. **Account Alignment (Critical):** Ensure that the **exact same Google Account** is signed into:
   * Your Android Studio IDE profile.
   * The primary profile on your physical test phone.
   * The primary Google App / Assistant voice-matching engine on the handset.

### Step 2: Push the Build and Generate the Cloud Preview
1. Connect your device via USB data cable, ensure USB Debugging is active, and click the green **Run (Play button)** to install the latest APK.
2. In your phone's **Settings > Accessibility**, find your app name and toggle the master permission to **ON**.
3. In Android Studio's top navigation bar, go to **Tools > App Actions > Google Assistant > App Actions Test Tool**.
4. In the side panel that appears, type your desired vocally spoken application name into the **App name** box (e.g., `Voice Assistant` or `Voice Bridge`).
5. Click **Create Preview**. This registers your temporary local voice profile to the cloud securely.

---

## 📱 Operational Vocabulary Matrix

Once the App Actions preview is initialized, you can use any of these natural phrase sequences directly into your phone by waking up your assistant device (*"Hey Gemini"* or *"Hey Google"*). Gemini will automatically forward the request directly into your background service.

| Intent Context | Voice Phrase to Speak | System Execution Result |
| :--- | :--- | :--- |
| **System Check** | *"Hey Gemini, search for **test** on Voice Assistant"* | Fires `VoiceProxyActivity`, triggers the internal `RUN_TEST` broadcast, and loops a verbal confirmation out of the phone speaker. |
| **YouTube Playlist** | *"Hey Google, search for **watch later** on Voice Assistant"* | Forces open the native YouTube app, maps layout visibility states, and executes the multi-stage navigation sweep logic. |
| **Universal Clicker** | *"Hey Gemini, search for **click Autofill** on Voice Assistant"* | Strips out control verbs, captures the core keyword text ("Autofill"), and attempts to pass a click action to the target node. |
| **Form Navigation** | *"Hey Google, search for **click Continue** on Voice Assistant"* | Traverses parent layers of the target visual layout boundary elements to trigger unclickable raw confirmation strings. |

*(Note: Whenever you make structural adjustments to the `shortcuts.xml` layout matrix, remember to click the **Update** button inside the Android Studio side panel tool to sync changes down to your test device).*

---

## Project Directory Architecture

For reference, verify files match this folder structure:

```text
AI-Accessibility-Project/
│
├── app/
│   ├── src/main/
│   │   ├── java/com/example/voiceassistantbridge/
│   │   │   ├── VoiceAutomationService.kt  <-- Core UI Scanner & Driver Engine
│   │   │   └── VoiceCommandReceiver.kt   <-- System Intent Radio Interceptor
│   │   │
│   │   └── res/
│   │       ├── xml/
│   │       │   └── accessibility_service_config.xml <-- System Flags Window Configuration
│   │       └── layout/
│   │
│   └── build.gradle.kts
└── README.md

```