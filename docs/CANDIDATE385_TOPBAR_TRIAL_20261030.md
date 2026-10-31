# Orivra Candidate 385 — Opt-in broad-thumb top prediction bar

## New, separately installable trial

Application ID: `com.chroomareborn.keyboard.build385trial`; versionCode `385`. Based on fully qualified Research 385 commit `866059d3fd4e5d1caa1ac7991fc3680bf8fcd02b`, not on experimental Research 380–384 letter–Space dispatcher. **No learning data migration, no automatic cloud backup, no Internet permission.** Candidate 378, Preview #178, 354 and 376 remain unchanged.

### New A/B setting (OFF by default)

Open Candidate 385 settings and enable **Larger prediction touch targets**, then reopen a text field. The ordinary bar's 44dp suggestion chips fill their already-present 48dp row. The 64dp RISE row is filled with its own suggestion targets, and narrow margins between suggestions are reduced. Keyboard height, normal letter key positions, Space, Backspace, prediction rank and accepted-text rules are unchanged.

When the existing **Swipe prediction selection** toggle is also ON, the new conservative recognizer increases horizontal/upward swipe activation distances, rejects ambiguous diagonals, reversals, pointer mismatches and swipe-return-to-tap, and allows minor finger drift for genuine taps. Existing session/editor/caret/privacy guards still apply.

When the new top-bar switch is OFF, the previous Candidate 378 chip layout and PredictionSwipe372 gesture behavior remain available for comparison.

### Verified gates and remaining proof

- Research 385: 140,000 synthetic assertions across 100,000 randomized gestures; CI green.
- Research 385 Android 35: **245/245 passed**, zero failures/errors/skips, GitHub Actions 41166613303.
- This candidate's own APK must pass Android 35 tests, package/version/permissions/signature verification and SHA-256 before public prerelease.
- Emulator success is NOT a Samsung S26 Ultra Android 17 phone test. A real trial must check tapping near top-strip edges, original three-chip smoothness, left/right selection, upward RISE selection, accidental taps during fast typing, long-sentence two-line suggestions, long-press preview, private fields and no adverse effect to the well-liked Candidate 378 two-thumb feel.

### Safe first comparison on the phone

Install alongside 378; choose **Orivra Candidate 385** in keyboard picker. Use a disposable note; do not type credentials into a new test build. Reopen the keyboard after changing the new top-bar setting; compare OFF and ON. Keep an older V4 learning export and do not expect installed learning or settings to move between distinct app IDs.
