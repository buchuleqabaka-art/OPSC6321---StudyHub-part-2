# Chat reliability verification

This change fixes chat observation lifetime and adds per-group in-memory drafts,
sending feedback, and retryable failures. A message enters the confirmed Room
cache only after Firestore reports no pending writes. Retrying an unchanged failed
draft reuses its document ID.

## Automated checks

Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
`ChatComposerTest` covers successful sends, retained failed drafts, stable retry
IDs, duplicate-submit prevention, editing after failure, and stale completions.

## Device / Firebase checks

1. Open group A, switch to B, then send a message to A from a second device.
   B must continue displaying only B's messages. Repeat A → B → A quickly.
2. Send a message online. The composer shows a waiting indicator, prevents a
   second submission, and clears only after server acknowledgement.
3. Disconnect, send a message, and reconnect. The composer must say it is
   waiting, not that delivery has failed or succeeded. The message should appear
   once after acknowledgement. Keep the app process alive for this check.
4. Using a Firebase test project/emulator, revoke the sender's group membership
   before sending. A rejected write must retain the draft and show a retry hint;
   it must not appear as a confirmed message. Restore membership and retry.
5. Start a send in A, switch to B, and type a draft there. A's completion must
   not clear B's draft. Return to A to see its updated send state.
6. Rotate while composing or waiting for acknowledgement. The draft and sending
   state should survive the view recreation.
7. Sign out and sign in as another account. No previous chat or draft may remain
   visible. Open a group available to the new account.
8. Scroll up in a long conversation, then receive a new message. The scroll
   position must stay fixed. At the bottom, new messages should remain visible.
9. Test a chat read permission failure: the refresh retry button must appear.
10. Repeat the sending/error states in English, Afrikaans, and isiZulu; check
    large text and screen-reader labels on a device.

## Scope and limitations

Drafts and send feedback are held in the activity ViewModel, not persisted across
process death. Firestore can retain queued writes, but this change does not add a
durable chat outbox or guarantee push delivery after process death. Full message
pagination, RecyclerView migration, and cleanup of historical cache records are
separate follow-ups. Native-speaker review of the new translations is recommended.
