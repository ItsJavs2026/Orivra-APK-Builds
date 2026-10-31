# Orivra — Two-location prediction swipe experiment (DESIGN ONLY)

**Status:** plan for next user session; no APK or code behavior changed.
**User requirement:** prioritize conversational typing productivity and comfort for larger thumbs, not decorative gestures. Keep the option to use prediction-bar swipes and experiment with an alternate *top QWERTY letter-row* swipe location.

## Proposed setting
A single mutually exclusive control, **Prediction swipe location**:
- **Prediction bar (default):** preserve Candidate 373 swipe left/right on existing suggestion chips and upward toward RISE when displayed.
- **Top QWERTY row (experimental):** horizontally drag on Q–P key row to select among predictions displayed in the suggestion strip; upward gesture may select a RISE alternative if visible.
- **Off:** regular typing and tap-only suggestions, preserving existing independent highlighted-Space acceptance setting.

Separate mode choice avoids accidental double-handling of a gesture when areas overlap. If phone feedback prefers both active simultaneously, review a fourth mode after tests.

## Fat-thumb and two-thumb behavior requirements
1. **No tiny target:** use full first-row key hit areas. Do not add an extra row or shrink key widths/heights. Preserve shaded visual design.
2. **Tap first:** quick down/up commits its original QWERTY letter exactly once. No prediction operation on down, and normal typing does not wait for a long-press timeout.
3. **Intentional drag only:** require displacement, dominant axis and movement consistency before promoting to a prediction gesture. Expose an optional sensitivity profile only if physical Samsung tests show a real benefit.
4. **Two-thumb protection:** a second simultaneous pointer cancels the speculative swipe; no missing or reordered characters. Preserve HintKeyPressOrder353 source of truth for letter commits.
5. **Special symbols:** continue existing fast ~320-ms alternate symbol hold; a moving swipe cancels that hold before it fires, and a stationary hold never becomes prediction selection.
6. **RISE:** only permit upward selection when RISE is truly visible, with an explicit visual highlight; otherwise upward motion does nothing rather than type an accidental word.
7. **Finger feedback:** highlight the currently eligible prediction chip without shifting suggestion positions; accept once on release, preserve original touch ripple/haptic preference.
8. **Editor safety:** revalidate live connection, composing word, cursor and selection at release. Never commit in protected/numeric fields or when IME focus changes.
9. **Disable/switch quickly:** settings mode selection applies on next editor focus without resetting dictionary learning or changing keyboard identity.
10. **No telemetry of private text or touch paths.** Synthetic tests and local optional performance counters only.

## Phone A/B comparison
Compare short (~30-word) and long (~150-word) ChatGPT drafts in both locations, including:
- Perceived speed and one-handed reach with larger thumbs
- Words per minute (optional manual timing)
- Number of accidental letters / unwanted prediction commits
- Two-thumb taps immediately following a swipe
- RISE gesture correctness
- Character/Space correction continuity in long drafts

### Current baseline
Candidate 371: phone-reported smoother key feedback.
Candidate 373: prediction strip swipes + Space-highlight corrections, phone tested; short prefix and long-draft issues reported.
Research 374: short 1–3 character Space completion eligibility, emulator-qualified.
Research 375 / Candidate 376: long-draft editing state classification and short-space rule, new trial undergoing qualification as of design drafting.
Avoid conflating these with a verified Samsung phone fix.

## Scope sequencing
Ship and phone-test current short Space / long-draft recovery **before** merging top-row gestures. For tomorrow, create a new research-only gesture mode from the latest PHONE-APPROVED candidate, run deterministic touch-order/regression tests and Android emulator, then produce a separate APK.
