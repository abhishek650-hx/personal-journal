# 📔 Personal Journal – AI-Powered Android Journaling App

**Personal Journal** is a modern, AI-powered Android application that helps you capture daily thoughts, preserve memories, and reflect on your everyday experiences.

Built using **Kotlin, Jetpack Compose, Room Database, and Cloudflare Workers AI**, the application combines voice-based journaling, organized daily records, AI-generated summaries, and text-to-speech functionality.

Your personal diary, always with you.

---

## 📲 Download the App

<escape>**</escape>Want to use Personal Journal? You don't need Android Studio or programming knowledge!<escape>**</escape>

### <escape>⬇️</escape> [Download Personal Journal APK](https://github.com/abhishek650-hx/personal-journal/raw/refs/heads/main/app-debug.apk)

Download the APK directly to your Android phone and install it.

**Installation instructions:**

1. Open the download link above on your Android phone.
2. Download `app-debug.apk`.
3. Open the downloaded APK.
4. If prompted, allow installation from this source in Android settings.
5. Tap **Install**.
6. Open Personal Journal and start recording your thoughts!

<escape>**</escape>Note: This is currently a debug APK. Android may display a warning when installing an app downloaded outside the Play Store. Only install APKs from sources you trust.<escape>**</escape>

---

## ✨ Features

### 🎙️ 1. Voice-Based Journaling
- Record your thoughts using Android speech recognition.
- Automatically convert spoken words into text.
- Save journal entries without manually typing everything.
- Capture your thoughts whenever you want.

### 🏠 2. Daily Journal Dashboard
- View all journal entries recorded today.
- Keep daily thoughts organized in one place.
- Delete individual journal entries.
- Access your daily journal conveniently.

### 📅 3. Previous Records
- Browse historical journal entries through date-based cards.
- View records organized by date.
- Select a date to see all entries recorded on that day.
- Delete all journal entries belonging to a selected date.

### 🤖 4. AI-Powered Daily Summaries
- Generate summaries of your daily journal entries.
- Convert multiple thoughts into a readable daily overview.
- Uses Cloudflare Workers AI.
- Powered by the `@cf/meta/llama-3.2-1b-instruct` model.

### 🔊 5. Text-to-Speech
- Listen to AI-generated journal summaries.
- Uses Android's built-in TextToSpeech engine.
- Listen to your daily reflections instead of reading them.

### 🎨 6. Modern User Interface
- Built using Jetpack Compose.
- Clean and minimal light-themed design.
- Custom application icon.
- Simple and convenient navigation.

---

## 🔄 How It Works

1. Open Personal Journal.
2. Record your thoughts using voice input.
3. Save your journal entry.
4. View your entries on the daily dashboard.
5. Visit Previous Records to revisit earlier memories.
6. Generate an AI summary of today's journal entries.
7. Listen to your summary using text-to-speech.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | Modern user interface |
| Room Database | Local journal storage |
| Kotlin Coroutines | Asynchronous operations |
| OkHttp | API communication |
| Cloudflare Workers AI | AI-generated summaries |
| Android Speech Recognition | Voice-to-text |
| Android TextToSpeech | Text-to-voice |
| Gradle | Build automation |

---

## 📋 Requirements

For users:
- Android smartphone.
- Internet connection for AI summaries.

For developers:
- Android Studio.
- JDK 17.
- Android SDK compatible with the project.
- Android emulator or physical Android device.

---

## 💻 Installation for Developers

Clone the repository:

```bash
git clone https://github.com/abhishek650-hx/personal-journal.git
```

Follow these steps:

1. Open Android Studio.
2. Select **Open** and choose the cloned project folder.
3. Wait for Gradle synchronization to complete.
4. Connect an Android device or start an emulator.
5. Run the application.

### Generate APK

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On Linux or macOS:

```bash
./gradlew assembleDebug
```

The generated APK will be available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## ☁️ Cloudflare AI Configuration

Personal Journal uses Cloudflare Workers AI to generate daily journal summaries.

### Setup Instructions

1. Create a Cloudflare account.
2. Obtain your Cloudflare Account ID.
3. Create an API token with the required Workers AI permissions.
4. Install and open Personal Journal.
5. Navigate to Settings.
6. Enter your Cloudflare Account ID and API token.
7. Save the configuration.
8. Tap **Summarise today** to generate your daily summary.

**AI Model:**

```text
@cf/meta/llama-3.2-1b-instruct
```

### Security Note

API credentials are stored in app-private SharedPreferences on the device. This is suitable for a personal prototype, but client-side credentials can potentially be extracted.

For production deployment, a secure backend should handle API requests and protect credentials.

Never upload real API tokens, passwords, signing keys, or private configuration files to GitHub.

---

## 🔐 Privacy

- Journal entries are stored locally using Room Database.
- Entries are not automatically uploaded to a cloud database.
- Journal content is sent to Cloudflare when an AI summary is requested.
- Voice recognition depends on the Android speech recognition service.
- Cloudflare AI processing requires an internet connection.

---

## 📂 Project Structure

```text
PersonalJournal/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/personaljournal/
│           │   ├── data/
│           │   ├── voice/
│           │   └── MainActivity.kt
│           │
│           ├── res/
│           └── AndroidManifest.xml
│
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
└── app-debug.apk
```

---

## 🚀 Future Improvements

- Journal search functionality.
- Mood tracking and analysis.
- Weekly and monthly AI summaries.
- Journal backup and restore.
- Additional voice options.
- Improved privacy and secure backend integration.
- More personalization options.

---

## 📜 License

This project is intended to be released under the MIT License. See the `LICENSE` file for details.

---

## 👨‍💻 Project

**Personal Journal – Your Thoughts. Your Memories. Your Story.**

Developed with Kotlin, Jetpack Compose, and AI.

**GitHub Repository:**  
https://github.com/abhishek650-hx/personal-journal

**Download Application:**  
https://github.com/abhishek650-hx/personal-journal/raw/refs/heads/main/app-debug.apk
