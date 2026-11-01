# StudyHub

**Organize. Collaborate. Achieve. Together.**

StudyHub is an Android study-group organiser for South African students. Create study groups, share resources, track tasks and deadlines, schedule sessions, and chat with group members. This release strengthens the prototype with offline-first task synchronization, account-aware push notifications, profile-photo cloud storage, and complete English/Afrikaans/isiZulu UI localization.

> **Module:** OPSC6321 — final StudyHub submission
>
> **Android application ID:** `com.studyhub.app`
>
> **Current app version:** `1.2.0` (version code 3)

## Features

- **Secure sign-in:** Firebase Email/Password authentication, password reset, validation, and Google sign-in through Android Credential Manager. Firebase Auth handles password hashing; the app never stores a raw password.
- **Study planning:** Home dashboard, groups and group chat, tasks with priorities/due dates, calendar agenda, sessions and attendance, profile editing, and preferences.
- **Three app languages:** English, Afrikaans (`af`), and isiZulu (`zu`). The selected language is persisted and applied with Android per-app language support. Screen labels, dialogs, validation messages, navigation, accessibility labels, notification text, quotes, and date/number formatting use localized resources.
- **Offline task outbox:** Task creation, completion, and deletion are saved to Room immediately. WorkManager waits for a network connection, checks the signed-in account and sync preference, and safely replays queued changes to Firestore. The Home screen shows the pending-sync count.
- **Group-message push notifications:** Firebase Cloud Messaging data messages are sent by the authenticated REST API only after it confirms the sender is a group member and owns the referenced message. Tokens are removed on sign-out where possible; the Android service also checks the active account and notification preference before displaying a push.
- **Profile-photo blob storage:** Students can upload an image from the Profile screen. Firebase Storage stores it under an owner-scoped path; uploads are limited to 5 MiB and image MIME types by both client and Storage rules.
- **REST API and database:** An Express API backed by Firebase Admin/Firestore serves localized daily quotes and educational resources. Resource create/edit/delete calls require a Firebase ID token and are restricted to the authenticated author.
- **Appearance and settings:** Dark mode, offline sync, data saver, notifications, and persisted per-app language preferences.
- **Professional baseline:** Material-based lilac visual system, app launcher assets, field validation, localized error states, automated unit/build checks, and a documented demo walkthrough.

## UI and architecture

The app UI is built with **Kotlin and Android XML layouts** using the Android View system and View Binding. Jetpack Compose is not used.

```text
XML layouts + Fragments
        ↓ events and StateFlow
AuthViewModel · MainViewModel · SettingsViewModel
        ↓
AuthRepository · StudyHubRepository · ApiRepository
        ↓
Firebase Auth / Firestore / Storage / FCM
Room task cache + outbox · DataStore preferences · WorkManager
Express REST API backed by Firebase Admin / Firestore
```

Firestore's Android SDK provides its own persistent cache. Room is the app's local source of truth for the task list and durable task outbox; DataStore stores user preferences. User-facing labels live in Android resources rather than being hard-coded in layouts.

### Important data flows

**Offline tasks**

1. A task change is written to Room and its outbox in a transaction, so it is immediately visible even with no connection.
2. Unique per-account WorkManager work waits for a connected network and an enabled sync preference.
3. The worker verifies that the same Firebase user is still signed in, replays queued operations by creation time, and removes an outbox item only after its Firestore write succeeds. Failed writes retry with backoff.

**Push notifications**

1. The user enables notifications in Profile and grants Android's notification permission where required.
2. The app registers its FCM token with `POST /api/notifications/register` using a Firebase ID token. The server stores the token under that user's `deviceTokens` subcollection.
3. After a Firestore chat message is written, the app calls `POST /api/notifications/group-message` with the group and message IDs.
4. The server verifies the Firebase ID token, group membership, and message ownership, then sends a data-only FCM message to the other members' devices. The app checks the target account and preference before showing localized notification text.

**Profile photos**

Photos are uploaded to `profile-photos/{uid}/profile-image`. The Firestore profile stores the Firebase Storage download URL. The bundled `storage.rules` restrict uploads to the authenticated owner and `image/*` files no larger than 5 MiB.

## Project layout

```text
StudyHub/
├── .github/workflows/android.yml  # Node syntax check, JVM tests, debug APK artifact
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json       # Firebase Android client configuration
│   └── src/main/
│       ├── java/com/studyhub/app/
│       │   ├── data/              # models, Room, Firebase, REST, sync worker
│       │   ├── notifications/     # FCM registration, service and display
│       │   ├── ui/                # XML-backed Fragments, dialogs and navigation
│       │   ├── util/              # validation, languages and date helpers
│       │   ├── viewmodel/
│       │   ├── MainActivity.kt
│       │   └── StudyHubApp.kt
│       └── res/                   # layouts, English/Afrikaans/isiZulu resources
├── docs/demo-script.md            # Suggested spoken demo walkthrough
├── firestore.rules                # Firebase database authorization rules
├── storage.rules                  # Firebase Storage authorization rules
├── firebase.json
├── server.js                      # Express + Firebase Admin REST API
└── package.json
```

## Build and test the Android app

Requirements: Android Studio, JDK 17, and Android SDK 34.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The current JVM tests cover supported-language normalization/resource labels and task-outbox payload round-tripping. With an emulator or device attached, run the instrumented suite with:

```bash
./gradlew :app:connectedDebugAndroidTest
```

The Android GitHub Actions workflow runs `node --check server.js`, the JVM unit tests, and the debug APK build on pushes, pull requests, and manual dispatches. A successful run publishes `studyhub-debug-apk` as a downloadable artifact for 14 days. The workflow does not publish a signed Play Store release.

## Firebase and Google configuration

The checked-in `app/google-services.json` and default Google web client ID belong to the current Firebase project. For another project:

1. Register an Android app with application ID `com.studyhub.app`, then replace `app/google-services.json` with that project's configuration.
2. Enable **Email/Password** and **Google** sign-in in Firebase Authentication. Configure the Android SHA-1/SHA-256 fingerprints required for Google sign-in.
3. Create the Firestore database, enable Firebase Storage and Cloud Messaging, and configure the project for Android 13+ notification delivery.
4. Deploy the repository's Firestore and Storage rules from the project root:

   ```bash
   firebase deploy --only firestore:rules,storage
   ```

5. Set project-specific Gradle properties in `~/.gradle/gradle.properties` (do not commit local credentials):

   ```properties
   studyhubApiBaseUrl=https://your-deployed-api.example/
   studyhubGoogleWebClientId=your-web-oauth-client-id.apps.googleusercontent.com
   ```

   The API base URL should include a trailing slash; Gradle adds it if omitted. The default is `https://example.com/`, so quote and push API calls remain best-effort until a real API URL is configured.
6. For a release, create a protected Android signing configuration, store signing material in CI secrets, and build/test the release variant before publishing. Never put a service-account private key or upload keystore in the Android APK or Git.

Firestore uses these collections: `users`, `groups` (with `messages` subcollections), `tasks`, `sessions`, `quotes`, `resources`, and the hashed `emailIndex`. The trusted API writes device tokens under `users/{uid}/deviceTokens`. The provided rules deny direct client access to token documents and protect user, group, task, and session writes.

## REST API

The API runs from `server.js` with Node.js 18 or later. It uses Firebase Admin credentials supplied by the hosting environment; no service-account key belongs in source control.

| Method | Path | Authentication | Purpose |
| --- | --- | --- | --- |
| `GET` | `/` | Public | Health/status response |
| `GET` | `/api/quote/today?lang=en\|af\|zu` | Public | Daily quote with localized Firestore fields or localized fallback |
| `GET` | `/api/resources?subject=...` | Public | List up to 50 resources, optionally filtered by subject |
| `POST` | `/api/resources` | Firebase ID token | Create a validated HTTP(S) resource as the caller |
| `PUT` | `/api/resources/:id` | Firebase ID token | Edit a resource owned by the caller |
| `DELETE` | `/api/resources/:id` | Firebase ID token | Delete a resource owned by the caller |
| `POST` | `/api/notifications/register` | Firebase ID token | Register the caller's device token |
| `POST` | `/api/notifications/unregister` | Firebase ID token | Remove the caller's device token on sign-out |
| `POST` | `/api/notifications/group-message` | Firebase ID token | Verify a sent group message and notify other members |

To run locally with Application Default Credentials:

```bash
npm install
export GOOGLE_APPLICATION_CREDENTIALS=/secure/path/firebase-service-account.json
npm start
```

Alternatively, provide `FIREBASE_SERVICE_ACCOUNT` as a JSON environment variable through your hosting provider's secret manager. Set `PORT` if required by the host. The Android browser client must use the deployed API's HTTPS URL; do not point a published app at localhost.

## Release notes and demo

Release notes for version 1.2.0 are in [`CHANGELOG.md`](CHANGELOG.md). [`docs/demo-script.md`](docs/demo-script.md) provides a short voice-over and screen-recording plan covering sign-in, language switching, an offline task/reconnect cycle, profile-photo storage, and a group-message notification. Record the actual demonstration only after the Firebase services and REST API are configured and deployed; no demo video is included in this repository.

## AI use

This repository was updated with AI coding assistance for resource localization, offline-sync and push-notification implementation, Firebase/API integration, tests, and documentation. AI-generated changes must be reviewed by the student against the project requirements and tested on the configured Firebase project and Android devices. No generative-AI service is called by the StudyHub app at runtime. See [`AI_USE.md`](AI_USE.md) for a short disclosure and verification checklist.
