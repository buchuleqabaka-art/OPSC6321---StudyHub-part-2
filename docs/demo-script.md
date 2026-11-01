# StudyHub demo video plan

**Target length:** 3–4 minutes. Record a real Android device/emulator and the configured Firebase/API console. This is a recording plan, not a pre-recorded video; do not narrate a feature as live until it has been verified on the configured project.

## Before recording

- Deploy Firestore/Storage rules and configure Authentication, Storage, FCM, and the REST API.
- Install the current debug APK and create two test accounts in the same study group on two devices/emulators.
- Confirm notifications are permitted on the receiving device and enabled in StudyHub.
- Prepare a small image under 5 MiB and ensure the API's `/` health endpoint is reachable.
- Keep service-account keys, passwords, private user information, and personal notifications out of the recording.

## Suggested narrated walkthrough

### 0:00–0:20 — Introduction and sign-in

**Show:** launcher icon, Welcome screen, registration/login, then Home.

**Voice-over:** “StudyHub is a collaborative study planner for students. Members can organize study groups, assignments, sessions, and group conversations. Accounts are managed by Firebase Authentication, so the app does not store raw passwords.”

### 0:20–0:55 — Language support

**Show:** Profile → Language; select Afrikaans and show the translated navigation, settings, and Home content. Repeat with isiZulu. Open a screen with a date to show locale-aware formatting.

**Voice-over:** “The app supports English, Afrikaans, and isiZulu. The selection is saved as an app preference, and the language change applies throughout the UI rather than only changing the settings label.”

### 0:55–1:35 — Offline task synchronization

**Show:** Home sync indicator; enable airplane mode; create or complete a task; show the task remains visible and the pending count increases. Re-enable network; wait for the count to clear; show the corresponding task in Firestore.

**Voice-over:** “Task changes are first stored locally in Room. WorkManager waits for connectivity, verifies the signed-in account, and synchronizes the outbox to Firestore. The Home indicator makes pending work visible while the device is offline.”

### 1:35–2:00 — Profile-photo blob storage

**Show:** Profile → upload profile photo; select the prepared image; show the saved profile image and, briefly, the object under `profile-photos/{uid}/profile-image` in Firebase Storage.

**Voice-over:** “Profile images are uploaded to Firebase Storage, not kept as large database fields. Both the app and Storage rules restrict the image type and size, and the object path is scoped to the signed-in user's ID.”

### 2:00–2:45 — Group chat and push notification

**Show:** send a short group message from device A and receive the localized notification on device B. Briefly show the matching group/message record and, if useful, the authenticated API response in logs.

**Voice-over:** “Group messages remain in Firestore. For a push, the API verifies the Firebase ID token, confirms that the sender is a group member, and checks that the referenced message was written by that sender before notifying other members. The receiving app checks that the push targets its active account.”

### 2:45–3:15 — Hosted API and automated checks

**Show:** the REST API health response and a successful GitHub Actions run with the test/build steps and downloadable debug APK artifact.

**Voice-over:** “The Express API uses Firebase Admin and Firestore for hosted data. GitHub Actions checks the API syntax, runs the Android unit tests, builds the debug APK, and publishes the artifact for review.”

### 3:15–3:25 — Close

**Voice-over:** “StudyHub brings group study planning, offline task continuity, multilingual access, and real-time collaboration into one Android app.”

## Evidence checklist

- Show the app UI, not only slides; include the language change and offline/reconnect cycle.
- Show only non-sensitive hosted data in Firebase/Actions.
- Keep test credentials and private service keys off-screen.
- If a flow fails during recording, stop and fix it rather than describing it as successful.
