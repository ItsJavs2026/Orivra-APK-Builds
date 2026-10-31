# Orivra Research 396 — Reliable DOWN after replies

User's Samsung S26 Ultra Candidate 391 feedback: the keyboard can feel sluggish and predictions may temporarily disappear after sending messages; when letter-row gestures stop working, the **DOWN gesture is affected most often**. Research 393 already adds guarded recovery when chat hosts clear the same editor and notify IME selection updates; Research 395 accelerates the two measured overloaded UI lookups under a 304/242/326 learned union.

## Confirmed source gap

Candidate 391 centers *a single idle next-word TextView* using `SuggestionStripLayoutPolicy.centerIdleSuggestions`, but `LetterRowPredictionSwipe386.chosen(DOWN)` always required **physical slot index 1**. A one-chip centered row contains only index 0, so DOWN returns null despite the visible centered prediction. The 42dp vertical threshold could also require leaving a short letter key's bounds before the recognizer locks.

## Narrow change

- Overload `LetterRowPredictionSwipe386.chosen(direction,visible,centeredIdle)`: for DOWN **only** when a gesture was armed with a verified idle next-word ticket and exactly one visible chip, choose its valid index-0 text, which is physically centered. Active-word lists still demand an actual index-1 center; blank center fails closed. LEFT/RIGHT unchanged.
- Recognize strongly vertical downward movement at **32dp** instead of 42dp; still requires vertical axis dominance 1.55x, matching pointer, continuous DOWN lock, no second touch/long-hold, safe cursor and editor. Short 0–31dp drift still types normally.
- Pass the same `idleNextWord388` flag to both capture-at-lift and deferred-commit candidate choice. Keep the existing value-equality check to prevent a chip changed by async predictions from inserting the old text.
- No modification to `acceptSuggestion`, Space policies, backups, learning stores, privacy/consent, prediction rank or keyboard geometry.

## Qualification

- Android simulated key View dispatch before leaving a 64px key, 1200 external Send clear/refocus cycles with exact `The ` commit, 0–31dp tap immunity, 32–80dp deliberate swipes, reversed/stale pointers, changed options/selection, active one-chip no bogus center.
- 120k synthetic DOWN/triple/solo/short-tap cases, existing 130k letter-row synthetic, 80k idle checks and 210k learning-parity cases plus 36k key typing and full 279+ Android suite (expected >=287).
- This is **NOT** proof of Samsung Android 17 handling or full IME end-to-end touch dispatch; preserve Candidate 391 and learning backups. Release a separately installable APK only after complete qualified checks and comparison.
