# Research 334 — reduce false learning from mid-word pauses

Phone baseline: Candidate 333 installed by user, but no 333 test report
yet. Do not modify its released APK.

Candidate 333 can learn a stable full word after 700ms without Space.
However, frequent short words can also be partial prefixes of longer ones,
e.g. 'con' while writing 'continue', 'can' while writing 'candy'.

Improve *evidence quality*, not autopredict:
- Two- or three-letter words require a 1600ms stable idle interval.
- Four to five letters: 1100ms; six: 800ms; seven or more: 550ms.
- Explicitly accepted suggestion taps and successfully committed Space
  still count immediately.
- A hidden/dismissed keyboard cancels pending observations.
- Do not count suffixes of letters, digits, or apostrophe-connected tokens.
- No changes to numeric/banking privacy or backup formats V1/V2/V3.
- Never claim external app Send succeeded: only the keyboard's visible
  stable word is considered, with session/editor checks.

Regression: Java timing and boundary tests plus complete Android 35 suite.
This is research, not an installable release.
