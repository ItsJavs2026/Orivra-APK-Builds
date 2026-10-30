# Orivra Part 5 — Candidate 309 user screenshot fixes / Samsung phone trial

Date 2026-10-29 CDT. Continuation from Candidate 308 in the user's Orivra development channel, NOT a new keyboard.

## User report and confirmed status
- User installed Candidate 308 on their Samsung Galaxy S26 Ultra Android 17 and responded **"Other than what I put in the screenshot. All good"**. This is positive general feedback with **two specific open issues**, not confirmation that they individually tested all learning-export and editor cases.
- Screenshot A: composing `Were` showed only `Were` in prediction strip, user wanted the **tap-only** optional `We're` alternative without changing correctly spelled `Were`.
- Screenshot B: composing `Hello there. hows` after sentence punctuation showed **"Keep typing • corrections appear here"** instead of a selectable `how's`; may involve the host committing characters before Orivra's async predictions.
- Do not infer anything beyond the visible screenshot. The earlier confirmed Google Keep prediction fix and centered idle suggestions must remain.

## Fixed source and measured QA
- Qualified source branch `research/build309-ambiguous-contraction-candidates-20261029`; user-facing trial branch `candidate/build309-tap-only-contractions-trial-20261029`.
- `EnglishApostrophePolicy`: conservative, exact `were → we're` and `hows → how's` alternatives, case-safe for `Were`, `WERE`, `Hows`, `HOWS`. Never automatically changes the original word. Retains existing contraction coverage and rejects emails/URLs/phrases.
- `RebornImeService`: tiny immediate preview of original+alternate when text is composing; when a host commits each key and composition is empty, it checks the exact live caret word and shows alternatives only when a safe source-and-position editing ticket exists. A delayed full-model response cannot apply a stale edit. Tap still re-reads live editor context before bounded text replacement.
- `CursorEditSnapshotGuard`: straight/curly apostrophes count inside one word, matching the rest of the editor policy.
- Existing privacy restrictions, local Learning V2.1 and user-confirmed 304 centered UI retained. No INTERNET permission added.
- Research Android CI https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41001117856 **SUCCESS**; pure Java: 95 contraction checks, 75,035 cursor safety checks, 105 new tap-only checks; **38/38 Android 35 emulator tests PASS**, including 3 real `EditText`/`InputConnection` cases. This is not Samsung Android 17 device acceptance.

## Separate Candidate 309 release gate
- App ID `com.chroomareborn.keyboard.build309trial`, versionCode 309, versionName `1.0.26-build309-tap-only-contractions`, launcher Orivra Candidate 309. Candidate 308, 304, 300 remain installed separately and unaffected.
- Workflow `.github/workflows/candidate309-tap-only-apk.yml` pins research SHA `00f73902cf16776c68b9f4978839703b5ea20cb8` and compares all protected IME, language/security, personal-learning and Android test source byte-for-byte, allowing only expected app identity and launcher copy. Repeats pure Java assertions, verifies pinned licensed ESDB asset, full **38-test Android 35 emulator suite**, exact APK package ID, debug signing, NO INTERNET, checksum and publication. If any fail, no prerelease APK should be published.
- Release CI https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41001544375 **in progress at initial checkpoint creation**. Do not claim APK exists before final GitHub release API verification.
- Expected prerelease tag `candidate-309-20261029`, file `Orivra-Candidate-309.apk`. Verify exact SHA-256 and size only after successful publication.

## Physical Samsung acceptance, once released
1. Install Candidate 309 alongside Candidate 308 and select it only for test typing in a disposable ChatGPT draft or Samsung Notes/Google Keep field.
2. Type `Were`, confirm both **Were** and **We're** appear; correct spelling **Were** must remain unmodified without tapping.
3. Type `Hello there. hows`, confirm **how's** appears rather than the placeholder. Tap it once and confirm only `hows` changes.
4. Try `WERE`, `HOWS`, `can't`, `ain't` and curly apostrophes; watch case preservation and no accidental edits.
5. Check centered idle predictions, Google Keep title/body, Backspace tap/hold, Safe Enter and optional personal-learning controls.
6. If any issue recurs, switch back to Samsung-tested Candidate 308. Do not delete previous APKs to test this one.

Other project directions remain: broad multi-typo SentenceFlow benchmarking, separate Spanish research, and privacy-safe portable learning improvements. No automatic background typing data capture or forced autocorrect.
