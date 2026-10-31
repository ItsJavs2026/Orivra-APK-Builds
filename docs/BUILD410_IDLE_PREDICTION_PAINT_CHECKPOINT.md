# Orivra Build 410 — typing-first idle prediction painting
Checkpoint: 2026-10-31

## Canonical source and user evidence
Repository: ItsJavs2026/Orivra-APK-Builds (public)
Branch: candidate/build410-idle-only-prediction-paint-20261031
Parent: candidate/build409-prediction-isolation-20261031
Do NOT use old private Chrooma-Reborn Alpha branch as the full keyboard; it lacks the settings and newer features.

Direct Samsung Galaxy S26 Ultra / Android 17 test reports in the ChatGPT composer:
- 409 Prediction diagnosis OFF: Smooth, close to Gboard.
- 409 WORKER ONLY: A = smooth, close to Gboard.
- 409 FULL (Calm ON): between B and C = noticeable lag bordering on serious.
This is subjective real-device evidence, not instrumented latency measurements. It strongly implicates FULL-only work such as main-thread suggestion merging, rendering, chip geometry, animations, or cursor/editor queries. WORKER ONLY does not exercise every FULL codepath, so the precise bottleneck remains unproven.
Other affected editors: Android notification-shade replies (especially with background music); ordinary ChatGPT input; unfamiliar text particularly slow.

## Source diagnosis
Build 408's PredictionUiBudget408 has a 360ms maximum burst and then may schedule 1ms refreshes while sustained typing continues. In FULL, renderSuggestions(...) can re-evaluate split predictions, touch editor state, resize/style/rebind chips and the Rise panel, followed by async predictor callbacks on the main thread. It can contend with key handling. Continuous typing should not repeatedly trigger expensive candidate UI updates.

## Build 410 implementation
- New IdlePredictionPaint410 policy: 210ms quiet interval after the most recent character; NO forced 1ms/max-burst repaint. The source helper stores only last monotonic key uptime, never user text.
- RebornImeService FULL mode schedules exactly one editor/session-scoped pending Handler callback, re-arming after every keypress, Space or ordinary closing punctuation, and safe Backspace events. The InputConnection key commit remains immediate.
- Pending FULL render/async callbacks are suppressed while typing refresh is queued or the quiet interval hasn't elapsed; once paused, normal SentenceFlow suggestions and one-tap accepting resume. Existing 409 OFF and WORKER ONLY diagnostic modes remain available.
- Build 410 user setting: **Typing-first suggestion display: ON/OFF**, default ON. OFF restores prior 409/408 FULL refresh pacing for an A/B test. Full older settings (theme, haptics, learning, V4 backup, predictions gestures) are retained.
- PredictionDispatcher(false) stays in the IME to preserve 409 isolation, but now pre-warms only public lexicon data on its existing background worker when FULL mode activates, avoiding first-use lexicon work on the main keyboard thread.
- Fresh separate package id com.chroomareborn.keyboard.build410idletrial, versionCode 410, versionName 1.0.64-build410-idle-paint, manifest label Orivra 410 Idle Test. Defaults to FULL with idle painting ON in the new install. Existing 409/408/400, Gboard and their learning data remain untouched. New package does not automatically inherit preferences/learning.

## Verification status
- Added source file app/src/testsupport/java/com/chroomareborn/keyboard/IdlePredictionPaint410Test.java. A matching local Java source mirror compiled with javac --release 17 and executed **50 checks successfully** for quiet intervals, continuous typing, editor reset and backward clock cases. This only verifies the pure timing policy. The Android source remains UNBUILT, uninstalled and untested on user's device as of this checkpoint. Do not claim the lag fixed.
- Static review checked branch appId/version, codepaths and settings. Any Java/Kotlin compilation failures must be fixed after the user's Android Code Studio build reports first error.

## One-command Samsung local build
Android Code Studio package com.tom.rv2ide with JDK17 and working Gradle 8.9 wrapper. Native ARM64 AAPT2 is located at $HOME/.androidide/aapt2. Public standard Maven Linux AAPT2 fails with Syntax error on arm64, so use the existing override.

From Android Code Studio terminal on phone, with Orivra409Isolation folder already present:
    cd ~
    git clone --depth 1 --single-branch --branch candidate/build410-idle-only-prediction-paint-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Idle410
    bash ~/Orivra-Idle410/scripts/build_orivra410_android_code_studio.sh

Script checks exact branch, prevents overwriting an existing Orivra410Idle project, exports tracked source only, reuses known Gradle 8.9 wrapper from Orivra409Isolation, builds via native AAPT2 override and verifies APK applicationId/versionCode.
Expected APK at:
    /storage/emulated/0/AndroidIDEProjects/Orivra410Idle/app/build/outputs/apk/debug/app-debug.apk
Expected metadata applicationId com.chroomareborn.keyboard.build410idletrial, versionCode 410. Install only after BUILD SUCCESSFUL and script's identity check. If already cloned the branch, git pull --ff-only first; exported AndroidIDEProjects folder is a snapshot and does not auto-update from Github.

## Samsung verification protocol
- Confirm Orivra 410 installs separately and full settings appear. Keep Gboard installed and selected except during disposable test input.
- FULL is default; verify **Typing-first suggestion display ON** and **Calm prediction display ON**. In ChatGPT, manually type long ordinary, joined/unknown, and misspelled sentences for 30-60s; watch key response, predictions arriving only after a brief pause, and any freeze. Compare to 409 FULL baseline (between B and C).
- Toggle Typing-first suggestion display OFF and reopen the field, repeat. This is an internal A/B on the same test APK, no extra installation.
- If improvement, test Space, Backspace repeat, punctuation, prediction tap, expanded Rise manually, long sentence one-tap, numeric/password privacy and screen-rotation. Notification quick-reply with music second.
- If still visibly laggy, prioritize profiling the main-thread renderSuggestions() and especially synchronous InputConnection queries and view/layout changes. Avoid repeatedly increasing delays blindly; consider lightweight cached pre-render ranking on worker and view diffing.
- No sensitive/private typing during experimental keyboard tests.

Current state: branch source committed; user has NOT yet compiled or tested Build 410 on Samsung.
