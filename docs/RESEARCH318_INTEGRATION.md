# Research 318 — integrated English + touch responsiveness

Isolated integration of source qualified independently in 316 and 317. Existing
Candidate 310 remains the only phone-verified working APK.

- 315: ordinary word + contraction correction; `tekk -> tell`.
- 316: low-ranked everyday dictionary additions; 627 -> 780 of 1000
  reference words, 487 synthetic benchmark cases with no group regressions.
- 317: release-side letter commit before animation, key feedback 32ms down,
  56ms up and reduced key scaling, plus skip full layout rebuild on
  normal spacebar when Shift stays the same.
- The Google Search, Enter, Backspace gestures, spacebar drag, editor privacy,
  opt-in Learning V2.1 and personal dictionary paths are unchanged.

Research 318 must pass Java tests, Android compilation, full Android 35
emulator tests and pinned vocabulary A/B before any phone trial. The improvements
do not guarantee measured Gboard latency parity on Samsung hardware.
