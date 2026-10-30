# Orivra Part 5 — Build 310 Google Search editor capability / Enter / gestures

Date: 2026-10-29 CDT / run can cross midnight UTC. Source-only research branch `research/build310-editor-capability-search-enter-20261029`. Preserve Candidate 309 isolated package and physical Samsung acceptance boundary; do not overwrite Candidate 308 or Candidate 304.

## Samsung user screenshots and observed defects

On Samsung Galaxy S26 Ultra Android 17 using Orivra, the user provided 3 screenshots of a Google search interface (`keyboard` / `keyboard live touch wallpaper` and a wrapped query). All display **“Private field • predictions off”** in Orivra, even though they are ordinary visible search queries. The user explicitly reports Enter inserts a line break instead of searching, Backspace/delete swipe fails and the spacebar horizontal cursor gesture fails when this misclassification happens. User has also reported and earlier confirmed the Google Keep fix. These screenshots DO NOT provide precise `EditorInfo.inputType`, `imeOptions` or Google app package identity; Android 17 Samsung field verification still required.

## Root causes confirmed in source

1. `EditorSuggestionPrivacyPolicy.decide` returned `sensitive=true` whenever a text editor set `TYPE_TEXT_FLAG_NO_SUGGESTIONS`, except one hardcoded Google Keep exception. That Android flag is a dictionary-UI preference, **not** a password marker. It not only hid all predictions, but the same global `sensitive` flag disabled Backspace swipe snapshot collection, word deletion, and spacebar cursor movement. This is a systemic, not app-specific, bug.
2. `attachSafeEnterKey` sent all tap events to `insertNewlineSafely`, ignoring `IME_ACTION_SEARCH` and `IME_ACTION_GO` even when the editor explicitly requested them.

## Narrow source improvements

- `EditorSuggestionPrivacyPolicy.Decision` now has **three independent outputs**: `sensitive` (genuine password/email/URI/non-text editor), `allowPersonalLearning` (OFF for `NO_SUGGESTIONS` and/or `IME_FLAG_NO_PERSONALIZED_LEARNING`), and `allowSuggestions` (allows normal free-form text, plus a narrow `NO_SUGGESTIONS` exception for **declared Search/Go** or the already phone-approved Google Keep normal note editor). Other NO_SUGGESTIONS fields can decline suggestions without losing Backspace/cursor navigation. Password/email/URL/non-text remain private and nonlearning.
- `RebornImeService` now stores `editorAllowsSuggestions` separately from `sensitive`. Every ordinary editing path uses actual credential privacy, not a suggestion UI hint; blocked suggestions don't start new predictions or load supplementary dictionaries, and display “Suggestions off for this field”, not a false password message. Both inputType and the editor-declared Search/Go action are used, **not package-specific guessed searches**.
- `EnterKeyPolicy.tapEditorAction` accepts ONLY `IME_ACTION_SEARCH` or `IME_ACTION_GO` on a normal tap, honoring `IME_FLAG_NO_ENTER_ACTION`. `SEND` still requires the previous deliberate hold and release. Normal multiline notes still insert newlines. The displayed key label is `⌕`/Go/↵ from the actual editor action.
- `RebornImeService.handleEnterTap` commits any composing query before `InputConnection.performEditorAction(SEARCH/GO)`. If the editor rejects the action and is NOT multiline, a tightly scoped physical Enter key fallback is allowed. No automatic SEND, no "search this text" guessed from its content. Calls happen after finger release, preserving Build 287 touch-target stability.
- `backspace` uses deletion in code points, then UTF-16 units if rejected, finally `KEYCODE_DEL` only when both calls reject the operation. It never repeats on success, nor sends to a detached editor.
- No internet permission, no automatic learning and no new background telemetry.

## Regression tests

- Existing `EditorSuggestionPrivacyPolicyTest` rewritten to verify normal Search/Go noSuggestions is not a password, still cannot learn, ordinary noSuggestions fields can suppress suggestions without disabling editing, and real credential variations stay protected.
- `EnterKeyPolicyTest` covers explicit Search/Go, disabled action flag, SEND hold-only, UI labeling, malformed action codes and all prior thumb-slop/release/cancel cases.
- `KeepPredictionSecurityAndroidTest` verifies actual Android `EditorInfo` flag mapping for Keep, Google Search and Chrome-like search, ordinary non-search noSuggestions, all credential/identifier variations, and non-text fields.
- `SearchEditorActionsAndroidTest`: 3 genuine Android 35 `EditText`/`InputConnection` integration cases (synthetic query only): Search action callback without newline; chat Send remains hold-only and noEnter action respected; NO_SUGGESTIONS search field can delete a char and move the caret via spacebar gesture while preserving query text.
- `.github/workflows/research310-google-search-editor-android.yml`: pinned exact source of Candidate 309 `871baee5ba7f82bf4f126824f8af83f35cfb2b46`, strict parity on every unrelated source and manifest, reruns 95 contraction, 75,035 cursor ticket, 105 tap-only, privacy and Enter Java tests, then Android 35 **44-test** emulator suite and pinned licensed English dictionary. Research only; does NOT publish new APK.

Research Actions run: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41003508799 . **Verify its final status** before claiming an Android-qualified Build 310 or publishing a phone APK.

## Samsung acceptance when new isolated candidate passes CI

- Google search field: enter `keyboard live touch wallpaper`; Orivra must NOT say “Private field”; suggestions may appear; tapping the Search/Go key should actually search, not add a line break.
- Google search: swipe left on Backspace to preview/delete a word, hold Backspace to repeat, and swipe horizontally on spacebar to move the caret. Search must continue working after gestures and moving the insertion point.
- Google Keep regular notes: suggestions present without learning when `NO_SUGGESTIONS` is set; Enter remains newline for real multiline text; existing caret/Backspace gestures and no duplicated spaces.
- ChatGPT chat input: normal tap Enter inserts newline (for multiline), intentional long-press sends only if the host declares SEND; no accidental send on ordinary tap.
- Genuine password, email/URI and numeric fields: continue to hide suggestions, never learn; no raw content is stored or logged.
- Previous Candidate 309 Were/We're and hows/how's options must still be tap-only and safe.

Android emulator testing is a synthetic Google-search-like EditText, **not the live Google app**. The user is the only source of physical Samsung Android 17 confirmation. If the Google search app reports a different EditorInfo contract (e.g. no explicit search action), investigate a safe feature-based fallback using an optional diagnostic of flags only (no query text), not hardcoded app names or silent submit.

## Future work
After Android qualification create a separate Candidate 310 APK and release gate with source parity to qualified research branch. Preserve Candidate 309, Candidate 308 and Candidate 304 as fallback installations. Do not merge into stable until user physically tests the Google search cases.
