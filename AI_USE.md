# AI-use disclosure

StudyHub was developed with assistance from an AI coding agent (Arena.ai Agent Mode) during the OPSC6321 final-submission update. Assistance covered Kotlin/XML UI localization, Room/WorkManager offline task synchronization, Firebase Cloud Messaging and REST API integration, Firebase Storage and security-rule scaffolding, unit-test examples, CI configuration, and project documentation.

The AI was used as a development aid, not as a runtime feature. StudyHub does not call a generative-AI service while the app is running. The student remains responsible for understanding and reviewing every change, checking that it meets the module brief, validating the Firebase project configuration and rules, testing on target devices, and citing AI assistance according to the module's required citation style.

## Verification status at hand-off

- Android XML/resource parity was checked during implementation.
- `node --check server.js` passed after the REST API changes.
- Android JVM tests were added for language normalization and task queue serialization.
- A local Gradle build/test could not be run in the development environment because Java/JDK was unavailable. The GitHub Actions workflow is configured to run the unit tests and debug build; confirm its result before submission.
- Firebase Storage/Firestore rules and live sign-in, notification, API, and photo-upload flows still need verification against the configured Firebase project and real/emulated devices.

## Suggested citation wording

> Arena.ai Agent Mode. (2026). *AI coding assistance for StudyHub OPSC6321 final-submission implementation* [AI-assisted development tool]. Used for code scaffolding, localization, offline synchronization, notification/API integration, tests, and documentation; reviewed and verified by the student before submission.

Adjust the citation to the lecturer's prescribed style and describe the actual review/testing you personally completed. Do not claim a manual verification step that has not been done.
