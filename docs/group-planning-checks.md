# Group planning verification

## Local / automated evidence

PlanningPolicy tests cover title boundaries, future/start/end validation, completion
permissions, per-assignee points, duplicate rows, legacy tasks, reminder boundaries,
and exclusion of computed properties from Firestore serialization.
Firestore emulator tests cover shared/private reads, owner/group queries, assignment,
completion restrictions, deletion, validation, attendance, member-name access,
and membership revocation. They use demo-studyhub, not a production project.
The Room 3→4 SQL migration was checked locally with SQLite for row preservation and
account-qualified shared task IDs. AccountCacheMigrationTest provides the device test.

## Physical-device checks (next stage)

1. Deploy the new Firestore rules to the intended test project. Create two accounts.
2. A creates a group, opens it, and shares its code. B joins with that code.
3. A creates a task for B in that group. Both see it; a third unrelated account does not.
4. B completes it: creator stays A, B gains 10 points, A does not. Toggle repeatedly:
   the score switches between 0 and 10 rather than accumulating. Delete as A: points clear.
5. Create a personal task/session. Other accounts must not see it.
6. Create a group session with chosen date/start/end. Reject blank/long titles,
   past starts, equal end/start and end-before-start. Verify B can change only B's attendance.
7. Check Tasks/Sessions reached from a group are scoped; bottom navigation restores all items.
8. Offline, create/complete a task. Reconnect and confirm synchronization on both devices.
9. With notifications enabled, create an assigned task / attended session 30–45 minutes
   ahead. Keep the device available for the approximate periodic check; verify one alert
   per event/time. Disable notifications, complete the task or withdraw attendance and
   confirm future checks skip it. WorkManager timing is not exact.
10. Switch accounts and verify caches, drafts, planning scopes and reminders stay isolated.
11. Upgrade a v3 installation with cached tasks and queued edits to v4; verify data survives.
12. Repeat the new UI in English, Afrikaans and isiZulu; review translations with speakers.

Live API/Google SSO/FCM verification is deliberately deferred to the next stage.
Full chat attachments, broader settings management and Play Store submission evidence
are outside this group-planning change.
