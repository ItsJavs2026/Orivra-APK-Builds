# Orivra Research 388 — Next-word prediction gestures on actual letter rows

Based on passed Research 387 `f88b21923fab5c5b70863089a46ed6e2720834b6`; Research 387 fixes center-only DOWN recognizer arming. **Not an APK and not merged into installed Candidate 386**.

## User's demonstrated problem

Samsung S26 Ultra screenshot shows `I | The | Hey` in the suggestion strip after a word boundary. User reports that swiping across the letter row does nothing in that state. In Candidate 386, the letter-row recognizer explicitly required `!latestSnapshot.isEmpty()`. Empty composition is normal for **idle next-word predictions**; the gesture never started even though the strip was visible and valid.

## Narrow correction

`LetterRowIdleMode388` recognizes either:
1. COMPOSING with unchanged nonempty input, retaining the exact Research 386 correction path, or
2. NEXT_WORD with empty composition, at least one selectable live chip, and a **current verified NextWordTapTicket340** that captures the caret, before/after editor text and input session.

On release the gesture rechecks: original InputConnection, session, pointer/press serial, selection, no private/numeric/mid-edit field, no change in current composing/caret word, live matching candidate in the same slot, and **NextWordTapTicket340.current**. This routes to the *existing* `acceptSuggestion(...,"",choice)` next-word branch which inserts `choice + " "` once, at the existing safe end-of-document boundary. It does not call the Space key handler separately or change prediction ranking. The ordinary active-word correction still uses the prior single-boundary `SentenceSuggestionCommit` path.

This is an explicit gesture, not automatic word insertion or sending chat text. Gestures remain OFF by default when installed on a new app. Preserves letter row modes, long-press symbol behavior, two-thumb cancellation, backup/privacy settings.

## Tests

Pure Java 80,000 randomized idle-state and pointer-direction checks, plus six Android 35 test cases checking the `I | The | Hey` display, real `BaseInputConnection` at `Hello ` word boundary, invalidated NextWordTapTicket after text changes, no arm within unfinished words, unchanged live-composing path and center-only idle case. Run existing Research 387 249 Android tests plus six = at least 255, no failure, skipped or errors. No release workflow.
