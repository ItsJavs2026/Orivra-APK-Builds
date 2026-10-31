# Orivra Build 416 research — UI hot path and editor stalls
Checkpoint: 2026-10-31
Status: **research/diagnostics only — no Build 416 APK, no user-phone verification**
Based on: `candidate/build415-fast-type-preview-recovery-20261031` at `b1edc3c435e063f8bf782ac60bb09f1c7a8be289`.

## Preserve current test baseline
- User has just downloaded Build 415 for Galaxy S26 Ultra (Android 17). Do not treat download as evidence of compile success, install success, or performance.
- Keep Build 415 branch and older trial apps intact; keep Gboard available, do not erase learning or backups.
- Build 415 app ID `com.chroomareborn.keyboard.build415speedtrial`; tests use a separate package/data store.
- The legacy private Chrooma-Reborn Alpha repository is **not** the correct modern source.

## Hard measurements from *Build 414*, not Build 415
114? No: audit elapsed **1130s**, FULL prediction, Calm ON, Idle Paint ON, Rise ON, frame updates ON.
- Letter: 296 keys; mean **3.55ms**; max **204.84ms**; 8/16/32ms exceedances 11/8/2.
- Initial key editor read: mean **1.70ms**, max **202.37ms**.
- Space: 8; mean **31.22ms**, max **55.54ms**.
- Suggestion entry: 485; mean **7.44ms**, max **197.73ms**, 42 >=16ms.
- Layout refresh: 68; mean **19.06ms**, max **152.22ms**, 28 >=16ms.
- Idle word observation: mean **0.07ms** (414's successful hot-path reduction).
- Stale untappable chips hidden 42; discarded frames 0; retries 0.
Stage clocks are nested; counts/timings are not end-to-end finger-to-glyph latency and cannot simply be summed.

## Source review — Build 415
Inspected `RebornImeService.java`, `PredictionDispatcher.java`, `IdlePredictionPaint410.java`, and Build 415's checkpoint.

What is actually changed:
- `FastKeySample415.skipInitialBeforeRead`: skips only redundant 1-char editor sample during safe existing composition; verified compositor still checks editor, and non-composing/cursor/private input follows previous path.
- `applyFastQuietPolicy415`: 145ms quiet threshold when enabled (210ms fallback), so FULL suggestions are intentionally deferred during an uninterrupted fast burst.
- Cached haptics at editor activation and `HAPTIC_TOUCH_415` timing.
- More granular Space `SPACE_SHIFT_415` and `SPACE_REFRESH_415` timers.
- Dispatcher scheduled/preempted/delivered counters. Delivered is **not** the same as visible chips.

Potential remaining *hypotheses*, not proven defects:
1. Main-thread work in `showSuggestions()` after idle can still collide with incoming ACTION_DOWN, even when postponed to 145ms. In the source, ordinary words invoke immediate suggestion assembly and `PredictionLite.publicWordForContext`; the worker result callback also performs personal dictionary/preference lookups and UI chip binding. 414 had suggestion-entry and layout outliers.
2. SystemUI/notification `InputConnection` reads/commits can stall. Build 415 removes only one qualifying initial read; existing editor verification remains necessary. A 414 editor-read max of 202ms justifies separate host/editor investigation.
3. A long fast burst may deliberately suppress predictions until typing pauses. This is a known 415 design limitation, not necessarily a model failure. Avoid making UI-heavy previews on every key without performance tests.
4. Haptic time precedes the character handler on ACTION_DOWN. Its cost is now measurable but user haptics ON/OFF comparison is outstanding.

## Build 415 A/B protocol, before any 416 APK
1. Use FULL, Idle Paint ON, Calm identical for comparison, Frame ON, Deferred Idle Word ON. Start Quick Wake ON and Safe Editor Sample ON.
2. First type benign text with audit OFF in ordinary composer, then stress typing / continuous fast typing and quick reply with music playing; inspect missing/doubled letters, dead taps, live chip binding and time to chip recovery after pause.
3. Enable audit, reset before each 30–60s comparable trial, copy **counts/timings only**.
4. Toggle Quick Wake OFF (returns 210ms), reopen editor, compare lag vs wake. Toggle Safe Sample OFF separately, reopen, compare editor stalls and correctness. Compare haptics ON/OFF separately.
5. Diagnose based on distinct evidence:
   - Initial editor read still >100ms: test guarded editor snapshot/combined read, but never remove editor/session validity checks.
   - `Suggestion entry` or `Layout refresh` spikes remain: move/bound main-thread ranking/paint tasks, test queue behavior and frame time.
   - Many dispatcher jobs canceled and few delivered: independently test lightweight prefix preview; do not add model work to ACTION_DOWN.
   - Jobs delivered, but no current tappable chips: inspect callback/session/frame acceptance and visible binding, not just predictor.
   - Space stays costly: use new Shift / Refresh counters before changing logic.
6. Treat any 415 phone result as unverified until user supplies it. No hidden text, PII, app IDs or actual typed phrases should be collected in reports.

## Implementation gate
Do **not** blindly publish Build 416 or assume an emulator test proves Samsung SystemUI responsiveness.
After 415 evidence, implement one isolated change with an ON/OFF switch on a new candidate branch. Run pure Java regression tests, Android compile, and an on-device notification-reply check; preserve 415 unchanged.
