# Orivra Research 398 — in-place Shift updates without keyboard teardown (NO APK)

Baseline: Research 397 `ffecb157d117862c668ed403f88009a09eab765a` passed **290/290** Android 35 tests. Separately released Candidate 396 remains the user's installed test build and feels initially smoother than 391; extended reply testing is ongoing. Do not imply Samsung 17 validation or that 396's remaining lag is fixed.

## Source audit and hypothesis

`RebornImeService.refreshView()` currently calls `keys.removeAllViews()` and reconstructs all letter and control Buttons, touch listeners, hint overlays and layout rows. It is appropriate for full layout/theme/numeric changes, but it is also reached for a *Shift-only* toggle after ordinary typing, after sentence-boundary Space, and on Shift's own release. These full rebuilds are unnecessary when only the displayed ABC capital/lowercase labels change. A potential benefit is less UI-thread work and fewer touch-target detachments; actual Samsung latency impact still needs measurement.

## Minimal change

- `AlphabetCaseRelabel398.relabel` validates the entire, already-mounted 4-row ABC tree in one pass **before** changing anything. It requires the known shifted third-row controls, FrameLayout letter cells, Button positions, exact public hint labels, and valid lowercase/uppercase key labels.
- Only after complete validation, update changed Button texts, associated accessibility descriptions, and the Shift/Lock indicator. No view removals, new controls, new input listeners, layout-geometry edits, personal word reads, network, editor writes, or synthetic click events.
- Route only the two explicit Shift actions and two deferred case-change callbacks through `refreshShiftCase398`. Reuse existing pressed-key/spacebar/backspace guards. If another letter press remains open, defer to the known-safe existing full refresh on release. If the mounted layout is unrecognized, fall back to unmodified `refreshView()`.
- Preserve the original `refreshView` for editor activation, numeric/symbol layouts, orientation, size, theme and other changes. Preserve its `showSuggestions()` behavior on Shift change.
- No changes to word ranking, V4 backup/import, learning, gestures, editor privacy, package, permissions or Candidate 396 APK.

## Required evidence before a future test APK

1. Four Android 35 instrumentation tests verify lower/uppercase text + accessibility, caps lock, all-or-nothing fail-closed handling, 1200 changes with original pressed-state/listener/parent preserved. Run all earlier regression tests: **>=294**, zero failed/errors/skipped.
2. Source guard requires exactly the expected 5 files changed vs passing Research 397, and independently verifies all learning, gesture, dispatcher, prediction and package files unchanged.
3. Normal typing, keyboard mode switching, long-press Shift and simultaneous-thumb gestures still require **Samsung Galaxy S26 Ultra Android 17 phone testing** after an independently qualified later candidate. The test fixture is not a full IME dispatch trial.
4. This branch is **research-only**. CI compiles and runs tests but does **not** release/publish an APK.

## Subsequent device diagnosis

Use Candidate 396's optional in-memory performance lab to collect just numeric key-handler and suggestion-view p95 with normal text and long replies. Avoid collecting any private typing or account content. If 398 passes, decide on a separate successor trial after reviewing actual phone behavior; preserve Candidate 391, Candidate 396, and V4 backups.
