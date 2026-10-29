# StudyHub

**Organize. Collaborate. Achieve. Together.**

StudyHub is an Android study-group organiser. Students can create groups, keep track of tasks and due dates, schedule study sessions, and chat with their group.

> Module: OPSC6321 — Part 2, App Prototype Development

## UI implementation

The app UI is built with **Kotlin and Android XML layouts** using the Android View system. The screen layouts live in `app/src/main/res/layout/`; Kotlin Fragments bind them with View Binding. `MainActivity` hosts the screens and the bottom navigation. **Jetpack Compose is not used by the app module.**

The existing lilac visual direction is kept in XML resources and Material Components: `#E3C6E8` background, `#C77DE8` primary accent, rounded cards, and dark-mode colour resources.

## Screens

- Welcome, login, and registration, including field validation, password-strength feedback, password reset, and Google sign-in.
- Home dashboard with a daily quote and upcoming tasks/sessions.
- Study groups with search and group chat.
- Tasks with due dates, priority, completion, and delete actions.
- Calendar agenda for tasks and sessions.
- Study sessions with scheduling and attendance.
- Profile editing, sign-out, and saved appearance/offline/data/notification preferences.

## Architecture

```text
XML layouts + Fragments
        ↓ events and StateFlow
AuthViewModel · MainViewModel · SettingsViewModel
        ↓
AuthRepository · StudyHubRepository · ApiRepository
        ↓
Firebase Auth / Firestore · Room · DataStore · Retrofit API
```

Firestore is configured with a persistent cache. Room also stores local copies of the main study data, while DataStore backs the user preferences.

## Project layout

```text
StudyHub/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/main/
│       ├── java/com/studyhub/app/
│       │   ├── data/          # models, local/remote data, repositories
│       │   ├── ui/            # XML-backed Fragments and navigation
│       │   ├── util/          # validation, date and password helpers
│       │   ├── viewmodel/
│       │   ├── MainActivity.kt
│       │   └── StudyHubApp.kt
│       └── res/layout/        # activity, screen, row and dialog XML
├── gradle/                    # version catalog and Gradle wrapper
├── server.js                  # Express + Firebase Admin REST API
└── package.json
```

## Build the Android app

Requirements: Android Studio, JDK 17, and Android SDK 34.

```bash
./gradlew :app:assembleDebug
```

For a different Firebase project, replace `app/google-services.json`, enable Email/Password and Google Authentication, and configure Firestore. The app's package name is `com.studyhub.app`.

The Google web client ID is set in `app/build.gradle.kts` as `GOOGLE_WEB_CLIENT_ID`. Configure it for your Firebase project before using Google sign-in.

## REST API

`server.js` is a small Express service backed by Firebase Admin. It provides:

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/quote/today` | Daily motivational quote |
| GET | `/api/resources` | List resources, optionally filtered by subject |
| POST | `/api/resources` | Add a resource |
| PUT | `/api/resources/:id` | Update a resource |
| DELETE | `/api/resources/:id` | Delete a resource |

Run it with Node.js 18 or later:

```bash
npm install
FIREBASE_SERVICE_ACCOUNT='{"project_id":"..."}' npm start
```

Set `API_BASE_URL` in the app module to the deployed API's base URL (including a trailing slash). It currently defaults to `https://example.com/`; the home screen uses a local quote if the API cannot be reached.

## Tests

The project includes starter JVM and instrumented Android tests. Run them with `./gradlew :app:testDebugUnitTest` and, with an emulator/device attached, `./gradlew :app:connectedDebugAndroidTest`.
