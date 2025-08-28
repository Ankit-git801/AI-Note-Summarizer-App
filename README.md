# 📸🧠✍️ AI Notes Summarizer

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blueviolet.svg) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-brightgreen.svg)

Turn scanned notes or pasted text into clean, concise summaries using **on-device OCR** and **Google Gemini AI**, then **save, tag, search, pin, and share**—all in a modern, smooth **Jetpack Compose UI**.

---

## ✨ Features

-   📷 **OCR Capture**: Scan text directly from paper or screens using a live camera preview powered by **CameraX** and **ML Kit Text Recognition**.
-   🤖 **AI Summaries**: Generate clear, bullet-point summaries with **Google's Gemini 1.5 Flash model**. The summary length is easily adjustable with a simple slider.
-   🗂️ **Robust History & Organization**: Automatically saves all summaries with timestamps. Features a powerful search that queries original text, summaries, and tags simultaneously.
-   📌 **Pin & Tag**: Pin your most important summaries to the top of the history list for quick access. Organize notes by adding comma-separated tags, which can be used as filters.
-   🔄 **Edit, Share & Copy**: Easily edit generated summaries and tags. Share or copy your notes with a single tap from both the result and detail screens.
-   🎨 **Modern UI**: A polished user experience built with **Material 3** and **Jetpack Compose**, featuring smooth animations, a dark mode theme, and an edge-to-edge display.

---

## 🛠 Tech Stack

-   **Language** → **Kotlin**
-   **UI** → **Jetpack Compose** (Material 3, animations, icons-extended)
-   **Architecture** → **MVVM** with a ViewModel, StateFlow, and Coroutines for managing state and asynchronous operations.
-   **AI** → **Google Gemini API** (`gemini-1.5-flash`) for summarization.
-   **OCR** → **CameraX** for camera management and **ML Kit Text Recognition** (Latin script) for on-device text recognition.
-   **Database** → **Room** for local persistence, including a schema migration from version 1 to 2 to add `isPinned` and `tags` functionality.
-   **Navigation** → **Accompanist Navigation Animation** for animated screen transitions.
-   **Permissions** → **Accompanist Permissions** for handling the camera permission request gracefully.
-   **Build** → **Gradle KSP** for Room's annotation processing and **BuildConfig** for secure API key injection.

---

## 🚀 Quick Start

### 1. Clone the Repository
git clone https://github.com/your-username/AI-Note-Summarizer-App.git
cd AI-Note-Summarizer-App

### 2. Set Up the Gemini API Key

The app uses the **BuildConfig** method to securely handle the API key.

1.  Create a file named `local.properties` in the root directory of the project.
2.  Add your Gemini API key to this file as follows:

    ```
    GEMINI_API_KEY="YOUR_API_KEY_HERE"
    ```

3.  The `app/build.gradle.kts` file is already configured to read this value and make it available in the app's `BuildConfig`.

### 3. Open and Run the Project

1.  Open the project in a recent version of Android Studio.
2.  Let Gradle sync and download the required dependencies.
3.  Run the app on an Android device or emulator (API 26 or higher is recommended). The camera functionality works best on a physical device.

---

## 🔧 Project Details

### Permissions

The app requests the following permissions, which are declared in the `AndroidManifest.xml`:
-   `android.permission.CAMERA`: Required for the OCR feature. It is requested at runtime using Accompanist Permissions. The app is fully functional without it if you only use the paste-text feature.
-   `android.permission.INTERNET`: Required to make API calls to the Google Gemini service.

### Database Migration

The app includes a database migration from version 1 to 2, which adds the `isPinned` and `tags` columns to the `summaries` table. This is handled in `data/AppDatabase.kt` and ensures that users updating the app will not lose their existing data.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
