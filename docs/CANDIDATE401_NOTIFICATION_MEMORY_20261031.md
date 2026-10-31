# Orivra Candidate 401 — Notification Reply Responsiveness + Memory Audit

**Status at source creation:** Research 401 `bb6709640ab230c31699b18c0e60860b5bd32978` passed 310/310 Android 35 tests in run 41211369197 (0 failures/errors/skips). **Candidate 400 FAILED Samsung phone testing** due severe lag and dropped keys in a WhatsApp notification shade reply while music played; Gboard remained responsive. These are real user observations, not emulator estimates. No Candidate 401 Samsung/Android 17 verification yet.

## Exactly what changes

- Move expensive `showSuggestions()` work away from the immediate letter/Space commit path. One first press opens a fixed **96 ms**, not endlessly reset, deferred refresh window for the same active editor. Key text still uses verified InputConnection operations; staleness, privacy, selection and editor-session rules remain.
- Release inactive session transient maps, UI suggestion caches, cursor/selection tickets, and delayed Rise state on IME view hide or editor/service finish, without erasing any persisted learning.
- In hidden-process Android memory-trim events, also release optional public/offline dictionary packs and an app-color cache. Keep native IME view teardown behavior and private-editor policies unchanged.
- App-icon color work: cap at **one running + one latest waiting** background request; abandon queued obsolete app requests. Preserve the forty-entry in-memory LRU and bitmap.recycle() behavior.
- Separately opted-in typed-word worker: up to **128** queued verified Space-learning updates. On saturation, suspend optional *learning* for that editor so that no private words remain queued indefinitely; resume automatically at the next editor. Real Space and character commits are never dropped by this learning backpressure, and saved V4 data is neither cleared nor modified.
- Candidate-only, opt-in in-memory numeric performance lab now reports key-handler, deferred suggestion UI p95, periodically sampled Java heap usage/peak, and native heap allocations. The report does NOT retain text, suggestion strings, user metadata, timestamps, or network telemetry. Heap counts alone do NOT establish absence of leaks.
- Shortens the Settings activity's extremely long on-screen historical changelog to a focused test summary, reducing avoidable view layout and string memory. Older release histories remain available in GitHub.
- Retains Candidate 400's center prediction-chip DOWN, ASDF safeguards, physical Space order; Candidate 399's phrase index/in-place Shift; Candidate 396's learning/composer reset.

## Independent data-safe trial

- Application ID: `com.chroomareborn.keyboard.build401trial`, versionCode **401**, separate from Candidate 400, 399, 396 and 391.
- Keep **Gboard as the primary** keyboard until real Samsung responsiveness is proven. Keep previous trial APKs and all V4 exports; don't erase app storage.
- Export a **fresh V4** from the app containing the latest confirmed learning, import it into 401 with on-device preview, export a new 401 V4 and verify checksum and categories. Import is never automatic. Learning boundaries and backup grammar remain unchanged.
- Re-enable ASDF middle-row gesture separately if using letter-row swipes; swipe DOWN starting on a **center prediction chip** independently via prediction-strip mode.

## Must pass before any release

1. Source diff from approved Research401 exact commit: only `app/build.gradle.kts`, `MainActivity.java`, `CandidateLatencyMetrics.java` package string, `OfflineLanguagePackAndroidTest.java` package expectation, this document, and a candidate release CI workflow. Everything in `RebornImeService`, learning workers/stores/V4, privacy, 401 tests and 400 gesture support must match Research401 exactly.
2. Replay previous 130k letter-row + 80k idle + 120k DOWN + 150k chat reset + 210k stored-learning stress, 120k public phrase parity, Research400 903,566 synthetic cases, and Research401 1.5m+ editor/session/privacy coalescing assertions.
3. Independent Candidate401 Android 35 run **>=310/310** tests, **zero** failures, errors or skipped, with offline dictionary pack SHA verification, signed APK identity, no INTERNET permission, and SHA-256 digest. Publish a separate GitHub prerelease only after all green.
4. **These are not full live IME/notification-shade soak measurements.** Candidate401 may still lag on a Samsung S26 Ultra Android17, especially if each remote InputConnection call blocks. The user shouldn't resume using it as daily keyboard solely because CI passes.

## Device validation when the user opts to test

- In an ordinary disposable note, perform several long paragraphs, repeated Space, backspace, caps/Shift, multi-thumb taps and direct center-chip DOWN. Verify no missing spaces/letters.
- Only if normal editor is responsive, test a disposable **notification-shade reply** while music plays, then with music paused. Include 20+ rapid replies/refocus cycles.
- With explicit opt-in, review key-handler p95, deferred UI p95 and Java/native heap current/peak before/after a long typing session. Never submit passwords, private messages or diagnostic dumps containing user text.
- If freezes remain, stop testing, switch back to Gboard, and prioritize per-InputConnection IPC tracing and a safe remote-editor fast path rather than adjusting gestures or treating heap cleanup as a complete solution.
