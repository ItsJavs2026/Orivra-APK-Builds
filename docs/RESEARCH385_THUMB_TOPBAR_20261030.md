# Orivra Research 385 — Broad-thumb top prediction bar (October 30, 2026)

**Prototype, not released or Samsung-tested.** Based directly on released Candidate 378 at `1df1e3065e7fabe939633ec75dfdd42022481304`. This is the start of the user's requested top-bar project tonight; integrate, verify and decide on an APK separately. The installed, positively phone-tested Candidate 378 remains unchanged.

## Implemented behind a new independent OFF-by-default switch

- MainActivity settings: **Larger prediction touch targets: ON/OFF**, sampled at editor focus. Existing prediction swipe selection setting remains independent.
- Normal suggestion chips: keep **existing 48dp header height** and three persistent views; when enabled, expand ordinary 44dp chips to 48dp with zero vertical margin. Reduce chip horizontal margin from 2dp to 1dp. Do not increase total keyboard height or change text size/rank/predictions.
- RISE chips: keep existing 64dp row, but expand each text target from 60dp with 1dp vertical margins to 64dp with zero margin, and reduce horizontal margin to 1dp. Old geometry is retained with switch OFF.
- Swipes (only when old swipe-selection switch is ON): a new pointer-aware recognizer requires 34dp horizontal or 40dp upward motion, with 1.4:1 axis dominance. It tolerates a small 18dp tap excursion. An ambiguous 19–33dp glide never commits a suggestion, a large swipe returning near its origin never converts into a tap, a pointer mismatch or direction reversal rejects the gesture. Existing `PredictionSwipe372.safeRelease` editor/privacy/selection guards and `choose` candidate mapping remain in place. No letter, Space or Backspace gesture changes.
- Candidate 345/344 chip identity reuse is preserved, including three stable slots and stale-suggestion unclickability. In-place layout params are updated only if changed.
- **No persistence of touch positions, no network, no keyboard height increase.** Other optional settings and V4 learning remain unchanged.

## Safety and test plan

Run 100,000 pure Java synthetic gesture cases plus Android 35 instrumentation: tap tolerance, deliberate horizontal and upward swipe, ambiguous glides, return-to-origin/reversal, wrong pointer and NaN, independent settings, measured 48dp normal strip height and 64dp RISE row. Then run the complete existing Candidate 378 Android suite with the added tests. No APK release until zero failures and no unexpected layout or event regression.

**Caveat:** An OFF-by-default switch is not proof this build performs comfortably on a physical Samsung. It remains a GitHub research prototype; do not substitute emulator success for a real-device trial.

## Next morning checkpoints

- Review emulator results and fix any failed or flaky tests.
- Inspect long-phrase and cursor-middle prediction layouts, idle next-word centering, visual continuity and accessibility on actual Android layout.
- If safe, spin a distinct **Candidate 385 trial APK identity** (keeping Candidate 378 installed), run full CI, verify release checksum, then request quick S26 Ultra thumb tests before enabling by default.
