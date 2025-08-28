# 📸🧠✍️ AI Notes Summarizer  

Turn scanned notes or pasted text into clean, concise summaries using **on-device OCR** and **Google Gemini AI**, then **save, tag, search, pin, and share** — all in a modern, smooth **Jetpack Compose UI**.  

---

## ✨ Features  

- 📷 **OCR Capture**: Scan text from paper or screens using **CameraX + ML Kit Text Recognition**.  
- 🤖 **AI Summaries**: Generate clear **bullet-point summaries** via **Google Gemini (gemini-1.5-flash)** with adjustable length.  
- 🗂️ **History & Organization**: Auto-saves summaries with timestamps; search by text or filter by tags.  
- 📌 **Pin & Tag**: Pin important summaries to the top and add comma-separated tags for quick grouping.  
- 🔄 **Share & Copy**: One-tap share or copy summarized content for reuse anywhere.  
- 🎨 **Modern UI**: Material 3 + Jetpack Compose with dark mode and edge-to-edge polish.  

---

## 🛠 Tech Stack  

- **Language** → Kotlin  
- **UI** → Jetpack Compose (Material 3, animations, icons-extended)  
- **Architecture** → MVVM + StateFlow + Coroutines  
- **AI** → Google Gemini API (`gemini-1.5-flash`)  
- **OCR** → CameraX + ML Kit Text Recognition (Latin)  
- **Database** → Room (with Migration v1→v2 for pinned & tags)  
- **Navigation** → Accompanist Navigation Animation  
- **Permissions** → Accompanist Permissions  
- **Other** → Hilt DI (if used), Gradle KSP, BuildConfig API key injection  

---
Clone Repository  
git clone https://github.com/yourname/AI-Note-Summarizer.git
