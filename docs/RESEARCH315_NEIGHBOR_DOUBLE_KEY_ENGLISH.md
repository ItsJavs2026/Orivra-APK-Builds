# Orivra Research 315 — everyday English, QWERTY repeated-neighbor correction

Source: **real Samsung Build 310 test:** `tekk` should suggest `tell`.
The underlying lexicon did not contain the very common verb `tell`.
Research 315 restores `tell / tells / telling / told` to the general English
lexicon rather than memorizing a specific one-off typo.

## Generalized correction rule
If an unknown English word has **two identical final letters**, try the
immediately adjacent key on the same physical QWERTY row for both letters.
Require the preceding letters to be unchanged, a recognized offline word,
and exactly one supported choice (otherwise abstain).
Examples `tekk` -> optional `tell`; `wikk` -> optional `will`.

This conservative option applies both to normal SentenceFlow suggestions and
Google-like committed-text editor caret suggestions. The original typed
literal stays available; committing is always a deliberate tap. Passwords,
private editors, no-suggestions fields, selection and stale cursor safety are
unchanged.

Existing Build 310 Google Search fix protected; no phone APK published by
this research branch. Include 315 + 314 tests and broad 487-sample English
suite before offering a new installation.
