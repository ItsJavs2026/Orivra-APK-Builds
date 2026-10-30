# Research 321 — learn from accepted fixes AND successfully used words

Samsung phone feedback: Candidate 319 feels more responsive. Preserve all touch
changes and the source-qualified English dictionary.

## Adaptive learning behavior

Existing preference `orivra_personal_learning_v1:enabled` stays opt-in.
Successful tap-accepted corrections still form `accepted:from:to` counters
and keep their exact V1 export/import structure.

NEW: When an explicitly typed, composing English word is finalized using
Space, capture ONLY a closed-list preceding word and a known dictionary word.
Record a bounded `used:previous:word` count after the editor successfully
accepts the space and remains in the same input session.

- Two explicit taps of a context suggestion -> still ranks.
- Three finalized uses of a known next-word after a safe context -> ranks.
- One prior explicit accepted tap + two successful ordinary uses -> ranks.
- A once-tapped typo correction like `tekk -> tell` can be promoted only
  when `tell` is later typed and committed in safe contexts at least twice.
- Ambiguous competing accepted corrections abstain. No silent rewriting.
- Private fields, selected text, mid-document edits, URLs, addresses,
  raw sentences and unapproved new words never enter the learner.
- Clearing Learning erases both old accepted pairs and new word-use counts.
- No Internet, no telemetry, no ambient keystroke logging.

The new `used:` counters are local and NOT YET in V1 export. Build 321
doesn't modify the user's working import/export format. Follow-up will
introduce a backward-compatible, explicit export of contextual learning
only after strict format and privacy regression tests.

CI: Java unit + 5k known-word guard + Android compilation +
Android 35 emulator privacy and behavior tests.
