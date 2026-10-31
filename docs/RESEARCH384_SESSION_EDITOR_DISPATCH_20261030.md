# Orivra Research 384 — Bounded session-aware editor-action ordering

Research-only. This branch starts from fully qualified Research 383 `1500f24a0f4aa499ef83b6891bad2628c6ea6bb4` (242 Android 35 tests passed). No changes to installed Candidate 378 or its permissions, package, signing, learning, settings, or layout.

## Purpose

Candidate 378's alphabet presses have a transient down-order queue but accepted mutations are later posted to the main handler; Space inserts immediately at release and may accept highlighted suggestions. Research 380–383 established that any mixed-key integration must order **actual editor mutations**, not only finger-up callbacks, and must reject stale actions after focus switches.

This branch adds `SessionBoundEditorOrder384`, a pure-Java research class **not connected to the IME**. A single FIFO poster sequences accepted actions after all preceding press decisions resolve. Each action is guarded at execution against the current input session and editor instance, and the entire queue has an epoch so already-posted callbacks become inert after cancellation. The bounded queue fails closed after 24 outstanding actions; duplicate releases, canceled Space cursor drags, refused posts and detached editors do not insert phantom text.

## Deterministic checks

- Reversed letter–Space lift preserves `"a "` actual mutation order.
- No editor mutation in a touch-event callback (actions posted).
- Switching input session/editor invalidates already-posted actions.
- `cancelAll` invalidates posted but not yet executed callbacks.
- Cancelled cursor-swipe Space does not type.
- Queue overflow clears stale tokens; duplicate releases fail closed.
- A refused callback post clears pending work.
- A stale editor predicate throwing `RuntimeException` fails closed.
- 50,000 randomized release sequences test FIFO ordering and bounded-drain invariants.

**Not a production or Samsung test.** Actual integration still needs real InputConnection editor-session binding, secure fields, proper Space highlighted-correction execution, Backspace and Shift behavior, predictive strip updates and no increase to perceived key latency. Candidate 378 was phone-installed and provisionally praised by user; do not silently integrate the experimental dispatcher. Top-bar remains a separate tomorrow-morning feature.
