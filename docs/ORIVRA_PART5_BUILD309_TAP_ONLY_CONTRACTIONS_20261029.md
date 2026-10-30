# Orivra Part 5 — Build 309 tap-only contraction alternatives and caret feedback

Date: 2026-10-29, time zone America/Chicago. Continuation from Candidate 308 release, not a fresh app.

## Actual user screenshot feedback (Samsung Galaxy S26 Ultra Android 17)

Candidate 308 was installed and the user replied **"Other than what I put in the screenshot. All good"**. This supports a general positive on-phone experience but does not prove that every backup/export/import scenario was tested; avoid falsely asserting complete feature acceptance.

The screenshots showed:
1. Typed **`Were`** in the ChatGPT input, with only **`Were`** visible as a suggestion. User expects **`We're`** to be offered as a separate, selectable candidate alongside valid `Were`.
2. Typed **`Hello there. hows`** and the keyboard displayed placeholder text **"Keep typing • corrections appear here"** rather than offering **`how's`**. This indicates possible composing/committed-caret transition or slow asynchronous prediction, not necessarily a model dictionary omission.

**Highest priority:** show sensible contraction alternatives promptly and preserve text when not tapped. Do NOT make `were` become `we're` automatically. Do not regress centered idle predictions, Google Keep compatibility, Safe Enter, Backspace, and the default-disabled personal learning policy.

## Research branch and implementation

Branch `research/build309-ambiguous-contraction-candidates-20261029` forked from CI-qualified, Samsung-installed **Candidate 308**. Physical 308 general experience is positive; two reported prediction behaviors remain open until this change is actually phone-tested.

- `EnglishApostrophePolicy.java`: a deliberately tiny `AMBIGUOUS_TAP_ONLY` English list (`were` -> `we're`, `hows` -> `how's`). Straight/curly, sentence-initial capitalization and all-caps hints supported; null, URLs, email, sentence fragments, protected text rejected. Unambiguous contractions are still available. `hasDistinctSuggestion` detects only eligible, distinct alternatives.
- `CursorEditSnapshotGuard.java`: reuses the IME's existing Unicode-letter/straight-apostrophe/curly-apostrophe token policy to guard the full contraction at the caret. Prevents tickets for partial words cut off at the apostrophe.
- `RebornImeService.java`: shows a tiny, **immediate** literal-plus-contraction preview for eligible composing tokens before the slower 40ms-background SentenceFlow result. For editors that commit each key so composition is empty, offers the same safe choices from the live cursor word only when a source-and-position editing ticket is valid; pending asynchronous editing path also shows immediate contraction choices. A tap still re-reads the live editor token and uses the safe replacement plan. No automatic text edit, no user-text logging, no network, no new learning behavior.
- `EnglishApostropheAndLearningV2Test.java`: old "were has no suggestion" fixture intentionally revised to "were preserves literal and offers an explicit alternative." Existing unsafe-input tests retained.
- `CursorEditSnapshotGuardTest.java`: new synthetic caret across `can't`, `we’re`, and punctuation-boundary cases.
- `ContractionTapOnly309Test.java`: dedicated synthetic tests for `Were`, `WERE`, `hows`, exact literal, case, private-field refusal, malformed/PII inputs, stale live caret, unchanged data until tap.
- `AmbiguousContractionEditorAndroidTest.java`: three genuine Android 35 `EditText`/`InputConnection` cases exercise `Hello there. hows`, `Were`, `WERE`. Each asserts the original editor text remains until the test manually chooses a replacement; the guarded plan replaces exactly one token, after which the old snapshot is no longer valid.
- `.github/workflows/research309-ambiguous-contractions-android.yml`: pins exact Candidate 308 source SHA `4d7c6fe346028618546664e840e53be7e0c4139e`, compares protected IME resources and preferences, runs all old and new Java tests and Android 35 **38-test** emulator suite with pinned licensed ESDB archive. It does NOT publish an APK.

## QA and eventual delivery
- Run: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41001117856
- **Java source and regression gate passed**. Android 35 emulator job must complete before claiming qualification. Do not invent its result.
- After passing emulator, build a separate `com.chroomareborn.keyboard.build309trial` APK with a dedicated CI release gate, and leave installed Candidate 308 unchanged.
- Required manual Samsung acceptance: type `Were` (see both `Were` and `We're`); type `Hello there. hows` (see literal + `how's` even with punctuation context); test case and all-caps; verify no silent changes and tap-only replacement; check Google Keep, idle centering, Safe Enter and Backspace. Keep Candidate 308 as safe fallback if anything fails.
