# OPSC6321---StudyHub-part-2

# StudyHub

**Organize. Collaborate. Achieve. Together.**

StudyHub is an Android study-group organiser built in Kotlin with Jetpack
Compose. Students create study groups, assign tasks with due dates and
priorities, schedule study sessions, chat with their group and track their
progress — all synced live through Firebase.

> Module: OPSC6311/6312 — Part 2, App Prototype Development
> YouTube video Link : https://youtu.be/l2lSKYYWNUc



---

## Purpose of the app

Group study at university usually falls apart in the admin: nobody knows who is
doing which part of the assignment, the session gets booked in a WhatsApp thread
that scrolls away, and the deadline arrives before the work does.

StudyHub puts all of that in one place. A student signs in, joins or creates a
subject group, and from then on the group shares one task list, one calendar,
one session schedule and one chat. Because everything is stored in Firestore
with offline persistence enabled, the app keeps working on campus Wi-Fi that
drops, then syncs when the connection returns.

## Features

| Feature | Description |
| --- | --- |
| Register & log in | Email/password via Firebase Authentication, with the password hashed server-side using scrypt |
| Continue with Google | One-tap sign-in through Credential Manager and the Google ID token |
| Continue with Apple (iCloud) | Apple ID sign-in through Firebase's OAuth provider |
| Unknown email flagging | The email field turns red and explains the address is not registered — checked on focus loss *and* on submit |
| Password strength meter | Live feedback while typing on the register screen |
| Home dashboard | Greeting, upcoming deadlines, quote of the day from the REST API, quick tiles |
| My Groups | Create, search and filter groups by All / Joined / Owned |
| Tasks | Create with due date and High/Medium/Low priority; tick off, filter, delete |
| Calendar | Month grid with agenda for the selected day, drawn from tasks and sessions |
| Study Sessions | Next-session hero card, upcoming list, attendance toggle, scheduling dialog |
| Group chat | Live Firestore-backed messaging per group |
| Profile & Settings | Stats, dark mode, offline sync, data usage, notifications, sign out |
| Offline support | Firestore persistent cache plus DataStore for settings |

## Design considerations

**Colour and type.** The palette comes straight from the design mock-ups — a
lilac background (`#E3C6E8`) with a stronger lilac for primary actions
(`#C77DE8`) and near-black ink for text. Contrast was checked so body text stays
readable on the lilac. All colours are defined once in `ui/theme/Color.kt` and
consumed through `MaterialTheme`, which is what makes the dark-mode toggle work
without touching a single screen.

**Rounded, soft shapes.** Every card, field and button uses a large corner
radius. This is carried by shared components (`PrimaryButton`, `LabeledField`,
`ScreenHeader`, `FilterChipRow`) so the screens stay consistent and a change to
one component updates the whole app.

**Error handling in the UI.** Errors are attached to the field they belong to
rather than shown in a generic toast. `AuthField` tells the ViewModel which box
to paint red, which is how an unregistered email is flagged on the email field
while a wrong password is flagged on the password field.

**No crashes on bad input.** Validation runs before any network call, every
repository call returns a `Result`, and every Firestore listener handles its
error branch. Invalid dates, empty fields, cancelled Google sheets and a missing
internet connection all produce a message instead of a crash.

**Accessibility.** Icons carry content descriptions, tap targets are at least
48dp, and the layouts scroll so they work on small screens.

## Architecture

MVVM with a repository layer:

```
UI (Compose screens)
   ↓ state / events
ViewModel (AuthViewModel, MainViewModel, SettingsViewModel)
   ↓ suspend calls returning Result
Repository (AuthRepository, StudyHubRepository, ApiRepository)
   ↓
Firebase Auth · Firestore · StudyHub REST API · DataStore
```

Screens are stateless: they receive a `UiState` and a set of lambdas. That keeps
the ViewModels testable without an emulator, which is what lets the whole unit
test suite run in GitHub Actions.

## Authentication

Three sign-in paths all land in the same Firebase user record:

1. **Email and password** — `createUserWithEmailAndPassword` /
   `signInWithEmailAndPassword`. The raw password is never stored by the app;
   Firebase hashes it with scrypt on their servers.
2. **Google** — `GetGoogleIdOption` through `CredentialManager` (the replacement
   for the deprecated `GoogleSignInClient`), then
   `GoogleAuthProvider.getCredential()`.
3. **Apple / iCloud** — `OAuthProvider.newBuilder("apple.com")` with
   `startActivityForSignInWithProvider`, which opens Apple's web sign-in sheet.

### Flagging an email that does not exist

Firebase's email-enumeration protection makes `fetchSignInMethodsForEmail`
return an empty list, and opening the `users` collection to unauthenticated
reads would leak every student's address. StudyHub instead keeps an
`emailIndex` collection keyed by the **SHA-256 hash** of the email. It is
readable before sign-in, but contains only digests and cannot be listed, so it
answers "is this address registered?" without exposing anyone's email.

The check runs twice:

- when the email field loses focus on the login screen, so the student is warned
  before they even type a password;
- after a failed sign-in, to decide whether to blame the email or the password.

### Password security

`PasswordSecurity` handles the local side: PBKDF2-WithHmacSHA256, 120 000
iterations, a 128-bit random salt, and a constant-time comparison. The resulting
hash backs the encrypted "remember me" cache in `SecureCredentialStore`, which
writes through `EncryptedSharedPreferences` (AES-256-GCM, key held in the
Android Keystore). Logs only ever contain an 8-character one-way fingerprint.

## REST API

The app calls a REST API that is part of this repository, under `api/`. It is an
Express service backed by Firestore, called from Android with **Retrofit**.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/quote/today` | Quote of the day on the Home dashboard |
| GET | `/api/resources` | Shared study resources, filterable by subject |
| POST | `/api/resources` | Add a resource |
| PUT | `/api/resources/:id` | Update a resource |
| DELETE | `/api/resources/:id` | Delete a resource |

Deployment instructions are in `api/README.md`. Once deployed, paste the public
URL into `API_BASE_URL` in `app/build.gradle.kts`.

## Firebase setup

1. Create a project at <https://console.firebase.google.com>.
2. **Add app > Android**, package name `com.studyhub.app`.
3. Download `google-services.json` and drop it into `app/`.
4. **Authentication > Sign-in method**, enable:
   - Email/Password
   - Google — then copy the **Web client ID** into `GOOGLE_WEB_CLIENT_ID` in
     `app/build.gradle.kts`
   - Apple — requires an Apple Developer account; set the Services ID, Team ID,
     Key ID and private key
5. Add your debug **SHA-1** fingerprint under Project settings (needed for
   Google sign-in):
   ```bash
   ./gradlew signingReport
   ```
6. **Firestore Database > Create database**, then publish the rules from
   `firestore.rules`.

## Running the project

```bash
git clone https://github.com/<your-username>/StudyHub.git
cd StudyHub
# add app/google-services.json, then open in Android Studio and sync
./gradlew assembleDebug
```

Requires Android Studio Ladybug or newer, JDK 17, and a device or emulator on
API 24+.



## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Auth (SDK) | Firebase Authentication, Credential Manager, Google Identity |
| Database | Cloud Firestore with offline persistence |
| Networking (external library) | Retrofit + Gson + OkHttp logging |
| Images (external library) | Coil |
| Local storage | DataStore Preferences, EncryptedSharedPreferences |
| Testing | JUnit 4, MockK, kotlinx-coroutines-test, Robolectric |
| CI | GitHub Actions |

## Project structure

```
StudyHub/
├── api/                      # Express + Firestore REST API
├── app/src/main/java/com/studyhub/app/
│   ├── data/
│   │   ├── local/            # DataStore settings, encrypted credential cache
│   │   ├── model/            # Firestore data classes
│   │   ├── remote/           # Retrofit interface and client
│   │   └── repository/       # Auth, Firestore and API repositories
│   ├── ui/
│   │   ├── components/       # Reusable Compose components and dialogs
│   │   ├── navigation/       # Routes and NavHost
│   │   ├── screens/          # One file per screen
│   │   └── theme/            # Colour, typography, Material theme
│   ├── util/                 # Validators, password security, date helpers
│   ├── viewmodel/            # AuthViewModel, MainViewModel, SettingsViewModel
│   ├── MainActivity.kt
│   └── StudyHubApp.kt
├── app/src/test/             # Unit tests
├── .github/workflows/        # GitHub Actions CI
└── firestore.rules           # Security rules
```

## Use of AI tools

_Replace this section with your own write-up (maximum 500 words) covering which
AI tools you used, for what, and how you verified the output. Be specific: name
the files or features, and describe what you changed afterwards._

## References

- Android Developers, 2025. *Jetpack Compose documentation*. [Online]
  Available at: <https://developer.android.com/jetpack/compose/documentation>
- Firebase, 2025. *Firebase Authentication on Android*. [Online]
  Available at: <https://firebase.google.com/docs/auth/android/start>
- Firebase, 2025. *Authenticate using Apple on Android*. [Online]
  Available at: <https://firebase.google.com/docs/auth/android/apple>
- Android Developers, 2025. *Sign in your user with Credential Manager*. [Online]
  Available at: <https://developer.android.com/identity/sign-in/credential-manager-siwg>
- Firebase, 2025. *Cloud Firestore security rules*. [Online]
  Available at: <https://firebase.google.com/docs/firestore/security/get-started>
- Square, 2025. *Retrofit*. [Online] Available at: <https://square.github.io/retrofit/>
- GitHub, 2025. *Automated build - Android app with GitHub Action*. [Online]
  Available at: <https://github.com/marketplace/actions/automated-build-android-app-with-github-action>
