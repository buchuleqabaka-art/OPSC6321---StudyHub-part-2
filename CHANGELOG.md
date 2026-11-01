# Release notes

## 1.2.0 — Final-submission feature release

- Added complete English, Afrikaans, and isiZulu app UI resources with saved per-app language selection and locale-aware dates.
- Added a Room task outbox and WorkManager synchronization for offline task creation, completion, and deletion, with a pending-sync indicator.
- Added Firebase Cloud Messaging group-message notifications and authenticated token registration, validation, and sign-out unregistration.
- Added account-aware notification delivery checks and Android 13+ permission handling.
- Added Firebase Storage profile-photo uploads with a 5 MiB/image-only validation limit and owner-scoped Storage rules.
- Secured REST API resource creation, updates, and deletion with Firebase ID-token validation and author checks; added localized quote fallbacks.
- Added Firestore and Storage rules, automated unit tests, API syntax validation in GitHub Actions, and expanded setup/demo documentation.

### Setup and compatibility notes

- Configure Firebase Authentication, Firestore, Storage, Cloud Messaging, the Admin SDK credentials, and `studyhubApiBaseUrl` before exercising the hosted features.
- Debug builds and local JVM tests are automated in GitHub Actions. The APK is not signed for store distribution; follow the release checklist in `README.md` before publishing.
