# Orivra Research 328 — frequent short words and early prefix predictions

User's three screenshots show a need to offer frequently typed words before
the user finishes entering all their characters. The existing 327 learning
only remembered adjacent safe word-pairs; standalone "continue" could not
strengthen itself without that surrounding context.

Source changes:
- New bounded opt-in standalone completed-word counts, eligible only in
  ordinary nonprivate editors, recorded after successfully committed Space
  using an active composing span in the same editor session.
- Explicit tap on a known word reinforces its frequency twice. No raw
  keystrokes or arbitrary clipboard/previous sentence persistence.
- After 3 confirmed uses: 3+ letters of "continue" may offer "continue".
- After 5 uses: 2-letter prefix may offer the completion.
- After 7 uses: 1-letter prefix for frequently used 2–4-letter words
  such as "my" or "can"; longer words cannot flood one-letter UI.
- Already completed, correctly spelled short words keep their original
  first suggestion; no automatic rewrites, tap to accept only.
- Immediate UI candidate path and async full-decoder merge retain the
  user's repeated-word choice without overwriting word boundaries.
- Learning OFF or a clear resets/hides the new store. No banking-field
  learning; numeric keypad unchanged.
- Word counts local and bounded to 320 words, no INTERNET permissions.

NOT YET PORTABLE in V1 or V2 export; before releasing this behavior widely,
a new validated export extension is required. Candidate 327 is untouched.
Tests are synthetic; actual Samsung input feel still requires user checks.
