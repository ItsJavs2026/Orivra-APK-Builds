# Orivra Research 380 — Overlapping letter + Space press order

**Research only. No APK or production InputMethodService changes.** Branched from the released **Candidate 378** commit `1df1e3065e7fabe939633ec75dfdd42022481304`.

## Observed source-level ordering risk

Candidate 378 already sequences overlapping alphabet presses with `HintKeyPressOrder353`, committing letters in ACTION_DOWN order only after each accepted ACTION_UP.

Space's listener is different: `attachSpacebarCursorControl` inserts its accepted Space immediately at ACTION_UP, while alphabet completion uses a separate queue. When letter A is pressed, then Space is pressed, but Space is released first, Space can be applied before A. This is a **source-level scenario**, not a reported Samsung incident or measured production error rate.

The user installed Candidate 378 and reported **“Actually it's good so far!”**; preserve that provisionally successful Samsung trial instead of introducing an unrequested behavioral change.

## Proposed pure-Java policy

`MixedThumbOrder380` is a bounded, memory-only sequencing barrier for typed letters and accepted Space taps. It models accepted actions in ACTION_DOWN order, even if the thumbs release in the opposite order. A recognized Space cursor swipe supplies no insertion action; cancellation, input-session reset, duplicate release and malformed event streams fail closed. 20,000 randomized sequences stress out-of-order completions.

This policy is **not wired to Orivra**. Integration would require additional analysis of InputConnection behavior, symbol holds, Backspace and spacebar cursor movement, editor changes, predictive Space acceptance, and release timing. No on-device impact is claimed.

## Baselines and next gates

- Candidate 378's Android 35 build: **234/234 tests passed**, released to GitHub and user installed it.
- Research 379 exposed a single exact-pixel assertion failure in the expanded Android 35 suite (240/241 passed). Its test assertion was corrected for a <=1px trailing rounding residual and the rerun is being checked separately.
- Candidate 378's existing touch and edge settings are unchanged, as are V4 backup, protected fields, signing/package, Space cursor swipes and top-bar gestures.
- Tomorrow: top prediction bar is its own workstream. Do not mix that feature with Research 380.

No network permissions or persisted typing/gesture traces are added.
