# Orivra Candidate 345 — Samsung video-guided normal bar fix

## Evidence
User-provided Samsung screen recording `85088.mp4` (~23 seconds,
~60 frames/s, 1080×1128) shows repeated transitions in the **ordinary
three-chip prediction strip** during fast typing/backspace. For example,
between 4.0 and 4.9 s, one visible chip alternates with three, along
with noticeable changes in chip backgrounds and labels. The recording
does not show a Rise panel as the primary problem.

## Root cause in Candidate 344
The first hotfix removed the "Keep typing" placeholder (343).
Candidate 344 retained the same three TextView objects when the
prediction count changed, but its **intermediate immediate render** still
called `setVisibility(INVISIBLE)` on both unfilled side chips and cleared
their displayed words. Shortly afterward, the asynchronous model result
called `setVisibility(VISIBLE)` and rebuilt the chip labels. Thus view
identity remained stable while *pixels still disappeared/reappeared*.

## This isolated fix
- A dedicated preliminary render path identifies the immediate
  single-word result before the asynchronous result is ready.
- Only when the same non-private editor has just received a complete
  result within 500 ms, and the current short word extends/backspaces
  the previous word by <=1 character, visually preserve the two
  previous side-chip labels and background until new results arrive.
- Those interim visual labels are **not tappable or accessible**:
  click handlers and content descriptions are cleared. A new current
  result rebinds tap handlers after its live input guards.
- When normal suggestions are temporarily empty, reuse the same
  three hidden TextViews instead of removeAllViews()/recreate.
- Never apply this treatment to sensitive editors, long sentence
  layouts, a new word, a new editor session or stale data beyond
  500 ms. No raw typing saved or transmitted.
- Preserve previous user-requested automatic Rise, split predictions,
  Backspace fixes and V1–V3 backup behavior.

## Verification plan
Previously-qualified 98 Android emulator tests plus six new tests:
same-word eligibility, side-chip visual continuity with stale clicks
disabled, new/private-editor exclusion, quiet-row reuse after
backspace, empty-chip rejection and 400 rapid simulated UI updates.
Emulator qualification is not proof of Samsung Android 17 smoothness.
Candidate 344 remains installed independently for rollback.

This is a targeted UI correction, not a ranking change or a rewrite
of the offline sentence-prediction engine.
