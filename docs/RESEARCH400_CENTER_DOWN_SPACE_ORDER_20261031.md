# Orivra Research 400 — center DOWN + reliable physical Space ordering

**Reproduction:** Samsung Galaxy S26 Ultra Android 17 screenshot on October 31, 2026 shows the host draft contains `thoseyou`, while Orivra's strip includes `those you` in the center. User reports DOWN not entering the prediction. Do not equate displayed suggestion with successful replacement, and do not infer which package or gesture origin was used without device confirmation.

**Baseline:** released Candidate 399 commit `cc73b1d5d36c23395fc91e98caa8b1c4ffe22b7a`, passing 294/294 Android35 tests and preserving Candidate 396. Candidate 399 is an independent package and its ASDF gesture setting defaults OFF; **re-enable in 399 settings if using letter-row swipes.** Candidate 399 release and original V4 remain unchanged by this branch.

## Two source-level pathways implicated

1. `PredictionSwipe372.direction` previously had LEFT, RIGHT and UP, **but not DOWN**. A deliberate downward slide starting on the center prediction chip therefore did nothing, even when a tap on that same chip was valid. Add DOWN only for >=~32dp axis-dominant motion; select only the actual center chip (index 1 for 3-slot strip, index 0 for a true singleton), never an adjacent chip or Rise. Reuse all existing same-editor/session/caret, live-word and explicit acceptance guards. No automatic text change on mere MOVE.
2. `LetterRowPredictionSwipe386` can recognize DOWN starting on ASDF/QWERTY only when the setting is enabled and exact editor/snapshot checks pass. A 320ms secondary-symbol hold can activate **before** slower finger travel reaches the full 32dp DOWN threshold. Recognize an early unambiguous 12dp downward *intent* solely to cancel the pending symbol-hold timer. Actual acceptance still requires the 32dp recognizer and live safety gates.
3. `HintKeyPressOrder353` queued alphabet presses in ACTION_DOWN order, but `attachSpacebarCursorControl` inserted a physical Space *immediately on ACTION_UP*, not into that same queue. Overlapping two-thumb Space/letter contacts can therefore mutate the host out of touch-start order. Give each physical Space a temporary queue token on ACTION_DOWN, resolve it with **one** deferred `space()` on ACTION_UP if recognized as a tap, or cancel it on swipe, second pointer, or ACTION_CANCEL. Validate the captured InputConnection and input session before the deferred commit. Keep accessibility/programmatic Space click unchanged. Never retry a rejected InputConnection mutation, save raw key text, change learning, or infer a missing space from a screenshot.

## Tests and safety gate

- Pure Java stress: 150k direction/center selection + early row intent scenarios, 35k overlapping letter–Space order scenarios (all synthetic).
- Five Android 35 tests: center-only DOWN, tap/diagonal/UP/LEFT parity, early ASDF intent, overlapping Space–letter ordering and canceled/stale safety.
- Full Android35 suite >=299, zero failures/errors/skips.
- Compare source exactly to Candidate 399. Only `PredictionSwipe372`, `LetterRowPredictionSwipe386`, `RebornImeService`, two new test files, this document and workflow may change. No changes to `PersonalLearning`, V4 backup/import, package, Android permissions, dictionary, caption, release logic, or core predictor. No Internet.
- **No APK on this research branch.** An independently signed package would be created only if all source and emulator tests pass.

## Important validation limits

These fixes target plausible concrete source problems, not proven causes of the user's reported Samsung missed Space or letter-row rejection. The screenshot alone does not distinguish lost physical Space from no-space typing or optional suggestion acceptance. The source and emulator tests cannot prove Android 17 gesture behavior on the actual phone. Keep 396/399 installed, preserve V4 backups and test future APK in a disposable text field, not private or banking data.
