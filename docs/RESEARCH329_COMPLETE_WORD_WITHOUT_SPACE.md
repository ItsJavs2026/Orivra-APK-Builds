# Research 329 — repeated words in messages sent without Space

Important user workflow: "continue" is often the entire message. A phone
Send button can be pressed without a trailing Space. Research 328 only
counted typed words at a successfully committed Space boundary.

Research 329 adds opt-in stable completed-word observations:
- After an ordinary text word remains exactly the current composing span
  and cursor content for 700 ms, count one completed recognized word.
- Every new keystroke cancels a pending observation. There is no per-key
  logging, arbitrary sentence recording, unsaved-name capture or network.
- The same editor and IME session are verified before storage.
- The already-observed word is not counted again on a following Space.
- The existing conservative frequency thresholds (three, five, seven
  uses for 3+, 2, 1 character prefixes) remain unchanged.
- Bank/PIN fields, private editors, selections, mid-document editing and
  opted-out learning cannot store word evidence.
- App Send buttons are not observable with guaranteed success by an IME.
  The timer records a stable *typed* complete word, not a verified message
  submission. Tests must not claim more.

This source research branch does not alter published Candidate 327.
The new word-use counts still require a portable V3 backup extension
before widespread use across frequent test app installs.
