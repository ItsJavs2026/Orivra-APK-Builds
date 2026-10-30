# Orivra Part 5 — Build 305 Learning V2 + English apostrophes

Date 2026-10-29. **Development research branch** `research/build305-learning-v2-contractions-20261029`; NOT a released APK or Samsung-tested build at time of checkpoint. Phone-confirmed working baseline: **Candidate 304** (user confirmed idle prediction centering visibly better). Preserve Candidate 303 as predecessor and Candidate 300 as Google Keep compatibility fallback.

## Why this work matters
- User explicitly asked to continue Learning beyond V1 and support punctuation-bearing words like **can't, ain't**; permitted concurrent work on language predictions.
- Source audit of Candidate 304: `PersonalLearning` V1 accepted only `[a-z]{2,24}` and kept single corrected words; `PersonalDictionaryPolicy` rejected apostrophes; `PredictionLite.suggestions` rejects non-letter raw input; `RebornImeService.type` previously finished composition when typing an apostrophe from the symbol page.
- Unicode [UAX #29](https://www.unicode.org/reports/tr29/) specifies an internal apostrophe can remain within a word between letters (WB6/WB7) and that leading/trailing quotation marks need careful handling. Both straight U+0027 and curly U+2019 are recognized. Android [InputConnection.setComposingText](https://developer.android.com/reference/android/view/inputmethod/InputConnection) permits updating a composing span with punctuation when managed by an IME.

## Implemented
- `EnglishApostrophePolicy.java`: pure-Java bounded canonicalization of U+0027 and U+2019; manual words with internal apostrophes including `O'Connor`; curated English contractions (can't, ain't, don't, I'll, we're, you've, y'all, etc); partial punctuation forms `can'` / `ain'`; optional missing-apostrophe candidates `cant→can't`, `aint→ain't`; ambiguous `were`, `its`, `well`, `ill`, `id` are never automatically mapped. All candidates require a tap to commit.
- `RebornImeService.java`: preserve a single apostrophe inside an active composing word, including from the symbol page; merge new options after the existing background predictor; use existing editor privacy policy to exclude private/no-personal-learning input; preserve Candidate 304 centered idle strip and offline dictionary feature.
- `CursorWordCorrectionPolicy.java`: when editing at the caret, an apostrophe can be part of a live word snapshot; exact live-state and stale-suggestion checks continue preventing destructive edits.
- `PersonalDictionaryPolicy.java` / `PersonalDictionary.java`: explicitly saved words may contain internal apostrophes. Curly and straight forms map to one canonical key, preserving selected display case; prefix suggestions allow a partially typed apostrophe.
- `LearningV2Policy.java`: extract only one short, changed word pair from an explicit accepted correction, including a fully aligned sentence with exactly one changed word. Preserve the legitimate `I` / `a` as **unchanged** context but never learn a one-character correction. Joined/no-space-to-spaced conversions and multiple changed-word sentences are deliberately excluded: they cannot be safely aligned and would risk storing user sentence content.
- `PersonalLearning.java`: keeps the original V1 `orivra_personal_learning_v1` preference file/key format, retaining V1 reinforced pairs within **the same Android app package**; adds apostrophized output and one-word extraction; requires 2+ explicit acceptances to promote a personal correction; diagnostics remain count-only by default; opt-in remains OFF by default.
- `MainActivity.java`: labels explain Learning V2 and dictionary support for contractions.

## Tests and qualification
- `EnglishApostropheAndLearningV2Test.java`: synthetic tests cover straight/curly, internal vs invalid apostrophe position, unsafe email/URL/digit/emoji input, exact preservation, missing-mark alternatives, manual saved words, explicit learning extraction, stale cursor edits and protected fields.
- `PersonalLearningV2AndroidTest.java`: Android `SharedPreferences` test with V1 pairs + new V2 reinforcement, verifies no whole-sentence data stored, default privacy in Google Keep with no suggestions; restores fixture preferences.
- `ApostropheComposingAndroidTest.java`: Android `EditText` + real `InputConnection` composition verifies `can'`→`can't` and `ain’`→`ain’t` and stale cursor-word safety. This is **not** a live Samsung physical keyboard gesture test.
- `.github/workflows/research305-learning-v2-contraction-qa.yml` pins Candidate 304 source and byte-compares unchanged prediction engine, privacy policy, editor manifest, app identity, optional language pack and suggestion-strip policies. Runs existing Java tests, new policy tests, and **30 Android 35 emulator instrumented tests**. Never publishes an APK.
- Initial CI attempt 40981822651 stopped because the inherited Java test compilation did not include the new `EnglishApostrophePolicy` dependency of `CursorWordCorrectionPolicy`. Corrected. Continue checking the **latest** run for final evidence.

## Critical sandbox limitation
The preference format is V1-compatible for an upgrade *within one Android package*, but Candidate 304 and future Candidate 305 use isolated `applicationId` values and **cannot automatically share private app data**. No cross-APK learned-history migration has been implemented. Do not claim otherwise. A separately authorized, reviewable import/export workflow can be designed later; do not copy private learned content without user action.

## Next
1. Verify full source-only Java and 30 Android 35 emulator tests; inspect actual failure evidence, repair and rerun as needed. No guesswork or fake Samsung claims.
2. Inspect UI and performance/latency effects of apostrophe composition, especially symbol-page long press and one-tap acceptance. Keep manual dictionary and V1 data unaffected.
3. If successful, create a separately signed Candidate 305 Android test APK with a new applicationId; never replace current physical Candidate 304.
4. Real Samsung acceptance: type and accept `can't`, `ain't`, `we're`, `I'm`; ensure punctuation remains in the composed word, and `were`/ `its` aren't silently changed. Verify Google Keep predictions and learning privacy.
5. In a separate research track, explore richer context-aware next-word ranking and eventual explicit user-controlled learning-data migration; do not bundle unqualified Spanish/glide typing changes into a working version.
