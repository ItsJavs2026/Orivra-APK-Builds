# Orivra 413 — measured keypress fastpath
Checkpoint: October 31, 2026

## Correct source and history
GitHub repository: ItsJavs2026/Orivra-APK-Builds (public)
Branch: candidate/build413-keypress-fastpath-20261031
Base: candidate/build412-async-split-stable-strip-20261031
Do NOT use old private Chrooma-Reborn Alpha branch, which lacks modern Orivra settings. Keep all current installed 400/407/408/409/410/412 candidates and Gboard, and preserve V4 backups. No automatic data migration between distinct APK package IDs.

## User-provided Build 412 actual Samsung audit
Device: Galaxy S26 Ultra, Android 17. Audit report had elapsed since reset 878 s. User says 412 improved but still feels laggy compared with Gboard; Rise OFF/ON did not change lag, switching theme did not help. Build 409 OFF and WORKER ONLY were smooth, FULL lagged; Build 410 improved slightly but not enough. User wants fix urgently, with full SentenceFlow/learning preserved.

Raw aggregate audit (each count is per operation and nesting overlaps, NOT touch-to-screen latency):
- Letter handler: n=190 avg 21.09 ms max 42.72 ms; 190 >=8ms; 157 >=16ms; 10 >=32ms.
- Space handler: n=7 avg 40.71 ms max 58.18 ms; all 7 >=16ms; 6 >=32ms.
- Backspace: n=24 avg 6.16 ms max 33.16 ms.
- Suggestion scheduling: n=284 avg 5.69 ms max 126.35 ms (19 >=16ms).
- Suggestion rendering: n=249 avg 2.05 ms max 39.05 ms (2 >=16ms).
- Rise UI: n=270 avg 0.03 ms max 0.92ms.
- Keyboard layout refresh: n=67 avg 14.61 ms max 40.39 ms (23 >=16ms).
- Theme update: n=54 avg 0.68 ms.
- Predictor worker: n=98 avg 7.40 ms max 35.33 ms.
- Word-at-cursor read: n=152 avg 2.32ms max25.62ms.
- Editor preparation: n=214 avg0.52ms.
- Chip binding: n=241 avg0.66ms max3.06ms.
- Split phrases on worker: n=90 avg0.06ms max0.14ms.

Interpretation: Rise, theme and individual chip binding are not the dominant observed costs. Actual per-key synchronous editor IPC, composition verification, scheduled stable-word observation, Space learning/persistence and occasional keyboard layout rebuild are now leading targets. Keep separate measurements of full prediction callbacks. This report does not prove touch latency or how each component overlaps.

## Build 413 implementation — source committed, not yet Android compiled
1. ComposingMutation340 adds an **optional trustPositiveAck** overload. The default old overload remains unchanged. The fast mode still reads editor precondition, calls setComposingText, verifies same editor/session, and ONLY after Android explicitly returns true skips an additional getTextBeforeCursor. Negative/ambiguous ACK continues full original verification and no-duplicate fallback. The next ordinary key again verifies composing precondition. This is a trial, not proven safe across all editors.
2. RebornImeService setting **Fast verified composing ACK: ON/OFF**, default ON for this separate experiment. Applied at editor activation. Sensitive, numeric, editing-in-middle and symbol fields retain original verification. In regular normal text, estimated one fewer synchronous editor read per successfully composing character; exact savings not yet measured.
3. Space already performs before/after editor checks, a guaranteed editor finish/commit and in-process learning. The verified approved-word persistence (`PersonalContextLearning319.recordUsed`, `PersonalWordFrequency328.recordCommitted`) was synchronous on the IME UI thread. A new `ApprovedSpaceLearning413` bounded single-threaded queue handles only already-verified, opt-in approved words OFF-thread; each stored method rechecks opt-in. Max 32 queued tasks and no caller-runs fallback so it never blocks typing. It reuses the same existing V4/learning preference schemas; it does not collect raw messages or new data. Settings **Background approved Space learning: ON/OFF**, default ON. On IME destroy, the worker is gracefully shut down, not awaited. A job can be dropped if queue saturates/process dies; compare persistence after test. The synchronous original path is available with switch OFF.
4. Audit stages added without typed text: initial key editor read, composing mutation, idle word observation, Space approved learning dispatch, Space actual editor finish/commit IPC, and background approved learning worker, alongside inherited total Letter/Space and suggestion/layout/Rise timings. Audit remains opt-in OFF by default, report copyable in full settings.
5. Full settings and SentenceFlow, candidate bar frame coalescing, background split-word worker and Rise switch from 412 are retained.
6. APK is isolated as `com.chroomareborn.keyboard.build413fasttrial`, versionCode 413, versionName `1.0.67-build413-fast-keypress`, label `Orivra 413 Fast Key Test`.

## Verification
- Structural source updates and test source exist in GitHub. Pure Java `ComposingMutation413FastPathTest` exercises positive ACK fast path, unchanged full verification, negative-but-applied no duplicate commit, verified fallback and stale preconditions. This test source has NOT been executed in the Samsung Android build as of this checkpoint. Do not claim test success before actual execution.
- Android Build 413 APK has **not yet been compiled, installed or tested**. Never claim lag is fixed until actual Android Code Studio compilation and same-device A/B.

## Phone build in Android Code Studio (same proven JDK17/Gradle8.9/AAPT2)
```bash
cd ~
git clone --depth 1 --single-branch --branch candidate/build413-keypress-fastpath-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Key413
bash ~/Orivra-Key413/scripts/build_orivra413_android_code_studio.sh
```
The guarded script exports tracked source to `/storage/emulated/0/AndroidIDEProjects/Orivra413Keypath`, copies Gradle wrapper from `Orivra412Strip`, compiles using native ARM64 `$HOME/.androidide/aapt2` override, and verifies applicationId/versionCode. It refuses to overwrite a nonempty existing export. If already cloned, git pull --ff-only before exporting; exported IDE project is a snapshot.
Expected local APK `/storage/emulated/0/AndroidIDEProjects/Orivra413Keypath/app/build/outputs/apk/debug/app-debug.apk`. Build first; only install if BUILD SUCCESSFUL and identity check passes. No GitHub Actions minutes required.

## Device A/B plan (no sensitive typing)
- Start with FULL predictions, Typing-first ON, Stable frame updates ON, Calm OFF (user reported better), Rise OFF (user reports no impact), Fast verified composing ACK ON, Background approved Space learning ON, Performance audit OFF.
- In ChatGPT input, type 30–60 seconds of similar ordinary/unknown text including Space and Backspace; compare responsiveness to 412 FULL. Verify no doubled/missing letters and correct one-tap predictions.
- Turn Fast verified composing ACK OFF, reopen editor, repeat. If it slows down without affecting correctness, the saved editor read is beneficial. If any incorrect composition, turn it OFF immediately.
- Turn approved background Space learning OFF, reopen, compare repeated Space speed. Verify learning backups and word counts separately without using personal/sensitive text. If persistence differs, report it.
- With test settings ON, turn audit ON, Reset measurements, type again, Copy report. Compare Letter avg, Verified composing edit, initial editor read, Space avg, approved learning dispatch/worker, keyboard layout refresh, suggestion scheduling. Avoid summing overlapping timers.
- If still >16ms per letter, next measure direct IPC for cursorHasTextAfter, case updates, InputConnection setComposingText and post-key stable word policies. If only Space remains slow, further isolate mandatory pre-Space verification. Avoid ungrounded theme/Rise rewrites.

## Privacy/safety
No network permissions, no words in timing report, app state separate from other keyboards. Keep Gboard installed. Fast ACK and asynchronous learning both have OFF switches to restore old algorithm for comparison. This is experimental code only until device testing.
