# StudyHub

**Organize. Collaborate. Achieve. Together.**

StudyHub is a native Android app that helps students run study groups in one place. Students can form groups, assign tasks, schedule study sessions, chat, take attendance, run quick polls and share past exam papers. It works offline and syncs when the connection returns.

Built for **OPSC6311 / OPSC6312** (Campus / Study Group Organizer, Part 2: App Prototype). Author: Buchule.

| | |
|---|---|
| **Version** | 1.3.0 (versionCode 4) |
| **Package** | `com.studyhub.app` |
| **Min / Target SDK** | 26 (Android 8.0) / 34 (Android 14) |
| **Language** | Kotlin 2.0.20 |
| **UI** | XML layouts, Fragments and ViewBinding |

---

## Table of contents

1. [Features](#features)
2. [Tech stack](#tech-stack)
3. [Architecture](#architecture)
4. [Project structure](#project-structure)
5. [Getting started](#getting-started)
6. [Configuration](#configuration)
7. [REST API](#rest-api)
8. [Security](#security)
9. [Offline support and sync](#offline-support-and-sync)
10. [Languages](#languages)
11. [Testing](#testing)
12. [Continuous integration](#continuous-integration)
13. [Troubleshooting](#troubleshooting)

---

## Features

**Accounts**
- Register and sign in with email and password. Registration checks the email format and accepts accredited academic domains.
- Continue with **Google** (Credential Manager) or **Apple / iCloud** (Firebase OAuth provider).
- Password reset by email.
- "Remember me" with an offline credential check (see [Security](#security)).

**Study groups**
- Create a group with a subject, description and icon, and join one with its invite code.
- Group detail screen with the invite code and shortcuts to the group's tasks, sessions and chat.

**Planning**
- **Tasks**: title, description, due date, priority (High / Medium / Low), assignee and completion. Each completed task earns 10 points on the student's profile.
- **Study sessions**: title, start and end time, location and a "going" list.
- **Calendar** with a daily agenda of the tasks and sessions on the selected day.
- **Reminders**: a local notification when a task or session is due within the next hour, even offline.

**Collaboration**
- **Group chat** in real time, with push notifications for new messages.
- **Attendance**: record attended or absent per finished session and see an attendance percentage.
- **Polls**: a question with 2 to 4 options, one vote per member, and a closed state.
- **Past papers**: upload PDFs (up to 10 MB) to a group and open them in any PDF viewer.

**Home and profile**
- Home dashboard with a daily motivational quote from the REST API. A localized quote is shown if the API is unreachable.
- Profile with name, course, year of study, profile photo, points and tasks completed.
- Settings for dark mode, offline sync, notifications and app language.

---

## Tech stack

| Area | Library |
|---|---|
| Language / build | Kotlin 2.0.20, Android Gradle Plugin 8.4.0, Gradle 8.6, KSP |
| UI | AppCompat, Material Components 1.10, Fragments, ViewBinding |
| Auth | Firebase Authentication, AndroidX Credentials, Google ID |
| Cloud data | Cloud Firestore, Firebase Storage, Firebase Cloud Messaging (Firebase BoM 33.1.2) |
| Local data | Room 2.6.1, DataStore Preferences |
| Background work | WorkManager 2.9.1 |
| Networking | Retrofit 2.11 with Gson, OkHttp 4.12 logging interceptor |
| Images | Glide 4.16 |
| Security | AndroidX Security Crypto (EncryptedSharedPreferences) |
| Testing | JUnit 4, AndroidX Test, Espresso |

---

## Architecture

The app uses a layered MVVM structure with a single-activity, multi-fragment UI.

```
Fragments (ui/screens)  ->  ViewModels  ->  Repositories  ->  Firestore / Storage (Firebase)
                                                          ->  Room (offline cache + task outbox)
                                                          ->  Retrofit (StudyHub REST API)
```

- **Fragments** render state and handle input. `MainActivity` hosts them and drives the bottom navigation (Home, Groups, Tasks, Sessions, Profile).
- **ViewModels** (`MainViewModel`, `AuthViewModel`, `SettingsViewModel`, `EngagementViewModel`) hold UI state.
- **Repositories** are the only layer that talks to data sources.
  - `AuthRepository`: sign-in, registration, Google and Apple login.
  - `StudyHubRepository`: Firestore data with a Room cache.
  - `ApiRepository`: REST calls.
- **Pure policy objects** (`Validators`, `PlanningPolicy`, `AttendancePolicy`, `PollPolicy`, `ApiEndpointPolicy`) hold business rules with no Android imports, so they run in fast JVM unit tests.

---

## Project structure

```
StudyHub/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json            # Firebase config (use your own project)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/studyhub/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── StudyHubApp.kt
│       │   │   ├── data/
│       │   │   │   ├── local/          # Room DB, DAO, entities, DataStore, secure credential store
│       │   │   │   ├── model/          # Firestore / API models
│       │   │   │   ├── remote/         # Retrofit client and API interface
│       │   │   │   ├── repository/     # Auth, StudyHub and API repositories
│       │   │   │   └── sync/           # WorkManager outbox sync
│       │   │   ├── engagement/         # Attendance and polls
│       │   │   ├── notifications/      # FCM service, reminders, push registration
│       │   │   ├── ui/                 # Fragments, navigation, dialogs
│       │   │   ├── util/               # Validators, policies, date and language helpers
│       │   │   └── viewmodel/
│       │   └── res/                    # Layouts, drawables, themes, strings (en, af, zu)
│       ├── test/                       # JVM unit tests
│       └── androidTest/                # Instrumented tests
├── gradle/libs.versions.toml           # Version catalog
├── gradle.properties
└── build.gradle.kts
```

---

## Getting started

### Prerequisites
- Android Studio (a recent stable release that supports AGP 8.4)
- JDK 17 for Gradle (the project compiles to Java 11 bytecode)
- Android SDK Platform 34
- A Firebase project (see below)
- A running StudyHub REST API (see [REST API](#rest-api))

### 1. Clone and open
```bash
git clone <your-repository-url>
cd StudyHub
```
Open the folder in Android Studio and let Gradle sync.

### 2. Set up Firebase
1. Create a project in the [Firebase console](https://console.firebase.google.com/).
2. Add an Android app with the package name `com.studyhub.app`, and add your debug SHA-1 and SHA-256 fingerprints.
3. Download `google-services.json` and place it in `app/`.
4. Enable these products:
   - **Authentication**: Email/Password, Google and Apple providers
   - **Cloud Firestore**
   - **Storage** (past papers are stored under `group-papers`)
   - **Cloud Messaging**
5. Copy the **Web client ID** from the Google provider settings (used for "Continue with Google").

### 3. Point the app at your API
Edit `gradle.properties` (or pass the property on the command line):
```properties
studyhubApiBaseUrl=http://10.0.2.2:8080/
studyhubGoogleWebClientId=<your-web-client-id>.apps.googleusercontent.com
```
`10.0.2.2` is the Android emulator's alias for your computer's `localhost`. On a physical device, use your computer's LAN IP.

### 4. Build and run
```bash
./gradlew assembleDebug        # build the debug APK
./gradlew installDebug         # install on a connected device or emulator
```
Or press **Run** in Android Studio.

---

## Configuration

Both values below become `BuildConfig` fields and can be set in `gradle.properties` or with `-P` on the command line.

| Gradle property | BuildConfig field | Purpose | Default |
|---|---|---|---|
| `studyhubApiBaseUrl` | `API_BASE_URL` | Base URL of the StudyHub REST API (a trailing `/` is added if missing) | `https://example.com/` (overridden to `http://10.0.2.2:8080/` in `gradle.properties`) |
| `studyhubGoogleWebClientId` | `GOOGLE_WEB_CLIENT_ID` | Web client ID for Google sign-in | The project's client ID |

Example for a release build:
```bash
./gradlew assembleRelease -PstudyhubApiBaseUrl=https://api.your-domain.com/
```

**Endpoint rules** (`ApiEndpointPolicy`): the API URL must use **HTTPS**, with one exception. Debug builds may use plain `http` only for `localhost` and private network addresses (`10.x`, `172.16-31.x`, `192.168.x`). URLs with credentials, query strings or fragments, and the `example.com` placeholder, are rejected.

---

## REST API

The app calls a separate StudyHub REST API (the code describes it as an Express service using Firebase Admin). The service is **not part of this repository**, so run your own instance and set `studyhubApiBaseUrl`.

Protected routes expect the signed-in user's Firebase ID token:
```
Authorization: Bearer <firebase-id-token>
```

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/quote/today?lang=` | No | Daily motivational quote (`id`, `content`, `author`) |
| GET | `/api/resources?subject=` | No | List shared study resources, optionally filtered by subject |
| POST | `/api/resources` | Yes | Add a resource (`title`, `subject`, `url`) |
| PUT | `/api/resources/{id}` | Yes | Update a resource |
| DELETE | `/api/resources/{id}` | Yes | Delete a resource |
| POST | `/api/notifications/register` | Yes | Register this device's FCM token |
| POST | `/api/notifications/unregister` | Yes | Remove this device's FCM token |
| POST | `/api/notifications/group-message` | Yes | Notify group members of a new chat message (`groupId`, `messageId`) |

---

## Security

- **Authentication** is handled by Firebase. The app never stores a raw password.
- **Offline login check**: the "remember me" feature keeps the email and a salted **PBKDF2-HMAC-SHA256** hash (120,000 iterations). The data is stored in `EncryptedSharedPreferences` (AES-256-GCM, key held in the Android Keystore). Hash comparison is constant time.
- **Logging**: HTTP logging is `BASIC` in debug builds and off in release builds. Authorization headers, tokens and passwords are never logged.
- **Past papers** are downloaded through authenticated Firebase Storage calls, not public URLs, and shared with PDF viewers through a `FileProvider`.
- **Account isolation**: cached Room data is keyed by user ID, and the sync worker never replays one account's queue under another account.
- **Secrets**: do not commit production keys. Keep your own `google-services.json` out of public repositories and restrict API keys in the Google Cloud console.

---

## Offline support and sync

- Groups, tasks, sessions and chat messages are cached in a **Room** database (schema version 4, with migrations 2→3 and 3→4), so screens load without a connection.
- Task changes made offline are saved to a durable **outbox** table. `PendingTaskSyncWorker` (WorkManager) replays them once the device is online and the same user is signed in.
- A periodic `PlanningReminderWorker` runs every 15 minutes and posts reminders for tasks and sessions due within the hour, using cached data.
- Offline sync can be turned off in **Profile → Settings**.

---

## Languages

The app UI is available in **English**, **Afrikaans** and **isiZulu**. Students can switch language in settings, and the choice is stored as a BCP-47 tag (`en`, `af`, `zu`) and applied through Android's per-app language support. String resources live in `res/values`, `res/values-af` and `res/values-zu`.

---

## Testing

### Unit tests (JVM)
```bash
./gradlew testDebugUnitTest
```
Reports are written to `app/build/reports/tests/testDebugUnitTest/index.html`.

| Test class | Covers |
|---|---|
| `ValidatorsTest` | Email, name, password and date-of-birth validation |
| `PlanningPolicyTest` | Task and session rules, points, reminder timing |
| `EngagementPolicyTest` | Attendance percentage and poll validation and voting |
| `ApiEndpointPolicyTest` | HTTPS and local-debug endpoint rules |
| `AppLanguagesTest` | Language tag normalization |
| `TaskSyncCodecTest` | Encoding and decoding of queued task operations |
| `ChatComposerTest` | Chat composer state while editing and sending |

### Instrumented tests (emulator or device)
```bash
./gradlew connectedDebugAndroidTest
```
`AccountCacheMigrationTest` checks the Room migrations and per-account cache, and `FeatureStorageTest` checks local feature storage.

---

## Continuous integration

GitHub Actions runs the build and unit tests on every push and pull request. The workflow is defined in `.github/workflows/android-ci.yml`. Test reports are uploaded as a build artifact.

---


## Author

**Buchule**: OPSC6311 / OPSC6312 coursework.
