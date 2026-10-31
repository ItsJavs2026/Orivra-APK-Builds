# Orivra Research 383 — Deferred editor mutations on a real Android main thread

**Research only, no APK or production IME modifications.** Branch based on Research 382 commit `2c1b65ca254d3bdf2ee8dbbf79e2258a78c7daee`. Candidate 378 remains the installed Samsung S26 Ultra trial, reported to feel good.

## Why this is different from Research 382

Research 382 proved with a pure-Java fake handler that sequencing touch releases does not guarantee editor-mutation order when letters are posted but Space can commit immediately. Research 383 adds **Android 35 instrumentation** with a real `Handler(Looper.getMainLooper())`, a genuine `EditText.onCreateInputConnection`, and actual `InputConnection.commitText` operations. The test builds isolated fictional editors and does not activate Orivra's `InputMethodService`.

Eight cases:
1. Demonstrate that a touch-only sequence barrier permits `" a"` when Space commits synchronously and letter mutation posts.
2. Post both accepted actions to the same main-Looper queue; expect `"a "`.
3. Three keys released in reverse order; expect `"a b"`.
4. A toy highlighted correction checks the completed `"ke"` at the actual queued Space execution, then replaces it with `"keyboard "`.
5. Switching to a new editor/session before callbacks execute must discard all stale actions.
6. Canceling the first letter must allow a later confirmed Space.
7. A recognized Space cursor swipe never enqueues a literal Space.
8. A long-held secondary symbol followed by duplicate letter release must emit only `"# "`.

**Limitations:** The correction test models ordering only; it does not exercise production `HighlightedSpace372`, its confidence thresholds or user dictionary. No live Samsung timing, production view dispatch, secure-field writing, virtual keyboard activation, or user data. Do not integrate into Candidate 378 solely from emulator success.

## CI gates

Restrict changes to this test, the research document and the no-release GitHub workflow; preserve Candidate 378 package/version and no INTERNET permission. Compile the existing app with its pinned offline dictionary, run **at least 242 Android 35 tests** (234 baseline + 8 new), require zero failure/skip/error; do not publish an APK. Top-bar work remains the user's separate tomorrow-morning milestone.
