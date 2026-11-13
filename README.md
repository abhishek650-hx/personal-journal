# Personal Journal

A modern, AI-powered personal journaling Android application built with **Kotlin, Jetpack Compose, Room Database, and Cloudflare Workers AI**.

Personal Journal helps you capture daily thoughts through voice, organize memories by date, and generate AI-powered summaries of your daily activities.

## Features

- **Voice Journaling:** Record journal entries using Android speech recognition.
- **Local Storage:** Store journal entries securely on-device using Room Database.
- **Daily Dashboard:** View today's journal entries in one place.
- **Previous Records:** Browse historical entries through date-based cards.
- **Date-wise Organization:** Open a date to view all its journal entries.
- **Date-wise Deletion:** Delete all records belonging to a selected date.
- **AI Summaries:** Generate summaries of daily journal entries using Cloudflare Workers AI.
- **Text-to-Speech:** Listen to AI-generated summaries using Android's built-in speech engine.
- **Custom App Icon:** A clean, light-themed application identity.

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Application development |
| Jetpack Compose | User interface |
| Room Database | Local data storage |
| Coroutines | Asynchronous operations |
| OkHttp | API communication |
| Cloudflare Workers AI | AI-powered summaries |
| Android Speech Recognition | Voice input |
| Android TextToSpeech | Voice playback |

## Requirements

- Android Studio
- JDK 17
- Android SDK required by the project
- Android device or emulator
- Internet connection for AI summaries

## Installation

1. Clone this repository:

   ```bash
   git clone https://github.com/abhishek650-hx/personal-journal.git
   ```

2. Open Android Studio.
3. Select **Open** and choose the cloned `personal-journal` folder.
4. Allow Gradle synchronization to complete.
5. Connect an Android device or start an emulator.
6. Press **Run**.

## Cloudflare AI Setup

Personal Journal uses Cloudflare Workers AI to generate journal summaries.

1. Create a Cloudflare account.
2. Obtain your Cloudflare Account ID.
3. Create an API token with the required Workers AI permissions.
4. Open Personal Journal.
5. Open Settings and enter your Account ID and API token.
6. Save the settings.
7. Tap **Summarise today** to generate a summary.

The application uses the model:

`@cf/meta/llama-3.2-1b-instruct`

**Security note:** API credentials are stored in app-private SharedPreferences on the device. This is suitable for a personal prototype, but client-side credentials can be extracted. For production deployment, use a secure backend to protect API credentials.

Never commit real API tokens, passwords, signing keys, or private configuration files to GitHub.

## Project Structure

```text
PersonalJournal/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/personaljournal/
│           │   ├── data/
│           │   ├── voice/
│           │   └── MainActivity.kt
│           ├── res/
│           └── AndroidManifest.xml
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Privacy

- Journal entries are stored locally in the Room database.
- Entries are not automatically uploaded to a cloud database.
- Today's entries are sent to Cloudflare only when an AI summary is requested.
- Voice recognition depends on the Android speech recognition service.

## License

This project is released under the MIT License. See the `LICENSE` file for details.
