# Orivra Research 338 — last-character Backspace bug

User's five screenshots in Google Messages on installed Candidate 333
show a word (e.g. asking), then a single remaining 'a', a transient
"Keep typing • corrections appear here" row, and an extra Backspace
needed to remove the final letter.

**Code-confirmed root cause:** RebornImeService.backspace() shortened
the composing buffer to empty and then called finishComposingText().
On Android, finishComposingText() only removes the composing marker;
it does not delete the final character. The next Backspace then
deleted the now-committed character. That also explains the sudden
empty-prediction status while the letter remained visible.

Fix:
- For a single remaining composing code point, call
  InputConnection.setComposingText("",1) to replace/delete the
  composed character rather than merely finalize composition.
- Some host editors report false; verify before/after snapshots and
  allow exactly one guarded fallback after finishing composition.
  No blind double deletion if the editor changed anyway.
- Suppress re-entrant selection callbacks during a single edit to
  avoid clearing local composition/repainting status midway.
- Preserve existing held Backspace, left-swipe word delete, spacebar
  cursor, privacy policy, V1/V2/V3 backup, banking number layout.

Regression replays the screenshot progression asking→...→a→empty,
including a fake editor that lies about return values and one that
rejects deletion, plus actual Android BaseInputConnection tests.
No Android phone result yet; Candidate 333 stays installed/unchanged.
