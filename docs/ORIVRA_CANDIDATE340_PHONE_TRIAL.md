# Orivra Candidate 340 — separate Samsung phone-test package

Based on Research 340 commit `141b6275df50bd8853e9a47503387862b3a4e37e`, which passed **91 of 91 Android 35 emulator tests** in [run 41087081803](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41087081803).

Candidate versionCode: 340. Package: `com.chroomareborn.keyboard.build340trial` (different from 333, 339, and stable). Android app source must differ from the qualified baseline only in `app/build.gradle.kts`, `MainActivity.java`, `CandidateLatencyMetrics.java`, and the `OfflineLanguagePackAndroidTest.java` package assertion.

Highlights:
- Composing-character mutation safety against false-but-applied Android editor writes
- Cursor/session ticket protection before applying next-word or multiword chips
- Repeated tap-by-tap, opt-in in-memory new-word sequence and phrase proposals
- Single-letter English `I` / `a` complete-word learning
- Cached personal-prefix lookups and split predictions
- Opt-in in-memory typed-key p50/p95/max diagnostic duration
- Preserved Candidate 339 Backspace fix, Google Keep/Search Enter and numeric Amount input
- Preserved V1/V2/V3 portable learning format and offline/no Internet permission

**Do not install an unqualified APK.** GitHub Actions must verify package identity,
APK signature, offline asset, privacy and Android emulator tests before release.
Phone testing on the Galaxy S26 Ultra Android 17 remains to be done.

For testing, export the V3 backup from the current candidate before switching.
Because each candidate has its own app ID, manually import V3 into Candidate 340
to transfer existing supported learning; no automatic migration occurs. Keep V1
for older-version rollback and keep the old installed builds.

Test selection/edit and final-letter Backspace in a disposable note; teach a
benign phrase by typing individual letters and spaces several times; test long
draft responsiveness, Google Keep and Search Enter, password/no-learning
fields, and harmless numeric keypad input. No passwords or private text needed.

Important limitation: **novel word-pair/trigram observations are currently
transient session state**, not persistent and not yet portable via V3.
This candidate is not proof of a Galaxy-specific fix or Gboard-level speed.
