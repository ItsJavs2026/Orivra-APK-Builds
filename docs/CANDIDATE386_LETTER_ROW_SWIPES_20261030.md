# Orivra Candidate 386 — Actual letter-row prediction gestures

Package: `com.chroomareborn.keyboard.build386trial`, versionCode 386. This is a separately installable debug-signed trial based on Research 386 commit `7981d8ca803e4040a6ebc051b0f9cbed514b7f24` (source originally based on the installed Candidate 378). It does **not** reuse Candidate 385's prediction-strip-size experiments or integrate the Research 380–384 experimental mixed Space barrier.

## Actual feature

In Candidate 386 Settings, the **Swipe predictions from letter rows** switch cycles OFF (safe default) → **ASDF middle only (recommended initial comparison)** → QWERTY top only → both rows → OFF. The suggestion-strip swipe setting remains independent.

With a visible live composing word and available verified candidates, perform an intentional gesture from a letter key on an enabled row:
- LEFT → first visible candidate
- RIGHT → rightmost valid visible side candidate
- DOWN → EXACTLY the center suggestion (physical slot 1). Missing center means no commit
- A short ordinary TAP continues to type a character exactly once
- A deliberate accepted swipe does not also type the starting letter

The DOWN action **reuses existing `acceptSuggestion` and `SentenceSuggestionCommit.insert`**, which applies `SuggestionContinuity353.spaceAfter` exactly ONCE. It does NOT manually press Space or transmit the chat message. Single- and tiny-prefix examples with a missing suffix: `t`→`the `, `te`→`tell `, `a`→`a `, `I`→`I will `. If a separator already follows, do not duplicate; before punctuation, do not invent extra spaces. Standard Space key's separate highlighted eligibility remains unchanged; DOWN is an explicit selected-center action.

Safety: OFF by default, no extra permissions, no network or telemetry, no automatic V4 data migration. Cancellations, overlapping fingers, failed recognizer, stale editor/session, changed selection, mid-word caret edits, protected/numeric fields, symbol mode and changed candidate fail closed. Existing letter-key fat-thumb geometry and hold-symbol rules are preserved.

## Qualification and phone testing

Research 386 synthetic: 125,065 assertions across 100,000 gesture cases, including short Space-prefix and boundary regressions, passed. A separate Android 35 research run is in flight, then this candidate workflow **independently** requires >=244 tests with zero failures/errors/skips, package/app label version check, signature, dictionary and no INTERNET. Publication is impossible without full green workflow.

After a verified GitHub prerelease, keep Candidate 378 installed. Install Candidate 386 separately, try ASDF-only first on a disposable note, then QWERTY-only, including one-letter and short-word completions. Check two-thumb overlap, symbol holds, accidental sloping taps, long typing and unwanted double spaces. Android emulator qualification is NOT Samsung Galaxy S26 Ultra Android 17 phone verification.
