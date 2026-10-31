# Orivra Candidate 400 — Center DOWN + Physical Space order

**Baseline:** Research 400 `7247869bbf9ff66364095d78773ef6dc2425f4bd`, based exactly on published Candidate 399. All candidate source changes are limited to package/version, main UI instructions, diagnostics package gate, Android dictionary package test, this document, and release CI.

## User-reported reproduction

On October 31 the S26 Ultra screenshot shows the joined word `thoseyou` in ChatGPT even though Orivra displays `those you` as the center correction. User then reports DOWN does not accept the suggestion. A screenshot does not prove whether they had tapped Space, used ASDF row DOWN, or swiped downward on the center chip. The prior candidate supported DOWN on letter rows only (settings default OFF in each new trial), and the main prediction chip swipe selector supported LEFT/RIGHT/UP but did NOT support DOWN.

## Candidate 400 improvements

1. A deliberate roughly 32dp downward slide starting **on the center prediction chip** selects that chip, subject to existing editor/session/caret/current-text guards. Strict three-slot center index 1, singleton index 0, no diagonal or short tap drift; never accept neighboring chips or Rise. This is distinct from ASDF gesture settings, and uses the existing prediction-chip swipe toggle (default ON).
2. ASDF/QWERTY letter-row DOWN retains its strict 32dp motion threshold. Detect strong 12dp early downward intent to cancel the separate symbol-long-press timer before a slow gesture is stolen. An intent never commits text; original 32dp + editor validation still required. New apps start with letter-row gesture OFF; manually enable ASDF or both in Settings and reopen editor.
3. Physical Space now gets a transient ACTION_DOWN token in the same `HintKeyPressOrder353` queue that already preserves overlapping alphabet keys. On valid Space UP, schedule **one** guarded Space action in order; on cursor swipe, cancel/multitouch or detached editor, resolve/cancel safely without insertion. Ordinary accessibility/programmatic click remains unchanged. Prevents specific out-of-order execution when adjacent thumbs overlap; does NOT auto-rewrite all no-space typing or promise to fix any source of missed taps.

## Data and safe test

- Package `com.chroomareborn.keyboard.build400trial` versionCode 400; independent install. Keep Candidate 396/399 and all original V4 backups; no storage modifications to prior packages.
- Export the newest V4 from whichever existing candidate actually holds your most current learning (396 or 399), explicitly import with local preview into 400, export a new 400 V4, verify checksum/categories, and retain old backup files. Do not uninstall older apps or clear storage.
- Test 10 deliberate center-chip DOWNs on a harmless typo, then 10 ASDF DOWNs **after enabling ASDF middle row**, then short/long tap, LEFT/RIGHT/UP, 20+ ChatGPT send/reopen cycles, quick two-thumb `those you` Space separation, hold Shift and numeric/private fields. If a DOWN fails, first check whether tapping the same center candidate works; identify which origin (chip vs letter row) failed.
- Optional in-memory Candidate latency lab (opt-in) retains only numeric timings. Never collect password, clipboard, text, or remote telemetry.
- Research Java stress uses synthetic text and touch samples. Independent Candidate400 gate: repeat 130k letter-row, 80k idle, 200k capacity, 120k existing DOWN, 150k chat-reset, 210k learning, 120k public-phrase cases, and new 903,566 assertions; full Android 35 **>=299 tests** with zero failures/errors/skips. Verify dictionary asset SHA, signed APK, version and absent INTERNET permission before prerelease.
- **Emulator-qualified is NOT Samsung Galaxy S26 Ultra Android 17 tested**. Do not claim either of the user's live defects is fixed until phone confirmation.
