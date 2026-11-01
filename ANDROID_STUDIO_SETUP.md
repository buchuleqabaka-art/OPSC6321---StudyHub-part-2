# Run StudyHub in Android Studio

1. Download and extract `StudyHub-Android-Studio.zip` (or clone the repository).
2. In Android Studio, choose **Open** and select the project root containing `settings.gradle.kts` and `gradlew`.
3. Set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to Android Studio's bundled JDK 17, then let Gradle sync.
4. Use an Android 8.0/API 26 or newer emulator/device. A Google Play image is recommended for Google sign-in and FCM. Sign in with a test Firebase account.
5. If using a different Firebase project, replace `app/google-services.json`, configure the required Firebase services, and deploy `firestore.rules` and `storage.rules` as described in `README.md`.
6. To use a deployed API and your OAuth web client, add these lines to your **user-level** `~/.gradle/gradle.properties` (not the repository):

   ```properties
   studyhubApiBaseUrl=https://your-api-host.example/
   studyhubGoogleWebClientId=your-web-oauth-client-id.apps.googleusercontent.com
   ```

   Without a deployed API, the app still opens and uses its localized quote fallback, but hosted quotes, API-backed resources, and push delivery will not work. Never use `localhost` as the API URL on a physical device.
7. Choose the **app** run configuration and press **Run**. To execute checks from Android Studio's terminal:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:assembleDebug
   ```

The project source was not compiled in the coding sandbox because that environment did not have Java installed. Please run Gradle Sync and the checks above locally before pushing; resolve any project-specific Firebase/Gradle configuration prompts in Android Studio.
