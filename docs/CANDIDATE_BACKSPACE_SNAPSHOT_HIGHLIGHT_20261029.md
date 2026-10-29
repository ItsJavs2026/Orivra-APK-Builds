# Candidate — Backspace Snapshot + Highlight Qualification
Date: October 29, 2026

## Scope and status
- Derived from Build 292 via the isolated composing-guard candidate.
- Preserves Preview #178, Build 262, original Build 292, and Build 291 safe Enter.
- No stable APK replaced or published. This is a source-only candidate pending handset qualification.
- Pure Java snapshot policy, deletion boundary, release session and preview checks are automated.
- Android compile-only CI must pass at the exact branch HEAD. A compile pass is NOT a Samsung test.
- Android 17 rendering crash regression remains a physical device risk until qualified.

## Exact end-user behavior
- Backspace **tap** deletes one Unicode code point.
- **Hold** after delay repeats character deletion. A genuine sideways movement disarms the hold.
- **Swipe left** for at least the configured threshold shows the target in the keyboard suggestion strip, highlighted using the current accent color.
- Example: typing `likethis` or `heythere` and swiping left should preview **Delete 1 word: likethis** (or **heythere**), then delete all eight characters only after release.
- `hello likethis`: a one-word swipe should leave `hello `; a longer two-word swipe should clear the whole two-word phrase.
- More than one word uses whitespace boundaries, never arbitrary partial syllables.
- The highlight is inside the keyboard, NOT host-app selection and never changes the editor before release.
- A preview and the corresponding deletion use the same short-lived snapshot; a changed cursor, selection, surrounding text, editor or session cancels the pending edit.
- An invalid snapshot shows `Swipe delete unavailable` and performs no deletion. No sensitive editor previews.
- Swipe back toward origin, pointer cancellation, focus loss, or switching editors cancels deletion.

## Android editor qualification matrix (NOT YET PERFORMED)
1. Newly composed `likethis`, exactly one left swipe -> full highlight and removal.
2. Newly composed `heythere`, same expectations.
3. Committed `likethis` followed by space: swipe once targets `likethis ` by existing whitespace policy; confirm expected editor state.
4. `hello likethis`, one and two-word swipes -> preview and deletion agree.
5. After moving cursor inside `likethis`, ensure only the preceding whole fragment is eligible; if snapshot cannot safely represent it, deletion must fail closed.
6. Cursor relocation mid-swipe -> stale preview must never delete at the new cursor.
7. External insertion/autocorrect of text mid-swipe -> no stale deletion.
8. Selection of `like this` -> preview selection and delete only selected text.
9. Swipe out then back, multi-touch, focus switch and app switch -> zero deletion, no stuck preview.
10. Long hold vs slow swipe -> character repeat only on an actual hold, not a swipe.
11. Rapid swipe/deletion/keyboard resizing -> no Android 17 view hierarchy crash.
12. Google Messages self-addressed draft: Enter tap never sends; multiline app gets newline; deliberate SEND-hold only when app advertises SEND.
13. Palette: adaptive/light/dark accent and sizing -> readable preview label and highlight.
14. Sensitive/password/numeric fields -> no private content preview; no logging or networking.

## Release gate
- Exact-HEAD Java regression suite.
- Exact-HEAD Android compile-only suite.
- Separate APK assembly/signature/package ID and permission checks only when candidate is qualified for packaging.
- Physical Galaxy S26 Ultra Android 17 verification before stable designation.
- Do not call the bug fixed on-phone before the user tests it.
