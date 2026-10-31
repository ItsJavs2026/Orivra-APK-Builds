# Orivra Candidate 399 — Public Phrase Index + In-Place Shift

**Source baseline:** qualified Research 398 `74a0bd5e2daf8d6fb518e53d31535ca4d42af2e2`, itself based on qualified Research 397. Candidate 396 is currently installed and user-reported initially somewhat smoother than Candidate 391. No long-session Samsung 17 validation of 396 has been reported yet.

## What's different from 396

- Research 397 reuses immutable precomputed normalizations and an indexed lookup for **fixed public** phrase examples. It does **not** cache user typing or personal learning, add phrases, or change ranking. Public phrase synthetic parity: 120,000; Android 35 previous regression: 290/290.
- Research 398 updates existing ABC letter-key labels and accessibility descriptions **in place** for Shift-only changes, avoiding unnecessary full keyboard teardown. It fully validates the view hierarchy first and falls back to the old full rebuild for an unrecognized layout. Research 398 Android 35 regression: 294/294, including 1,200 synthetic Shift toggles.
- Existing 396 faster personal lookup, ChatGPT Send composer recovery, reliable DOWN, editor privacy, 384/320/400 learning ceilings, and V4 transfer behavior are retained. No new network permissions, telemetry, extra data collection, or automatic learning transfer.

## Independent installation / data protection

- App ID `com.chroomareborn.keyboard.build399trial`, versionCode 399. It is **NOT** an update of 396, 391, or any other trial. Its local preferences and learning start empty.
- Preserve Candidate 396 and all previous V4 files. First **export a fresh V4 from Candidate 396** after its current test. Review/verify the backup locally and keep copies.
- Install Candidate 399 alongside 396. Open 399's settings, **Import V4**, approve the preview, verify the imported aggregate records, then **Export a new Candidate 399 V4**. Compare checksum and record counts. Do not uninstall 396 or clear any app storage.
- Enable **Swipe predictions from letter rows → ASDF middle row** in 399 settings (separate-app preference defaults OFF), then reopen the editor.
- Test 2+ long paragraphs in ChatGPT, at least 20 Send/refocus cycles, sentence-boundary Shift changes, held Shift/caps lock, two-thumb overlapping keys, ASDF DOWN/LEFT/RIGHT, short-word Space, backspace, secure numeric entry and normal Google Keep. Check for skipped letters, stuck caps, canceled key presses, stale prediction strip, double Space, unexpected Send and subjective lag.
- Optional in-memory performance lab: toggle ON explicitly, type synthetic text, refresh only numeric p95 reports and then clear/disable. No user text telemetry. Compare 396 vs 399 on the same phone; do **not** claim emulator timing predicts phone results.

## Mandatory promotion gate

1. Source-safety gate: exactly this candidate's app identity, MainActivity transfer instructions, CandidateLatencyMetrics package gate, Android dictionary test package expectation, release notes, and CI files may differ from Research 398; **production IME / prediction / gestures / V4 learning code must be bit-for-bit the same as the qualified Research 398 baseline**.
2. Replay baseline randomized policy checks (130k letter row, 80k idle gesture, 200k learning capacities, 120k DOWN, 150k chat reset, 210k stored-learning cases), plus 120k Research 397 public-phrase parity.
3. Independent CI Android 35 emulator: **at least 294 tests**, including Research 397 and 398 tests, zero failures/errors/skipped.
4. Verify package/version, official pinned dictionary asset, APK signature and no INTERNET permission; compute SHA-256 and publish a separate **prerelease only after all gates pass**. Never overwrite an existing release.
5. **Emulator qualification is not Samsung Galaxy S26 Ultra / Android 17 validation.** Keep Candidate 396 as working comparison until the user explicitly confirms 399 is better.

This document and this build are candidate-only and do not modify Candidate 396 or any saved learning files.
