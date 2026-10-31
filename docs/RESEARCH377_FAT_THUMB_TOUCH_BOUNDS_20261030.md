# Orivra Research 377 — Fat-thumb letter touch bounds

**Status: research-only. Not a released APK, not integrated into RebornImeService, and NOT Samsung-tested.**

## Grounded source inspection

- Candidate 376's `addKey` uses a **full-cell hit box** and an inset DRAWABLE, rather than margins that create dead zones.
- Letter keys have their own ACTION_DOWN / MOVE / UP sequence so secondary-symbol hold and existing `HintKeyPressOrder353` two-thumb ordering remain intact.
- The current letter MOVE path cancels after leaving key bounds by 10dp. A release position not preceded by a MOVE is not independently bounds-checked.
- Standard/Tall/Extra Tall already exist. Tall portrait letter rows are 72dp; Extra Tall is 84dp; non-Standard top rows gain 5dp.
- Existing instant key feedback, haptics, prediction corrections, Space cursor swipe, Backspace swipe and prediction-strip gestures remain protected.

## Proposed ergonomics experiment (not in any APK)

`ThumbTapGuard377.java` is a transient pure-Java pointer geometry policy for studying slight thumb drift. It does NOT resize or shift key hit zones, auto-correct words, read editor text, collect touch coordinates, persist a profile, or override accessibility.

- Capture a single down pointer and the **fixed** cell bounds. Do not change dimensions while touches are active.
- Grace beyond original full-cell target: at most **14dp horizontal** / **18dp vertical**, further limited to 38% key width / 28% key height.
- Reject deliberate sliding across most of a cell: beyond 80% key width / 65% key height.
- Wrong pointer, missing/invalid coordinates, large slides, explicit CANCEL and duplicate UP always fail closed. A canceled touch cannot be restored by re-entering the key.
- Validate both on MOVE and final UP (including an UP delivered without an intervening MOVE).
- Preserve existing key-owned pointer / hint hold / two-thumb order behavior during any future integration.

## Synthetic reproducibility

```sh
mkdir -p /tmp/orivra-research377
javac -encoding UTF-8 -d /tmp/orivra-research377 \
  app/src/main/java/com/chroomareborn/keyboard/HintKeyPressOrder353.java \
  app/src/main/java/com/chroomareborn/keyboard/ThumbTapGuard377.java \
  app/src/testsupport/java/com/chroomareborn/keyboard/ThumbTapGuard377Research.java
java -cp /tmp/orivra-research377 com.chroomareborn.keyboard.ThumbTapGuard377Research
```

The program checks 50,000 generated slight edge slips, 50,000 deliberate cross-key slides, cancellation, bad pointer data, and 1,000 overlapping two-thumb pairs using existing `HintKeyPressOrder353`. Local Java run passed **101,029 synthetic assertions**.

**Limits:** This is a constructed synthetic comparison against Candidate 376's 10dp cancellation cutoff. It is not an observed finger-error distribution, an Android MotionEvent instrumentation suite, a Gboard accuracy comparison, a Samsung device test or proof of improved typing.

## Safety and next gates

1. User tests the separate Candidate 376 on a disposable note; do not replace stable Candidate 354 or Preview #178.
2. Future integration is **OFF by default** until Android MotionEvent tests cover wrong/canceled pointers, secondary symbols, two-thumb sequence, release-without-MOVE, focus changes, key rebuilds and accessibility.
3. Compare dropped characters, wrong characters, long-press symbols, swipe behavior and typing latency on Galaxy S26 Ultra Android 17. Revert if tradeoffs are worse.
4. Keep all experimentation offline, avoid key/touch telemetry, preserve package identities, V4 backup and all installed candidates.
5. Tomorrow's top-bar gesture work stays separate from today's letter-key ergonomics work.
