# Orivra English Research 313 — committed editor corrections

This branch is intentionally based on fully CI-qualified Research 311 (which
includes the Samsung-reported Google Search fixes from Candidate 310).
Research 312 bilingual changes are separate and must not destabilize English.

Problem: in some search/note editors the host commits every key, so the IME
composition buffer stays empty. Previously the keyboard offered "were/we're"
but not ordinary typo candidates such as "helo/hello" at the end of text.

Fix: read the *live* full word at the caret; show literal-first immediately,
then request the existing off-thread word corrector. Only words meeting a
strict English token and UI/privacy eligibility rule qualify. Edits still
require a deliberate tap on a valid current-editor snapshot; no automatic
replacement, no background learning, no network, no revision of Enter/delete
and spacebar gesture code. Preserves next-word suggestions after spaces.

Also preserve "I'm" and "I've" capitalization when a lowercase incomplete
word receives a manually tapped suggestion in no-composition editors.

Qualification:
- Pure Java policy and legacy regression tests.
- Compile Android IME and Android instrumented test source.
- Android 35 emulator and live Samsung Android 17 checks before shipping.
- Record 24-hour observed examples to drive stronger English SentenceFlow,
  rather than claiming benchmark accuracy from an isolated screenshot.
