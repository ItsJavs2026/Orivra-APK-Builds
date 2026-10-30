# Research 323 — bank/amount numeric keypad regression

Samsung phone report: Candidate 319 displayed alphabetic QWERTY in a bank
Amount field showing $0.00 and "Private field · predictions off". The entire
bottom letter switcher was suppressed, leaving only Go, so digits could not
be entered. This is an Orivra layout bug, not a problem with the user's bank.

Root cause (verified source):
- `numericEditor=true` for Android TYPE_CLASS_NUMBER/PHONE
- UI still constructed ABC KeyboardLayoutSpec rows.
- The code gated Shift and '?123' on `!numericEditor`, so every
  route to digit keys was removed.

Fixes:
- Real 0–9 numeric pad automatically for declared NUMBER, PHONE, DATETIME.
- Decimal '.', numeric minus when signed, phone '+', date '/'.
- Numeric passwords/PINs hide punctuation.
- Numeric pad keeps tap Go/Enter action, Backspace hold and release.
- Always show ABC fallback, plus a ?123 route back from alphabetic override.
- Normal text keeps its existing ?123 symbol row and Google Search fix.
- Private editor never resumes predictions or persistent learning.
- Candidate 319 APK and preference files remain untouched.

Automated Java/Android 35 tests validate field class, decimal, pin,
privacy, action labels, no Internet permissions. The actual banking app
must still be tested on the user's Galaxy: Android cannot guarantee a
third-party field accepts a character just because a keypad exposes it.
