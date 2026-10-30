# Orivra Research 319 — contextual learning from accepted words

User goal: If Orivra learns a new word, it should learn HOW that word was
used in a sentence, not only that the word is spelled correctly.

This isolated research build uses only explicit tap-acceptance events:
- A confirmed correction `please tekk` -> `please tell` may learn
  the small relationship `please: tell` if accepted `tell` is an approved
  public vocabulary entry.
- Explicitly accepting a next-word chip records the same relationship.
- Accepting an already-decoded multiword sentence may record *only*
  adjacent safe-word pairs, never the sentence or user text.
- A previously unknown word can participate only if manually saved in
  the personal dictionary and subsequently explicitly accepted.
- Requires the existing Learning toggle ON, at least two reinforcements
  before surfacing the word in idle next-word suggestions, and a safe editor
  without personalized-learning restrictions. No passive keyboard logging.
- Prior context is a short, closed allowlist of common function and
  conversational words. Names, locations, addresses, passwords and URLs
  are not admitted as previous-context keys.
- At most 240 two-word count records, capped per record. Local app-private
  SharedPreferences, no internet, no telemetry, not full raw sentences.
- The existing Settings CLEAR learning action clears these new context pairs
  too; toggling Learning OFF prevents reading and writing them.
- Existing ORIVRA_LOCAL_LEARNING V1 backup format is unchanged: context
  pairs are deliberately excluded from backup pending privacy review. Do not
  claim portable context learning yet.
- The initial implementation learns preceding-word relationships only; it
  does not yet store arbitrary right-side context or automatic next words
  typed after an unaccepted suggestion.
- A contextual suggestion remains an explicit, tap-only choice. Never
  automatically inserts or rewrites content.

Requires Java policy tests, Android source compilation, Android 35 emulator
regressions and on-device Samsung testing before it can be called ready.
