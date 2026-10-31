# Orivra Build 408 — calm predictions and notification reply experiment
Date: 2026-10-31

## Canonical source
Repository: ItsJavs2026/Orivra-APK-Builds (public)
Branch: candidate/build408-calm-prediction-reply-20261031
Parent: candidate/build407-full-settings-recovery-20261031, which came from the full Candidate 401 source, **not** from old private Chrooma-Reborn Alpha 1.0. This preserves the broad settings and learning features.

## User bug and evidence
User's real Samsung Galaxy S26 Ultra on Android 17 still lags on full Build 407. Lag increases when predictions cannot interpret typed text, while background music plays, and in notification shade quick replies. User uploaded a 36-second Samsung screen recording; visible in it are repeated updates and different guesses in the prediction strip during unfamiliar/jumbled words. This cannot by itself measure event-to-screen latency or prove a particular root cause.
Source review: Candidate 401 schedules a UI suggestion refresh at WAIT_MS=96 after starting a burst. PredictionDispatcher starts background computations with 12/20/28ms debounce. Each render can re-evaluate split phrase suggestions and expanded Rise panel; unknown inputs can produce >3 alternatives that automatically expand the panel. These are plausible sources of CPU/UI contention, not yet instrumentally proven.

## Targeted changes committed
1. New PredictionUiBudget408: no user text retained; offers 150ms trailing-edge quiet period, capped at 360ms per uninterrupted typing burst; avoids full UI prediction processing on every fixed 96ms interval. Existing TypingRefreshGate401 editor/session validation still guards callbacks.
2. RebornImeService toggles this with keyboard_feedback preference `orivra_calm_predictions_408` (default true); OFF keeps Candidate 401 pacing. The IME reads it at next onStartInput. Its scheduling/cleanup resets the budget at editor exit.
3. With calm predictions ON, suppress *automatic* expansion of the supplementary Rise suggestions for ambiguous inputs; manual arrow expansion remains. The stable normal suggestion row is retained. SentenceFlow and learning are not removed.
4. PredictionDispatcher drops its completed ScheduledFuture reference after processing so it does not retain the finished captured input/listener; generation checks/cancellation remain intact.
5. MainActivity retains the full advanced settings and adds a reversible 'Calm prediction display: ON/OFF' switch at top.
6. New deterministic pure-Java PredictionUiBudget408Test.java (9 checks) — test source added; a matching pure-Java local JDK 21 copy compiled and passed 9 checks (not Android compilation and not Samsung testing).
7. Independent Android appId `com.chroomareborn.keyboard.build408calmtrial`, versionCode 408, versionName `1.0.62-build408-calm-prediction`, label `Orivra 408 Calm Test`, to preserve installed 400/407 and user learning data. No automatic preferences migration.

## Current verification
Source committed; **not yet compiled as full Android APK or tested on Samsung**. Previous full 407 compiled successfully with 32 tasks and 24s, native AAPT2 override. Do not claim 408 fixes the lag until user reports tests.

## Mobile Android Code Studio procedure
Android Code Studio package com.tom.rv2ide. On user's phone JDK17 + Gradle 8.9 works. The standard Maven AAPT2 is incompatible with ARM64, override with `$HOME/.androidide/aapt2`.

```bash
cd ~
git clone --depth 1 --single-branch --branch candidate/build408-calm-prediction-reply-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Calm
mkdir -p /storage/emulated/0/AndroidIDEProjects/Orivra408Calm
git -C ~/Orivra-Calm archive HEAD | tar -xf - -C /storage/emulated/0/AndroidIDEProjects/Orivra408Calm
cp -r /storage/emulated/0/AndroidIDEProjects/Orivra407Full/gradle /storage/emulated/0/AndroidIDEProjects/Orivra408Calm/
cp /storage/emulated/0/AndroidIDEProjects/Orivra407Full/gradlew /storage/emulated/0/AndroidIDEProjects/Orivra408Calm/
cd /storage/emulated/0/AndroidIDEProjects/Orivra408Calm
bash gradlew -Pandroid.aapt2FromMavenOverride="$HOME/.androidide/aapt2" :app:assembleDebug --no-daemon --max-workers=2
cat app/build/outputs/apk/debug/output-metadata.json
```

Expected metadata packageId com.chroomareborn.keyboard.build408calmtrial, code 408, version 1.0.62-build408-calm-prediction. Install only on BUILD SUCCESSFUL and correct metadata, retaining Gboard and previous candidates.

## Test plan
- Start music; use Android notification quick reply with Calm ON; type difficult example `itinstskk`, `thisiscoolbutitslaking`, normal short sentence, long sentence, quick Space, Backspace and prediction taps.
- Record input stalls, suggestion rise/flicker, responsiveness, and whether the IME is ever unresponsive.
- Toggle Calm OFF in Orivra 408 settings; reopen text field; repeat same typing (A/B comparing Candidate 401 pacing, but without changing installed app).
- Test split words, learning backup/export, symbol keyboard, secure and numeric fields, caret edits. Do not type sensitive information in this experimental IME.
- The exact setting is reversible and defaults ON. Continue tuning only using measured device evidence.

## Source check
- GitHub branch source inspected after commits: MainActivity has calm switch and full 65 KB settings UI; RebornImeService has refresh budget and preference; build file uses distinct 408 appId; predictor futures now clear completed captures.
- No 408 Android APK has been built, installed or performance-tested as of this checkpoint. The next evidence gate is the user's real phone build.
