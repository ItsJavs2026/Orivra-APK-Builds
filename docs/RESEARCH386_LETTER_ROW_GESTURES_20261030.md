# Research 386 — Prediction swipes on the actual QWERTY and ASDF letter keys

**Corrected request:** Candidate 385 made the prediction strip bigger, but the user wanted gestures starting on QWERTY or ASDF **alphabet keys**. Do not present Candidate 385 as this feature. Keep Candidate 378 installed and unchanged.

## Layout recommendation and optional settings

Start with **ASDF middle row only** because it is closer to a relaxed two-thumb position and farther from the prediction strip. This is an ergonomic hypothesis, NOT a measured Samsung result. Allow OFF -> ASDF middle only -> QWERTY top only -> BOTH -> OFF so the user can test which works.

## Gesture contract

A gesture begins with one pointer on an enabled letter row:
- Swipe LEFT >=46dp, predominantly horizontal: choose physically left suggestion.
- Swipe RIGHT >=46dp: choose rightmost visible side suggestion.
- Swipe DOWN >=42dp, predominantly vertical: choose exactly **physical center slot (index 1)**. No fallback to a different side suggestion.
- Direction reversals, wrong pointers, cancellation, invalid candidate, secondary simultaneous finger, symbol holds, selection changes, stale InputConnection/session, secured/numeric editors and changed candidate fail closed.
- A regular short tap still types the pressed letter once. A gesture must not also type its initial letter.
- This is an *explicit suggestion selection*, not swipe-to-spell or a send-message gesture.

## Downward gesture and Space rules, including tiny words

**Do not implement this as suggestion insertion + a separate `space()` or literal `commitText(" ")`.** The single action must call Orivra's existing `acceptSuggestion` once, which uses `SentenceSuggestionCommit.insert` for live composition. The latter verifies the active input matches the current word and appends `SuggestionContinuity353.spaceAfter` in the SAME editor commit. This is the same separator logic used by Space's existing highlighted-word acceptance. It adds one boundary when needed, no extra separator before existing spaces or closing punctuation, and it supports **one-letter, two-letter and three-letter inputs** on explicit selection, including prefix completions and unchanged center words. The explicit gesture can choose the center suggestion even if it is not highlighted; this deliberate selection does not weaken the normal Space-key's `HighlightedSpace372.eligible` auto-accept guard.

Examples to test: composing `I` -> center `I will` -> `I will `; `t` -> `the` -> `the `; `te` -> `tell` -> `tell `; `a` -> `a` -> `a `. If a space already follows, do not double it; if punctuation follows, do not put an extra gap before the punctuation. The user requested identical *spacing semantics* to pressing Space, while the DOWN choice is an explicit force-selection.

## Limits

Research only; not released, Samsung-tested, or merged with Candidate 385. The existing Space key remains unchanged, and V4 learning, typing privacy and no-INTERNET are preserved. Qualify with pure Java randomized gestures, one/short-word boundary tests and Android 35 editor tests; then consider an independent APK for Samsung comparison.
