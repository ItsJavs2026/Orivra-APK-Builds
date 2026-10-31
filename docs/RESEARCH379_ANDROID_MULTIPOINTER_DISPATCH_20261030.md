# Orivra Research 379 — Real Android pointer dispatch qualification

**Status:** research-only. No change to Candidate 378, no APK release, no release signing or user data collection.

## Why

Candidate 378 passed 234 Android 35 emulator tests, but its new `ThumbTouch378AndroidTest` exercises mostly the geometry policy and two-thumb queue **directly** rather than through an Android touch dispatcher. This new suite checks what happens with actual multi-pointer `MotionEvent` objects routed by an Android `ViewGroup` with motion-event splitting turned on.

Seven Android instrumentation checks:
1. Right thumb releases before left; output retains ACTION_DOWN order.
2. Left thumb releases first; no duplicated or missing letter.
3. A slight drift past an adjacent cell boundary stays with the original letter.
4. A large cross-key slide cancels both old and accidental neighboring insertion.
5. Android ACTION_CANCEL prevents a ghost letter; fresh taps work afterward.
6. A revoked editor-session queue cannot commit late touch-up events.
7. Weighted alphabet row produces **measured** wider first/last key widths without changing row width.

No letter, pointer position, or editor content is logged or persisted; the test uses fictional letters A/B in isolated throwaway views.

## Limitations

- A measured stand-alone `LinearLayout` and mock letter listeners are used, **not** Orivra's production InputMethodService, a real host app, actual Samsung Android 17, display cutout, skin/touch shape or human thumbs.
- The suite validates Android dispatch semantics and guards but does not prove that the candidate's `RebornImeService` wiring or UI is perfect.
- A release must not be issued based solely on these synthetic emulator tests. First collect the user's Candidate 378 Samsung results (drift ON/OFF, edge ON/OFF, two-thumb lag, letter misses, symbol holds).


## First CI finding and corrective test adjustment

The first run, GitHub Actions `41162721408`, compiled successfully and executed **241 Android 35 emulator tests**. Only `weightedEdgeLettersOwnWiderMeasuredCells` failed: it asserted a 1000px exact rightmost edge, but Android `LinearLayout` rounded fractional child weights to x=999. The other 240 tests passed, including the new two-thumb and event-dispatch scenarios.

The assertion is now **bounded to at most 1px trailing rounding residual**, and additionally requires: the first cell begins at 0, every key has positive width, and every adjacent key has continuous boundaries without internal dead zones. This does not modify the production keyboard, layout weights, user data, APK or original release.

This is a test-harness correction for normal pixel rounding, not confirmation of phone ergonomics. A subsequent CI pass is required before marking Research 379 qualified.

## Preservation

Branch starts from released Candidate 378 HEAD `1df1e3065e7fabe939633ec75dfdd42022481304`. Files changed are only this Android test, documentation, and the research-only Android 35 CI workflow. Candidate 376, Candidate 378, protected Preview #178, V4 backups and the scheduled top-bar design are untouched.
