# Orivra Candidate 323 — banking Amount numeric keypad

Separate debug-signed prerelease candidate, distinct Android application ID
`com.chroomareborn.keyboard.build323trial`.

This candidate includes the Samsung phone-qualified touch improvements of
Candidate 319 and only the isolated numeric keypad fix from Research 323.
It does NOT merge the newer used-word learning / experimental V2 backup,
which remain in their own research branches until verified.

Numeric input classes now show actual digit keys, '.' decimal for payment
amounts, '+', '/' for phone/date, and '-' when the input permits a signed
number. Numeric passwords do not offer decimal punctuation. A permanent
ABC fallback switches to letters, with ?123 returning to the numeric pad.
Banking/financial input remains private and never enters learning history.

All existing Google Search, Safe Enter, Space and Backspace gestures, and
V1 dictionary export/import remain as in Candidate 319.

GitHub Actions must verify source identity, Java regression, 50+ Android
35 tests, signed APK identity, SHA256 and no INTERNET permission before
publishing. User should test a non-submitted amount only, not send money.
