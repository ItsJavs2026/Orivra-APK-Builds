# Research 391 — Integrating both S26 Ultra fixes on one clean candidate path

**No APK released from this branch.** All source originates from two separately tested lines based on the same Candidate 386 release commit `7cf2a9d35aa7d5a4b85b02c326e1af9387236256`.

- **Research 388** passed 255/255 Android 35 tests. It permits actual QWERTY/ASDF key swipes to select next-word choices after Space (e.g. `I | The | Hey`) using the existing `NextWordTapTicket340` caret/session validation. Research 387 already ensures center-only candidates can start DOWN. Existing short-word completion with one Space is preserved.
- **Research 390** raises the live and import limits **together** to 384 accepted corrections, 320 context preference keys, 400 frequency words. User's two SHA-256-valid V4 backups have a distinct-record union **304/242/326** plus 58 typed vocabulary records and 30 typed-word pairs. Real Android tests synthesize those exact sizes, import in both directions, export all records, enforce MAX per-key counts and idempotence, and reject overflow before writes.

These two changes occupy disjoint production files; Research 391 combines their exact code and Android tests without changing MainActivity, release identity, signatures, learning consent or privacy, keyboard geometry, V4 checksum grammar, key tap/hold thresholds, or prediction rank. Expected full test suite >=261 Android tests, all passing plus both pure-Java suites. Any failure blocks the candidate step.

Once green, a *separately installable* candidate can be prepared. On the user's phone, preserve Candidate 378 and 386 and both original backups; import 386 V4 first then 378 V4 into the **new empty app**, export and verify the merged V4 counts. Never destructively reset 386's current learning. Exports above the old ceilings may not be importable into Candidate 386, so keep the old files forever as rollback.
