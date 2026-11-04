# AI-use disclosure

StudyHub was developed with assistance from an AI coding agent (Arena.ai Agent Mode) during the OPSC6321 final-submission update. Assistance covered Kotlin/XML UI localization, Room/WorkManager offline task synchronization, Firebase Cloud Messaging and REST API integration, Firebase Storage and security-rule scaffolding, unit-test examples, CI configuration, and project documentation.

The AI was used as a development aid, not as a runtime feature. StudyHub does not call a generative-AI service while the app is running. The student remains responsible for understanding and reviewing every change, checking that it meets the module brief, validating the Firebase project configuration and rules, testing on target devices, and citing AI assistance according to the module's required citation style.

## Verification status at hand-off

- Android XML/resource parity was checked during implementation.
- `node --check server.js` passed after the REST API changes.
- Android JVM tests were added for language normalization and task queue serialization.
- A local Gradle build/test could not be run in the development environment because Java/JDK was unavailable. The GitHub Actions workflow is configured to run the unit tests and debug build; confirm its result before submission.
- Firebase Storage/Firestore rules and live sign-in, notification, API, and photo-upload flows still need verification against the configured Firebase project and real/emulated devices.


## Additional ChatGPT/Codex assistance — November 2026

ChatGPT/Codex assisted with chat-observer cleanup, send-state tests, group-linked
planning, task assignment and permissions, session time selection, derived completion
points, local reminders, account-qualified Room migration, emulator tests, and
supporting documentation. Earlier hand-off limitations above describe the earlier
stage; subsequent GitHub Actions build results are recorded in the pull requests.
Live Firebase/API/SSO/FCM and physical-device validation must still be performed.
The student must review and understand the changes and include an accurate AI-use
write-up and citations in the submitted document as instructed by the lecturer.

### Question Paper Quiz (4 November 2026)
ChatGPT/Codex assisted with the PDF quiz screens, account-scoped offline storage, authenticated backend integration, validation, translations and automated tests. The feature uses OpenAI to generate practice questions, suggested answers and explanations from a user-selected PDF after consent. Generated answers and translations need human review; the feature is not an official memorandum. Live generation and physical-device testing are documented separately from automated checks.
