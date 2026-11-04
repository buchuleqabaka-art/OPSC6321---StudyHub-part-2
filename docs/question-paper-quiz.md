# Question Paper Quiz

Home → Question Paper Quiz → Choose PDF → select 5 or 10 questions → consent → Generate quiz.

Upload a readable PDF up to 5 MB. The paper is sent to the StudyHub API and OpenAI only after consent. The app does not keep the original paper after successful generation. The server sends it inline and does not upload it to permanent file storage. Provider data handling policies still apply; `store: false` is not a promise of zero provider retention. Use papers you may share and remove personal details.

Answer one question at a time. Each submitted answer is saved on this device. Reopen a quiz to resume, review the score and explanations, or retry incorrect answers as a new quiz. Each account can keep 20 quizzes. Delete a quiz to free space. Saved quizzes and answers are account-scoped in app-private, non-backed-up storage; uninstalling or clearing app data removes them. They do not sync across devices and do not change task points.

## Server setup

- Use Node 22 and the existing Firebase Admin credentials.
- Set `OPENAI_API_KEY` in the API hosting service's environment, never in Android code or GitHub.
- Optional `OPENAI_QUIZ_MODEL` defaults to `gpt-4o`; use a model supporting PDF inputs and strict structured outputs.
- Deploy `server.js`, `server/quiz.cjs` and `package.json` together.
- Configure Android `studyhubApiBaseUrl=https://YOUR-API-HOST/` in your local Gradle properties, then rebuild.
- The server uses a Firestore transaction in `quizUsage/{uid}` to cap generation at 10 attempts per UTC day. Failed provider attempts consume quota. Clients must not be granted access to this collection. Existing default-deny rules cover it.
- Endpoint: authenticated `POST /api/quizzes/generate?count=5&lang=en`, `Content-Type: application/pdf`, raw PDF body and Firebase bearer token.
- Responses: 400 invalid PDF/options; 401 auth; 413 oversized PDF; 429 daily quota; 503 missing AI configuration; 502 generation unavailable/unreadable/insufficient paper. PDF signatures are screened but the provider is responsible for parsing valid PDFs.
- Configure hosting request limits/timeouts to allow 5 MB uploads and a 90-second provider request. Monitor actual provider usage and set project budget alerts. This is practice feedback, not an official marking memorandum.

## Verification

Automated: `npm run test:quiz`; Android `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

Physical-device checks still required: generate from a known paper and compare every answer to its memo; test EN/AF/ZU; select/cancel file picker; malformed/large/encrypted/scanned PDF; denied consent; offline generation; timeout; account switch during generation; rotate and leave/reopen during quiz; finish and retry mistakes; delete; switch accounts; reopen saved quiz with airplane mode enabled. Live AI generation requires a deployed API and funded server key and has not been verified by the code tests.

Implementation references: https://developers.openai.com/api/docs/guides/file-inputs and https://developers.openai.com/api/docs/guides/structured-outputs (accessed 4 November 2026).

## Deploy the supplied container

1. Merge the feature PR when you are ready to deploy it, or point your host at `feature/question-paper-quiz` while testing.
2. In a Docker-capable hosting service, select this repository and its root `Dockerfile`. The container starts the API on `PORT` (8080 by default), listens on all interfaces, and runs as an unprivileged user. Use `/` as the health-check path. A healthy process does not prove Firebase or AI credentials work.
3. Add `OPENAI_API_KEY` to the host's secret environment settings. For non-Google hosting, also set `FIREBASE_SERVICE_ACCOUNT` to your Firebase service-account JSON. For Google Cloud, assign an appropriate service account and use Application Default Credentials. Never send these secrets in a chat or commit them.
4. Ensure Firebase Auth and Firestore use the same project as `app/google-services.json`. Enable the required Firestore API and grant the backend service account access to the existing collections and `quizUsage`.
5. Deploy and copy the HTTPS URL. Open it in your browser: it should return `StudyHub API` and `status: ok`.
6. Copy the setting from `android-api.properties.example` into your user-level Gradle properties file, replace `https://YOUR-API-HOST/` with that URL, sync Gradle, rebuild and install the app.
7. Sign in and generate a quiz from a short known PDF. Check its answers against a memo. Turn off internet and reopen the saved quiz to verify offline practice.

`GET /api/quizzes/status` with a Firebase bearer token reports whether a key is configured (without exposing it), upload size and supported counts. This checks configuration presence only; it does not validate the key, provider billing, model access or Firestore permissions. Only an end-to-end quiz generation verifies those.

For a local Docker build: `docker build -t studyhub-api .`. Set secrets in a local ignored `.env`, then run `docker run --rm --env-file .env -p 8080:8080 studyhub-api`. The Android production client requires HTTPS, so deploy behind your host's HTTPS endpoint for phone testing. Docker is not required to open the Android project.
