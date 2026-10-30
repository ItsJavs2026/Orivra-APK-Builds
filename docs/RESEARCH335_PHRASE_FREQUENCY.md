# Research 335 — accepted sentence also teaches individual word choices

Gap found in Candidate 333 source:
- Explicitly accepted full-sentence suggestions already teach adjacent
  word pairs through PersonalContextLearning319.recordChosenPhrase().
- Individual word frequency was only recorded with recordTapped(word),
  which rejects phrases containing spaces. Therefore an explicit tap on
  "Please continue" did not reinforce the standalone word "continue"
  for subsequent con/cont completions.

Fix:
- Record eligible distinct words from each explicitly accepted suggestion
  only after the editor has successfully committed the suggestion.
- A single tap counts each eligible word once, even if repeated in phrase.
- Skip I/a as one-letter words, never retain complete phrases, URLs,
  numbers, arbitrary punctuation or private text in frequency store.
- PersonalWordFrequency328.recordTapped continues to check the public
  lexicon and manually saved dictionary.
- Existing opt-in Learning, clear, V1/V2/V3 backups, banking privacy,
  and touch performance are preserved.
- Emulator test uses synthetic accepted phrases, validates V3 export,
  opt-out, and reimportable word counters.

Candidate 333 remains untouched while the research branch qualifies.
