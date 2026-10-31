# Orivra Research 381 — Android letter + Space dispatch test

**Research-only**, based on Research 380 at `daa12b44f0c65972de505c30dfbe16201bad2f8b`.

The user reported Candidate 378 feels good so far on Samsung S26 Ultra Android 17. Do **not** replace that version or integrate an unverified mixed-barrier. This branch only adds emulator test coverage.

## Added tests

Eight Android 35 instrumentation tests synthesize two pointers routed through `LinearLayout.setMotionEventSplittingEnabled(true)` and actual adjacent `Button` views, exercising the pure Java `MixedThumbOrder380` and existing `SpacebarCursorGesture` / `ThumbTapGuard377` policies with an isolated, fictional in-memory text sink:

1. Letter down then Space down, Space released first -> final "a "
2. Letter released first -> "a "
3. Spacebar cursor drag -> only "a"; no accidental Space
4. Space down before letter down, letter released first -> final " a" in true press order
5. Multi-pointer Android ACTION_CANCEL -> no ghost; next tap works
6. Slight thumb drift of the initial letter before Space -> "a "
7. Large deliberate slide and re-entry -> cancels original letter, preserves accepted Space\n8. Simulated input session invalidation -> late release cannot reinsert stale text

## Limitations

This is an artificial two-button harness and **not** the production input service or a realistic Samsung touch study. It does not prove latency, real predictive-Space acceptance, finger comfort or editor correctness. Actual integration would require a separate design review and phone tests. Does not collect text, touch paths or device data, change backup/privacy behavior or make an APK. Top-bar work remains separate.

## GitHub CI failure and corrected event stream

Initial corrected run `41164245285` ran 241 tests and failed only `slightLetterDriftStillOrdersBeforeSpace`: the expected `a ` became ` `. Inspection showed a **test helper defect**, not a production IME failure: the simulated first thumb moved from x=211 back to x=40 on the second finger's `ACTION_POINTER_UP`, triggering the large-slide rejection. The updated harness keeps the held thumb at x=211 through that event, while preserving the existing negative case that an intentional large slide, even with re-entry, cannot commit. The harness now covers eight test cases; Android 35 rerun is required.
