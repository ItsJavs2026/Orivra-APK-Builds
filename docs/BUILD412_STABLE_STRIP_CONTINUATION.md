# Orivra 412 — async split predictions and stable suggestion-bar updates
Checkpoint: October 31, 2026

## Correct source and observed user behavior
Repository: ItsJavs2026/Orivra-APK-Builds (public)
Branch: candidate/build412-async-split-stable-strip-20261031
Parent: candidate/build411-performance-audit-20261031 (includes 409 diagnostic and 410 idle-paint code, full settings). Do not return to private Chrooma-Reborn Alpha 1.0 prototype; it lacks the real features/settings.

On Samsung S26 Ultra, Android 17:
- 409 Prediction diagnosis OFF: smooth close to Gboard.
- 409 WORKER ONLY: smooth close to Gboard.
- 409 FULL: noticeably laggy, user rated between B (slightly delayed) and C (bad lag/freezes).
- 410 idle-paint improved responsiveness but remained unusably delayed, user reported turning Calm OFF helped.
- Changing the keyboard theme did NOT improve the lag, so a theme-only explanation has weaker support.
- Problem happens in ChatGPT composer AND Android notification quick reply, with background music making symptoms more apparent.
- The user specifically asked whether Rise contributes and requested a better way to update the prediction bar.
These are subjective device test reports, not instrumented timings.

## Source findings
- Previous 411 renderSuggestions() performed PredictionLite.splitPhraseSuggestions(input) and splitNextWords(input) synchronously on the IME main thread. Particularly costly for unfamiliar/long joined strings. This work was **not** captured by the 409 WORKER ONLY A/B mode.
- The bar already has three persistent slots for ordinary words (PredictionStripFrame344) but can rebind listeners, style chips, update expanded Rise and rebuild non-word rows unnecessarily. Full candidate results and immediate previews may arrive in the same display frame.
- Source included a 210ms typing idle gate from 410 and an A/B Rise panel switch plus privacy-safe audit from 411.

## Build 412 implementation in this branch
1. PredictionDispatcher.scheduleWithSplit412 runs both primary SentenceFlow suggestions and optional split phrase/next-word suggestions on the existing SINGLE background predictor worker (generation validation and cancellation remain). Main-thread renderSuggestions only uses split result caches if they exactly match the current composing snapshot. Preliminary frames do not perform split computation. Audit now records SPLIT_WORKER_412 time; SPLIT_ON_UI should no longer be sampled for this path.
2. RebornImeService has a frame-coalescing queue: latest candidate snapshot wins per Android display frame using candidates.postOnAnimation; it is canceled on typing, Space, Backspace and IME lifecycle transitions, and validates editor identity, session, composing text and cursor selection. Protected/no-suggestion fields take the immediate safe path. One additional frame of suggestion visual latency is possible, deliberately traded for lower UI contention.
3. ChipActionBinding412 and a keyed Android View tag reuse click/swipe/long-press listeners when the suggestion's meaning, editor session and long-preview status are unchanged. Inactive/hidden chips clear their action tags, so stale tap targets must not remain actionable. Existing TextView style/geometry diff and stable 3-slot ordinary-word layout remain intact.
4. Settings screen preserves all existing Orivra controls and adds a reversible **Stable frame-based suggestion updates: ON/OFF** toggle (default ON), so A/B comparisons need no new APK. The inherited **Rise expanded panel: ON/OFF** setting allows independent isolation of Rise while ordinary suggestions continue.
5. Inherited Build 411 aggregate in-process performance audit: opt-in switch, Reset, Show and Copy report. No input text or candidate phrases recorded, no network permissions. Report includes stage counts, avg/max execution times, >=8/16/32ms counts and current test settings. It is instrumenting code only; audit overhead may change responsiveness.
6. Separate Android package: com.chroomareborn.keyboard.build412striptrial, code 412, name 1.0.66-build412-stable-strip, label Orivra 412 Stable Strip. Existing Orivra versions and Gboard remain installed, without data migration.

## Engineering verification and limits
- Source-file inspection confirms no remaining synchronous PredictionLite.splitPhraseSuggestions(input) call in RebornImeService renderSuggestions. The helper now runs on the prediction worker.
- Checked balanced Java braces/comments/string syntax in changed main classes by a lightweight scanner; NOT a Java compiler check.
- New pure-Java ChipActionBinding412Test defines six action identity checks; NOT yet run against Android build.
- The **full Android APK has not yet been compiled or installed on Samsung** at this checkpoint. Source changes may still expose compile/runtime issues. No reduction in keypress latency is claimed as verified.

## Phone-first build recipe
From Android Code Studio terminal, using existing working Java 17/Gradle 8.9/native ARM64 AAPT2:
```bash
cd ~
git clone --depth 1 --single-branch --branch candidate/build412-async-split-stable-strip-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Strip412
bash ~/Orivra-Strip412/scripts/build_orivra412_android_code_studio.sh
```
The script checks the exact source branch, exports tracked files into /storage/emulated/0/AndroidIDEProjects/Orivra412Strip, reuses Gradle wrapper from existing Orivra410Idle, builds with -Pandroid.aapt2FromMavenOverride="$HOME/.androidide/aapt2", and verifies output-metadata.json has applicationId com.chroomareborn.keyboard.build412striptrial and versionCode 412. It refuses to overwrite existing 412 project files. If a previous clone exists, git pull --ff-only before building; IDE project copies are exported snapshots and never update automatically.
Expected APK: /storage/emulated/0/AndroidIDEProjects/Orivra412Strip/app/build/outputs/apk/debug/app-debug.apk . Install only on BUILD SUCCESSFUL plus identity verification. Never uninstall stable apps, Gboard or erase user dictionaries.

## Device A/B audit protocol
1. On first run, set Prediction diagnosis FULL, Typing-first suggestion display ON, Stable frame-based suggestion updates ON. Match Calm mode to Build 410 comparison (user reported Calm OFF felt better). Set Rise expanded panel OFF initially. Keep same keyboard theme as earlier test.
2. With performance audit OFF, type the same normal, typo-heavy and long no-space sentences in ChatGPT; confirm prediction chips appear after typing pauses, taps accept the correct current candidate, Space/Backspace and numeric fields behave normally, no stale suggestion tap.
3. Toggle Rise ON, reopen ChatGPT input and repeat. If Rise ON causes noticeable regression, Rise panel contributes.
4. To isolate frame coalescing, compare Stable frame-based updates OFF vs ON with Rise held OFF and same other settings.
5. If still delayed, enable performance audit, Reset and type for 30–60 seconds; return to Orivra settings, Copy report to share in ChatGPT, then switch audit OFF. Compare Letter handler, Suggestion entry, Suggestion render, CHIP_BIND, RISE_PANEL, SPLIT_WORKER_412 and Word-at-cursor/Editor preparation timings. Theme test already failed to change lag, no need to repeat unless new evidence.
6. Test SystemUI notification quick reply with music only after ChatGPT editing is reasonably smooth. No sensitive texts. Gboard remains daily fallback.

## Next technical steps depending on results
- If frame coalescing helps but chip binding still >16 ms, replace remaining non-word structural row rebuilds with a permanently mounted layout and true style diff; review high-frequency InputConnection IPC in render path.
- If Rise OFF helps strongly, make the Rise panel update independently only on explicit user expansion and a confirmed new prediction batch; maintain swipe and one-tap behavior.
- If no improvement, inspect timing report and Android perf trace; move personalized ranking/editor queries off the main thread where safe, measure allocation/GC and avoid throwing away the existing prediction engine.
