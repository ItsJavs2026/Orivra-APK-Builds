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

## Memory and long-session lifecycle audit (expanded per user request)

**Source findings, not Samsung heap measurements:**
- The built-in predictor lexicon and maps are fixed-size per process; the optional app-icon hue cache already held up to 40 hue entries and recycled its short-lived 64×64 bitmap. However, the pending worker requests were not bounded by a separate concurrency cap, so **Research 401 caps icon requests at eight in-flight operations**, retains the existing 40-entry LRU, and clears optional cached hues during hidden-process trim.
- `PersonalNextWordModel` session maps have existing ceilings (4096 contexts, 64 continuations/context and 8192 words) but had only a partial hide-lifecycle reset. `releaseInactiveSuggestionMemory401()` now clears transient word/session maps, prefix cache, split lists/snapshots, prediction/options, cursor editing tickets, and delayed Rise actions when the keyboard view hides or the input/service exits. It does not clear private learning, composing text, or backups. This reduces retained objects between status-bar replies.
- `TypedWordLearningWorker346` was an opt-in single background writer but could enqueue an arbitrary number of successful Space-learning updates if disk processing fell behind. **Cap pending optional Space-learning tasks at 128**. On saturation, suspend *only optional training for that editor*, safely reset its pair chain on the worker, and resume automatically on the next editor. The actual Space/letters still commit through the IME's original checked InputConnection path; saved word counts/V4 backups are not deleted. Reject worker submissions after shutdown without crashing or retaining a private word task.
- `OfflineLanguagePackWorker` is opt-in, asynchronous, and closes workers on service exit. Its 78,942-word optional pack can be retained while the service stays alive; `onTrimMemory` now releases it **only when the keyboard is hidden**, along with the small optional next-word seed object. A future field activation can load it again.
- The UI and background-predictor threads are distinct; the predictor cancels superseded jobs and closes on service destruction. The remaining **synchronous remote InputConnection** reads/validation must be profiled separately. A good heap number does not establish good notification-reply latency.
- Candidate performance lab keeps user-opted-in **numeric-only** Java used/max heap, peak sampled heap (every 16 key events), current native allocations, key handler and deferred suggestion p95. These are **not full process PSS, not GC-trace evidence**, and never include typed text.

**Additional qualification checks:**
- Android 35 tests repeatedly generate 13k synthetic committed words and verify per-session maps stay within stated caps and `clear()` empties them; 16k synthetic prefixes verify one-prefix cache replacement; 32 key timing samples verify the opt-in heap report and its OFF-state reset.
- Two Android tests verify icon color cache trim and that 300 distinct app requests never exceed eight in-flight and forty cached hue values.
- Typed-learning worker stress test holds the background writer and emits **5000 Space-boundary events**, verifies the 128 pending-learning limit, safe overload, no unintended persistence, and fresh learning in a subsequent editor.
- Full suite now requires **at least 310 Android 35 tests**, zero failures/errors/skips. This research branch still creates **NO APK**.

## Qualification

- Pure Java deterministic randomized replay: 60k multi-key burst windows, 60k stale-session/changed-editor cases, 100k privacy combinations; explicit assertion for bounded 32-120ms policy and no lingering editor reference.
- Android 35: 5 coalescing tests plus 3 memory tests, 2 icon-cache tests and 1 typed-learning saturation test; full previous suite **at least 310**, no failures/errors/skips.
- Compare file changes against **exact released Candidate 400** SHA. Changes are restricted to `RebornImeService`, `CandidateLatencyMetrics`, `TypingRefreshGate401`, `AppIconHueCache366`, `TypedWordLearningWorker346`, their tests, this document and workflow. No gesture geometry, predictor ranks, keyboard layout, persistent V4 import/export grammar, manifest, package or permissions changes.
- The previous Candidate 400 gesture/Space queue mechanics are still safety checked in the 299 existing tests. This change is separate from any gesture UX claims.
- Research branch: **NO APK, NO prerelease**. If qualified, build a **different installable test package**, not an upgrade of 400.
- **This is not yet proven to fix Samsung lag.** Real-phone tests in notification-shade WhatsApp reply and ChatGPT, with background music then without, plus optional timing lab, are required. Gboard remains the safe daily driver until that validation.

## Limitations / next investigation

The editor transaction still performs synchronous InputConnection verification because older remote editors sometimes report failure after already inserting text. Removing those checks without a distinct host behavior test could duplicate or lose characters. This research prioritizes reducing unrelated UI work without weakening text correctness. If the phone still freezes, next instrument the time spent inside remote InputConnection versus suggestion rendering and consider a separate, explicitly tested fast-editor strategy, not speculative "music fixes".
