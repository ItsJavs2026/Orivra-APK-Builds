# Orivra Part 5 — Candidate 304 centered idle predictions

Date: 2026-10-29. Direct continuation of Orivra Candidate 303, positively tested on physical Samsung Galaxy S26 Ultra Android 17. Preserve Candidate 303 as the newest positively tested handset build and Candidate 300 as a proven Google Keep compatibility fallback. Stable Preview #178, Build 262, Candidate 293/298 remain untouched.

## User report
The user's screenshot of a dark keyboard displayed idle next-word suggestions **I | The | Hey** anchored against the LEFT edge, leaving substantial empty space before the right expand caret. They asked for predictions to **center when nothing is being typed**. This is a cosmetic alignment request; do not change the prediction engine, dictionary, Safe Enter, Backspace, Google Keep privacy, or expanded caret.

## Source diagnosis and limited fix
- `RebornImeService.onCreateInputView()` constructs a `HorizontalScrollView` with `setFillViewport(true)`, containing a horizontal `LinearLayout candidates` previously initialized with `Gravity.CENTER_VERTICAL`. The `renderSuggestions` method created wrap-content next-word chips for `input.isEmpty()`, so those short chips were anchored to the leading edge while the viewport had unused horizontal width.
- `SuggestionStripLayoutPolicy.centerIdleSuggestions(input,count,sensitive)` now returns true ONLY with empty composing input, at least one candidate, and a non-sensitive editor.
- `renderSuggestions` toggles chip row gravity to `Gravity.CENTER_HORIZONTAL|CENTER_VERTICAL` for these idle cases and restores `Gravity.START|CENTER_VERTICAL` while editing/typing or for private messages. It avoids resetting gravity when unchanged. The scroll viewport, chip widths, separators, source predictions, click actions and right caret are unchanged.
- `SuggestionStripLayoutPolicyTest`: adds 38 pure-Java assertions for idle vs typed and private input/zero alternatives.
- `IdleSuggestionCenterAndroidTest`: 3 Android 35 View-measurement instrumentation tests against a realistic 360dp scroll + right caret fixture: three idle chips centered as a group, 1/2/3 chips centered, typed/private layout returns to start, caret remains fixed, and idle layout returns after clearing.
- Separate application identity `com.chroomareborn.keyboard.build304trial`, versionCode 304, versionName `1.0.23-build304-idle-predictions-centered`. User-installed Candidate 303 remains untouched.
- Build 303 optional dictionary remains default OFF with its original pinned ESDB content/license; it is not modified by the UI change.

## Quality gate
Workflow `.github/workflows/candidate304-centered-idle-apk.yml` pins Candidate 303 verified source SHA `ca2d42aee8a9ab6e98457422017c3deef5488b8c`, requires byte parity for prior language/security components and exact only one allowed method edit in `RebornImeService`; runs inherited pure-Java safety tests, Android 35 emulator 26-test suite and controlled isolated debug APK identity/signature/license/no-INTERNET checks. Public release occurs only after all guards pass.

- First build [run 40978738221](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40978738221) failed because the CI source-diff fixture had incorrect multiline indentation, not because of a keyboard defect. Corrected the CI check without changing UI logic.
- Second build [run 40978804751](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40978804751) Java job passed. **Verify emulator conclusion before claiming APK ready.**
- Expected prerelease tag `candidate-304-20261029` and filename `Orivra-Candidate-304.apk`. **Do not assume asset exists until GitHub release API confirms it.**

## Physical test after CI success
Install separately, select `Orivra Candidate 304`. Open a disposable Samsung Notes or Google Keep normal note with NO text being composed. Three default next-word suggestions should appear centered as a group; type `hey` to confirm active typing reverts to normal layout; Backspace to empty to recenter. Check expand caret remains right, Keep predictions, Safe Enter, any flicker or unexpected shifting. If problems, revert to Candidate 303. Only mark phone verified on explicit user feedback.
