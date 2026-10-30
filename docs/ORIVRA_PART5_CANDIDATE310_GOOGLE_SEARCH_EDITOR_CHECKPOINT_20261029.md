# Orivra Part 5 — Candidate 310 Google Search, Enter and gesture compatibility checkpoint

Date: October 29, 2026 America/Chicago (Actions run in UTC, potentially October 30). User has Samsung Galaxy S26 Ultra Android 17. THIS IS CONTINUATION of Orivra Part 5; do not create a new keyboard project.

## Proven prior user acceptance
- Candidate 304: idle next-word predictions centered as a group; user said “much better.”
- Candidate 300: Google Keep ordinary notes were wrongly treated as bank fields; after fixing, user explicitly replied “Fixed! Thanks!!”.
- Candidate 308: user shared `Were` showing only `Were` and `Hello there. hows` leaving an empty prediction message, otherwise “All good.” General Samsung positive except those issues. Candidate 309 was released with an isolated tap-only contraction fix but **physical 309 test result has not yet been reported**.
- Candidate 309 prerelease https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/tag/candidate-309-20261029 . Source SHA from verified CI run `871baee5ba7f82bf4f126824f8af83f35cfb2b46`, 38/38 Android emulator tests.
- Keep previous installs (Candidate 308/309/304), never overwrite stable Preview #178/Build 262.

## Current Samsung feedback and reproduced screenshots
Three screenshots from Google Search UI with synthetic search terms `keyboard` and `keyboard live touch wallpaper` show keyboard prediction strip **“Private field • predictions off”**, plus user's direct report: Enter adds a line break instead of performing the search, and the Backspace delete gesture and spacebar cursor movement do not work in the affected search editor.
These are **user-reported real phone problems**. The screenshots do NOT expose Android `EditorInfo.inputType` and `imeOptions`, so app-specific runtime verification remains open.

## Root source bugs and the generalized fix
1. Prior `EditorSuggestionPrivacyPolicy.decide` treated `InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS` as `sensitive=true` outside one hardcoded Google Keep case. That flag is only a dictionary suggestion preference, not a credential/privacy marker. The IME reused `sensitive` to disable backspace word swipe, preview and spacebar cursor movement, wrongly disabling editing functions in normal search editors.
2. Prior Enter handling always called `insertNewlineSafely`, ignoring explicit `EditorInfo.IME_ACTION_SEARCH` and `IME_ACTION_GO`. The Android 35 synthetic multiline search test showed the framework may set `IME_FLAG_NO_ENTER_ACTION` even when a field explicitly declares Search.

**Generalized design** (not a Google package-name hack):
- `EditorSuggestionPrivacyPolicy.Decision` now has independent `sensitive`, `allowPersonalLearning`, and `allowSuggestions`. Password/web-password/visible-password/email/URI/non-text remain protected. `NO_SUGGESTIONS` does not make a normal text field sensitive and NEVER permits personal learning. A normal editor may still opt out of prediction UI, but remains eligible for Backspace and spacebar cursor editing. A declared Search/Go action or the previously accepted Google Keep free-form note exception supports ephemeral offline suggestions without storing query text.
- `RebornImeService` now uses `editorAllowsSuggestions` for suggestion UI/optional language pack activation separately from actual sensitive credential restrictions. No personalized learning for noSuggestions/noPersonalized, no INTERNET permission.
- `EnterKeyPolicy.tapEditorAction` only selects explicit Search or Go (even when Android adds a generic NO_ENTER_ACTION alongside multiline). Regular chat SEND is still only on an intentional long press, while real multiline note Enter inserts newline. Search/Go uses `InputConnection.performEditorAction` after finishing composition; if rejected by a nonmultiline editor only, a physical Enter fallback may be used. Key text `⌕`/Go/↵ responds to host-declared action.
- Backspace tap first tries code-point deletion, then UTF-16 deletion; only if both reject, uses KEYCODE_DEL. No repeat after successful deletion, and no retry on detached editors. Backspace swipe still uses fully checked tickets and will not delete stale context.
- Previous Candidate 309 contraction, Candidate 308 Learning V2.1 / explicit export/import, Google Keep and centered prediction fixes preserved.

## Verified source-only and Android tests
- Java `EditorSuggestionPrivacyPolicyTest`: 132 synthetic checks of search/Keep, other editor no-suggestion opt-out, passwords/identifiers, and preserved gesture ability.
- Java `EnterKeyPolicyTest`: action routing for Search/Go, held SEND, multilines, motion cancellation and key labels.
- Java 309 inherited `EnglishApostropheAndLearningV2Test`, `ContractionTapOnly309Test`, `CursorEditSnapshotGuardTest`: 95+105+75,035 checks from the first research run.
- Real Android 35 synthetic `SearchEditorActionsAndroidTest`: Search action must trigger host callback without mutating query, NO_SUGGESTIONS search field must allow Backspace and spacebar cursor gesture, SEND remains held and host action flag handling is verified. `KeepPredictionSecurityAndroidTest` extends mapping with Search/Go/Chrome-like generic editor variants and actual credentials.
- Total expected emulator instrumentation count = 44/44. This is **NOT live Google Search and not Samsung 17**.
- Research CI https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41004699012 rerun after fixing a genuine test failure. Original research run 41003508799 wrongly appeared “success” even though Gradle failed one test because its post-test grep only checked `Finished 44 tests`. DO NOT cite that original success as qualification. Another rerun 41004350365 failed because multiline shell quoting was invalid. The final CI uses one folded POSIX shell command with explicit Gradle exit status, `Finished 44 tests`, `BUILD SUCCESSFUL`, and fail-marker rejection.
- Candidate release CI https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41004889134 checkout by a named accessible research branch, with exact SHA `abeb967656d29acaa39cffdace2e7626f2fce431` guard; copies every protected Java, manifest, Android test source byte-for-byte, allowing only separate package ID, app label and latency gate. If test count/Gradle/signer/identity/license fails, **do not release an APK**.
- Candidate package `com.chroomareborn.keyboard.build310trial`, versionCode 310, versionName `1.0.27-build310-search-editor-gestures`, label `Orivra Candidate 310`. Remains a separate debug-signed trial.
- Expected tag only **if published**: `candidate-310-20261029`. Direct URL is not confirmed until GitHub release API returns the actual APK with SHA256; do not invent file size/hash or claim physical acceptance.

## Physical Samsung verification after release
1. Install Candidate 310 alongside Candidate 309/308, do not uninstall the known-good keyboards.
2. Open Google Search (the same screen from the three screenshots); enter `keyboard live touch wallpaper`. Verify the bar no longer says **Private field**; predictions are offered.
3. Tap the new `⌕` Search key once. It should execute the real Google search without inserting a newline. If it does not, report whether the search icon changed and whether a newline appeared.
4. Backspace tap, hold for repeat, and slide left for word deletion; verify only desired word removed and no jump.
5. Drag horizontally on the spacebar; caret should move inside the search string without modifying it.
6. Switch to Google Keep and a disposable multiline note: predictions should remain; Enter inserts newline and caret/Backspace still work. Test a disposable ChatGPT draft: no accidental SEND on normal Enter tap.
7. Recheck `Were` / `We're`, `Hello there. hows` / `how's` and true password field hides suggestions, never learns.
8. If regressions, return to Candidate 309/308. Report screenshots plus app name and visible behavior; avoid sharing credentials or raw private text.

## Next development
On successful physical Candidate 310 tests, continue wider SentenceFlow / Learning V2 accuracy and bilingual offline research with new independent frozen test suites. Add optional **non-content** editor capability diagnostics (inputType class/variation, imeOptions action, no-suggestions/no-personalized flags, current app package if user explicitly opts in) to identify future custom editors without logging typed text; never assume a program's name reliably determines its editor semantics. Do not silently enable personal learning.
