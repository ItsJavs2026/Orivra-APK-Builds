# ORIVRA 414 — idle-learning hot-path fix and dead-chip recovery
Checkpoint date: 2026-10-31

## Source of truth and previous milestones
Canonical public repository: ItsJavs2026/Orivra-APK-Builds
Working branch: candidate/build414-idle-learning-chip-recovery-20261031
Based on: candidate/build413-keypress-fastpath-20261031
Never use the private Chrooma-Reborn Alpha branch to build a full Orivra candidate: it lacks modern settings.
The user compiles locally on Samsung Galaxy S26 Ultra Android 17 in Android Code Studio; the user has installed/tested Build 413, and has Gboard as a stable fallback. Build 414 is source only until the next successful local Android compile. Preserve previous candidates, personal data and backups; packages use separate IDs.

## Latest EXACT USER report — NOT a second independent run
User reposted their same **Build 413** performance report, measured over 364 s since reset, and clarified that prediction chips were **NOT clickable** when the bar "gave up". Earlier phrasing in the previous assistant turn implied chips could still be tapped; that was incorrect. The repeated report is the same baseline, not a new device retest.

Build 413 report:
Prediction FULL, Idle paint=true, Calm=false, Rise=true, Frame updates=true, Fast Compose ACK=true, Background Space learning=true, theme Shaded Surface • App Hint.
- Letter key handler: count=182 avg=14.15ms max=49.62ms, >=8/16/32ms=80/66/41
- Space handler: n=13 avg=29.44ms max=39.09ms, >=8/16/32=13/13/6
- Backspace: n=12 avg=3.66ms max=6.70ms
- Suggestion scheduling: n=249 avg=4.91ms max=30.93ms
- Suggestion rendering: n=246 avg=1.72ms max=21.54ms
- Rise UI: n=277 avg=0.08ms max=0.83ms
- Key layout rebuild: n=24 avg=14.32ms max=34.42ms
- Theme: n=8 avg=0.50ms
- Predictor worker: n=96 avg=5.46ms max=47.60ms
- Word-at-cursor read: n=359 avg=4.42ms max=31.64ms, 57 >=16ms
- Editor prep: n=194 avg=0.61ms
- Chip binding: n=232 avg=0.62ms
- Split phrases worker: n=43 avg=0.06ms
- Key initial editor read: n=182 avg=0.71ms
- Verified composing edit: n=121 avg=0.32ms
- **Idle word observation: n=182 avg=11.64ms max=33.27ms; 61 >=16ms**; one call per letter, a leading hot-path cost.
- Space learning dispatch: n=12 avg=0.00ms
- Space finish/commit: n=12 avg=0.17ms
- Space boundary pre-commit: n=12 avg=0.11ms
- Space after commit: n=12 avg=2.56ms
- Background learning overflow drops: 0.
Nested stage timers overlap; these are not touch-to-screen timing or identical controlled sessions.
Build 412 vs 413 subjectively improved but still not Gboard-level. Theme and Rise changes did not help. Earlier Build 409 OFF and WORKER ONLY were smooth while FULL lagged.

## 414 source fixes committed
1. **Remove per-letter idle-learning editor scans.** In RebornImeService.scheduleStableWord329, default ON: derive only a locally typed current candidate from composing or the current-session lastTappedWord340 through pure policy IdleWordCandidate414. No wordAtCursor()/cursorHasTextAfter() Android IPC on the keypress path. A delayed task at existing StableWordTiming334 interval re-checks same live editor/session, consent, selection, numeric/private fields, cursor suffix and bounded editor word. It verifies via StableWordUsagePolicy329.sameVisibleWord before approved learning. With Background Space learning ON, persistence is sent to existing bounded background worker, not UI thread. Safe rejection is no-op, not a retry. **Deferred idle-word learning ON/OFF** setting defaults ON, OFF invokes the original 413 observation method unchanged for A/B. Do not infer predictive accuracy from the policy: it only alters verified learning cadence.
2. **Dead/untappable suggestions.** The legacy PredictionStripContinuity345.holdVisualOnly() deliberately kept stale side-chip text VISIBLE but removed click/long/touch handlers. If model updates were delayed/discarded, the row could appear "stuck" and unclickable as the user reported. 414 stops using that old visual-only state: stale side guesses become INVISIBLE (reserve cell width) and have handlers/tags cleared. Only CURRENT candidates are displayed/clickable. Rebind when an active chip was disabled, even if its prior tag looks equal. Existing click, drag, long-press actions and live editor safety validation remain.
3. **Frame-drop recovery.** If frame coalescing 412 rejects an outdated batch for a changed composing word/cursor *within the same editor/session*, a single debounced fresh showSuggestions() is queued after 110ms, with a 300ms minimum between recoveries. A new character/editor exit cancels it. The old stale result is NEVER rendered. No retry for wrong editor/session/private field or while pending typing-idle paint.
4. **Expanded aggregate audit:** stage `Idle word verification after typing stops` (separate from per-letter `Idle word observation`); counters `Stale untappable chips hidden`, `Outdated prediction frames discarded`, `Prediction frame refresh retries`. Counts and times only; no typed content, suggestions or app IDs in report. Reuse Build 411 opt-in reset/copy mechanism. Report header now shows Build 414.
5. **Preserved:** complete full settings, SentenceFlow, English/Spanish research, personal dictionary and backups, 412 async split/one-frame strip, 413 fast compose ACK, bounded approved Space learning worker, Rise A/B and performance audit.
6. Separate build identity: appId `com.chroomareborn.keyboard.build414livetrial`, versionCode 414, versionName `1.0.68-build414-live-suggestions`, display name `Orivra 414 Live Test`. Existing installed apps/learning data unaffected; personal data does NOT auto-migrate.
7. **Tests added:** IdleWordCandidate414Test.java (7 pure-Java assertions: composing takes precedence, current-session direct-commit word, stale session blocked, unsupported fields, empty input, incomplete contractions). Existing ComposingMutation413FastPathTest.java covers 14 checks. Script run_orivra414_core_tests.sh compiles them with javac --release 17 and runs both before Android compile. The pure helper's equivalent local Java17 copy passed 7 checks, but the user's Samsung and GitHub checkout still need the script's real compilation/tests; don't claim full APK readiness in advance.

## Phone compilation / offline workflows
Android Code Studio's terminal, using the working local JDK17, Gradle 8.9 and native ARM64 AAPT2 override. No paid GitHub Actions needed:
```bash
cd ~
git clone --depth 1 --single-branch --branch candidate/build414-idle-learning-chip-recovery-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Live414
bash ~/Orivra-Live414/scripts/build_orivra414_android_code_studio.sh
```
If already cloned: `cd ~/Orivra-Live414 && git pull --ff-only && bash scripts/build_orivra414_android_code_studio.sh`. The guarded script refuses to overwrite a pre-existing nonempty export: `/storage/emulated/0/AndroidIDEProjects/Orivra414Live`. Do not delete an old build/export without preserving it.
The script copies Gradle 8.9 wrapper from prior successful `AndroidIDEProjects/Orivra413Keypath`, uses `-Pandroid.aapt2FromMavenOverride="$HOME/.androidide/aapt2"`, builds :app:assembleDebug and checks metadata for appId and version 414.
Expected local APK: `/storage/emulated/0/AndroidIDEProjects/Orivra414Live/app/build/outputs/apk/debug/app-debug.apk`. Install only after BUILD SUCCESSFUL and identity verification. Build 414 is NOT currently Android-compiled or handset-tested. Structural scans are not proof of Java compilation.

## Critical test order when compiled
1. Select 414 keyboard, preserve Gboard. FULL predictions, Idle paint ON, Calm OFF (user's smoother setting), Stable frame ON, Rise OFF or ON (Rise irrelevant on prior test), Fast compose ACK ON, Background Space learning ON, **Deferred idle-word learning ON**. Performance Audit OFF initially. Type harmless normal and difficult/no-space words in ChatGPT. Check the suggestion chips are *either current and responsive* or hidden rather than stale and dead. Check one-tap insertion is accurate, including when slowing/stopping typing. If missing/duplicated letters or model stops showing live options, stop and report.
2. Enable performance audit, Reset, type a comparable 30-60 s sample. Copy report. The key outcome should be a reduction in `Letter key handler` from baseline 14.15ms and `Idle word observation` from 11.64ms, but no result is predicted/claimed. `Idle word verification after typing stops` is expected outside key handler; don't add nested timing metrics.
3. Toggle **Deferred idle-word learning OFF**, reopen input, repeat to compare against the original observer method **in the same APK**, without reinstalling. Preserve learning setting/backup; check learned words are retained appropriately. ON should not leak typed content.
4. Track counters for stale chips hidden/discarded frames/retries; these provide reasons for the bar seeming to "give up". If frame recovery high, consider a true editor-scoped state machine and snapshot-based UI ranking; don't add arbitrary delays.
5. Test Space (still 29.44 ms avg baseline) and notification quick reply with music after ChatGPT composer typing is reliable.
6. Newer 414 features require Android APK and Samsung verification before we claim Gboard-like performance.

## Other notes
Work is on source branch, not GitHub Actions. GitHub Free spending allowance previously exhausted; phone-first is preferred. This checkpoint is intended for a fresh chat to continue exactly, without losing the latest results.

## Latest source-level verification note
- A matching independent local copy of `IdleWordCandidate414` and its 7-test Java suite compiled under `javac --release 17` and printed **IdleWordCandidate414Test PASS (7 checks)**. This is a test of the pure candidate policy only, not a full Android compilation, and the actual GitHub byte-for-byte checkout has not been locally downloaded. The on-phone Build 414 builder is configured to execute tests on the checked-out source before Gradle.
- Source inspection reports balanced Java braces/parentheses across modified RebornImeService, MainActivity, OrivraPerfAudit411, and IdleWordCandidate414. That does not detect all compiler errors or runtime errors.
- Actual Build 414 Android APK and new handset performance report remain **pending**. The user has not supplied any Build 414 result; do not misinterpret the repeated Build 413 performance audit as a new run.
