# Orivra Research 374 — short Space completion and long-draft continuity

Date: 2026-10-30. Baseline: separately installable Candidate 373
(`candidate/build373-prediction-swipes-space-accept-20261030`).

## Phone-reported symptoms (not closed)
- One-, two-, or three-letter highlighted completions are sometimes refused
  when Space is tapped. Exact final highlighted prediction, editor type and
  composing state still need physical phone verification.
- In longer messages, prediction choice appears to collapse to a centered
  single chip containing the current word. Keyboard switching did not
  restore useful suggestions. The screenshot is consistent with an
  already-committed caret word; it is **not** proof of a fixed character cap.

## Confirmed code facts
- `HighlightedSpace372.eligible()` previously rejected all
  `expected.length() < 2` and rejected long one-word prefix completions
  through a three-character maximum length difference.
- Even when a different option is visible, `SuggestionVisualStyle.highlight`
  is a **display rule**, not model confidence; a final rendered highlight
  alone does not certify language accuracy.
- `RebornImeService.showSuggestions()` has separate
  `composing.isEmpty()` and active composing branches. A committed caret
  word routes to `CommittedEnglishCursorPolicy.immediate/merge` and
  `CursorWordCorrectionPolicy.safeOptions`; the current word is centered
  with potentially blank side chips. This explains how a single chip can
  appear without proving why the host stopped composing.
- `onUpdateSelection` clears composition if Android reports a missing
  composing candidate range or selection moves outside that range. A
  host-editor behavior change is a plausible hypothesis, **not proven**.
- `LongDraftPrediction358` has fallback suggestions for a verified live
  composing word, but the committed-word correction policy intentionally
  rejects phrase/next-word options. The two edit paths must not be mixed,
  or a next-word suggestion could accidentally replace an existing word.

## Scoped change
Research 374 changes only the `HighlightedSpace372` pure eligibility
policy. A *final, visibly highlighted*, safe one-word prefix completion from
one to three letters can be accepted by the already existing single-commit
Space path. No new InputConnection code, touch path, haptics, private field
handling, dictionary, V4 learning, or display rendering is changed.
Multiword predictions still require deliberate tapping or swiping.
Settings toggle retains the behavior opt-out.

## What is NOT fixed yet
Long-draft loss of useful predictions. Before changing editor composition,
we need tests that simulate Android host selection callbacks with
`candidatesStart/End == -1`, committed word at end of document,
and transitions after longer messages. Fixes must **not** silently extend
a rejected composing span, delete surrounding text, or show next-word
chips through a single-word replacement tap handler.

## Later top-row swipe experiment
User prefers reviewing this in a later session. Feasible as a separately
toggleable gesture on a keyboard key row, but it must discriminate
finger drag vs normal keys (touch slop, direction, one-finger ownership,
two-thumb cancellation), preserve the 320ms special-character hold and
Backspace/spacebar controls, and be measured on Samsung Android 17.
**No top-row swipe code in Research 374.**

## Release discipline
This research branch is NOT an APK. Candidate 373 is unchanged. A phone
trial is appropriate only after full Android regression qualification and
separate package identity/rollback verification.
