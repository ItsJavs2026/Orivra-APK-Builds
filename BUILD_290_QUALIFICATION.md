# Orivra Build 290 — Backspace swipe safety candidate

## Verified baselines (physical Samsung Galaxy S26 Ultra)
- Build 287: initial Backspace tap crashed; captured AndroidRuntime exception was a null child in a LinearLayout during draw.
- Build 288: user confirmed the tap-crash fix.
- Build 289: user reported all six manual smoke checks passed — tap, hold repeat, left-swipe one word, longer left-swipe multiple words, slide-back cancellation, and no crash during those tests.
- These historical confirmations do **not** imply Build 290 has been installed or tested.

## Purpose of Build 290
Improve the already working Build 289 gesture without changing the stable Build 289 code:
1. Guard touch-release Backspace work against editor/input-session switches.
2. Reset held-delete state when Android destroys/restarts the input session or view.
3. Prefer a live editor selection when deleting selected text.
4. Fail closed on invalid/malformed gesture coordinates.
5. Remove redundant prediction-strip rendering during the release refresh.
6. Expand deterministic pure Java regression coverage to 12,000 text samples.

## Automated GitHub checks
- Java 17 policy regression test (BackspaceSwipePolicyTest)
- Existing layout, haptic, suggestion-strip, punctuation and cursor tests
- Android :app:assembleDebug
- APK signing, package ID, version code and no INTERNET permission checks
- Artifact name: Orivra-Build290-Swipe-Safety-Experimental-APK

## Manual Samsung test matrix (new Build 290 only)
Run in an ordinary nonsensitive text field, using synthetic text. Do not share actual passwords or private text.

| # | Action | Expected |
|---|---|---|
| 1 | Type "hello", tap Backspace | Deletes exactly one character, no crash |
| 2 | Press and hold Backspace | Repeats while held, stops on release |
| 3 | Type "hello world", swipe left from Backspace | Removes the final word on release |
| 4 | Type "one two three ", make a longer left swipe | Removes multiple complete words |
| 5 | Start a left swipe, move back toward Backspace, release | Deletes nothing |
| 6 | Start a left swipe then cancel the touch (e.g. another finger) | Deletes nothing unexpectedly |
| 7 | Select a word in text, swipe-delete | Deletes selection without adjacent text |
| 8 | Enter text including emoji and accents (e.g. "hi 👍🏽 café") | Deletion does not leave broken Unicode text |
| 9 | Delete words next to punctuation and multiline text | No unintended deletion beyond chosen word boundaries |
| 10 | Initiate Backspace, then change focus/text field before the pending work | The new editor is never modified by the old touch |
| 11 | Rapid repeated single taps and hold/release cycles | No LinearLayout rendering crash or stuck deletion |
| 12 | Switch to/from another keyboard and reopen Orivra | Key labels, predictions, and deletion controls remain responsive |

## Release discipline
- The Branch build290-swipe-safety-regression is a *candidate*, not stable.
- Do not modify or delete Build 289 or the user's older stable Preview #178.
- A passing CI build proves compile/tests/signing, not Samsung device behavior.
- No telemetry, network access, sensitive text logging or background collection should be added.
