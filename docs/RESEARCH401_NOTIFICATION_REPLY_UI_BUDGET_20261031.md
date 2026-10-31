# Orivra Research 401 — Notification Reply / Main-Thread Typing Budget

**Failure prompting work:** User reports Candidate 400 freezes, stops accepting some keys, and is particularly slow while replying to a WhatsApp notification shade with music playing. A supplied 63.79-second recording shows Orivra during a status-bar WhatsApp reply then a switch to Gboard. **Candidate 400 FAILED REAL SAMSUNG RESPONSIVENESS TEST; do not recommend continuing to use it.** This recording does not prove the music application caused the lag and does not establish exact millisecond delay.

**Baseline:** Candidate 400 independent release `5fbe7437b3d31c3253e9e704bba349411d64bffe`, 299/299 Android 35 tests. No source updates to any existing release.

## Source-backed cause candidates

- The IME's physical letter handler commits via checked, synchronous InputConnection operations on the Android UI thread, then runs `showSuggestions()` on that same thread **for every accepted character**. `showSuggestions` reads editor text, consults personalized word/context sources and rebuilds suggestion view state; Android selection echoes can trigger another refresh. Even with offline prediction calculation on a background worker, the UI preparation remains synchronous.
- In a remote notification reply, repeated host-editor reads plus synchronous UI work can block frame drawing and touch response. This is a **hypothesis from source and video**, not a measured Samsung stack trace.
- Candidate 400 puts physical Space in the alphabet ACTION_DOWN order queue; when a Spacebar drag turns into cursor movement, the unused Space token previously remained pending until ACTION_UP. Later letters can queue behind it. The research now releases the token on the first confirmed cursor MOVE.

## Narrow Research 401 changes

1. `TypingRefreshGate401` models a **96 ms, non-resetting**, editor/session-scoped refresh window. The **first** typed letter or normal Space starts exactly one pending suggestion update, and subsequent presses during this window do not schedule more updates. This is bounded rather than 'wait until the user stops'; continuous typing still gets periodic predictions. A token holds only a short-lived InputConnection reference and session number, **not typed text**, and is fully cleared on session/keyboard lifecycle changes.
2. Existing editor writes and composition verification stay unchanged (no retries, no duplicate commits). The expensive suggestion refresh is deferred from the raw key handler; any selection echo or Shift-label redraw while the window is active also refrains from invoking the full suggestion pipeline. The dispatched callback only runs against the exact live editor/session and re-reads the latest state. Prediction/space highlight generations are invalidated at the start, so outdated visual suggestions cannot be committed merely because they remain visible.
3. Password, private, numeric, symbolic and selection fields bypass this coalescing; original privacy/control behavior remains. Text is not logged or sent to network.
4. Once a genuine Spacebar cursor drag is recognized on MOVE, its ACTION_DOWN order token is **cancelled immediately**, releasing later queued alphabet presses. A canceled touch never inserts a Space. Ordinary physical Space taps retain Candidate 400's exact down-order sequencing.
5. Adds an **opt-in in-memory numeric** timer for the deferred suggestion refresh to the existing Candidate diagnostic report. No editor characters, suggestions, apps, touch positions, wall-clock timestamps, analytics or input history are stored. Existing V4 data layout is untouched.

## Qualification

- Pure Java deterministic randomized replay: 60k multi-key burst windows, 60k stale-session/changed-editor cases, 100k privacy combinations; explicit assertion for bounded 32-120ms policy and no lingering editor reference.
- Android 35: 5 new `TypingRefreshGate401AndroidTest` tests; full previous suite **at least 304**, no failures/errors/skips.
- Compare file changes against **exact released Candidate 400** SHA; only `RebornImeService.java`, `CandidateLatencyMetrics.java`, new gate, two tests, this document and workflow. No gesture geometry, predictor ranks, keyboard layout, persistent learning, V4 import/export, manifest, package or permissions changes.
- The previous Candidate 400 gesture/Space queue mechanics are still safety checked in the 299 existing tests. This change is separate from any gesture UX claims.
- Research branch: **NO APK, NO prerelease**. If qualified, build a **different installable test package**, not an upgrade of 400.
- **This is not yet proven to fix Samsung lag.** Real-phone tests in notification-shade WhatsApp reply and ChatGPT, with background music then without, plus optional timing lab, are required. Gboard remains the safe daily driver until that validation.

## Limitations / next investigation

The editor transaction still performs synchronous InputConnection verification because older remote editors sometimes report failure after already inserting text. Removing those checks without a distinct host behavior test could duplicate or lose characters. This research prioritizes reducing unrelated UI work without weakening text correctness. If the phone still freezes, next instrument the time spent inside remote InputConnection versus suggestion rendering and consider a separate, explicitly tested fast-editor strategy, not speculative "music fixes".
