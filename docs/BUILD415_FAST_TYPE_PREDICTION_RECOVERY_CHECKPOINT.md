# Orivra Build 415 — safer faster typing and prediction wake
Checkpoint: 2026-10-31

## Canonical repository and user state
Public repo: ItsJavs2026/Orivra-APK-Builds
Current source branch: candidate/build415-fast-type-preview-recovery-20261031
Based on: candidate/build414-idle-learning-chip-recovery-20261031
Do not build from the private legacy Chrooma-Reborn Alpha repository: it has incomplete settings/features.
User has **installed and is testing Build 414 on a Samsung Galaxy S26 Ultra / Android 17** using on-device Android Code Studio. They report typing is clearly smoother but still sometimes stutters, especially during very fast typing, and predictions sometimes feel lost/delayed. User intends to compare vibration/haptics ON vs OFF, but has **not reported the outcome of that comparison** yet. Retain Gboard as stable fallback and never remove older keyboard packages or wipe learning.

## Exact Build 414 user-submitted device performance audit
Audit ON, elapsed since reset 1130s; FULL predictions, Idle paint true, Calm true, Rise true, frame updates true, Fast compose ACK true, background Space learning true, deferred idle word true, theme Shaded Surface • App Hint.
- Letter keys: 296, avg **3.55ms**, max 204.84ms, >=8/16/32ms: 11/8/2.
- Space: 8, avg **31.22ms**, max 55.54ms, >=8/16/32ms: 8/7/4.
- Backspace: 36, avg 5.85ms, max 24.09ms.
- Suggestion scheduling: 485, avg **7.44ms**, max 197.73ms, 42 >=16ms.
- Suggestion rendering: 528, avg 2.21ms, max 92.15ms, 5 >=16ms.
- Rise: 600, avg 0.12ms.
- Keyboard layout refresh: 68, avg 19.06ms, max 152.22ms, 28 >=16ms.
- Theme/color: 87, avg 0.54ms.
- Predictor worker: 225, avg 5.42ms, max59.29ms, 34 >=16ms.
- Word-at-cursor: 349, avg 1.66ms, max88.48ms.
- Editor preparation: 332, avg0.60ms.
- Chip binding: 522, avg0.58ms.
- Split-phrase worker: 174, avg0.06ms.
- Initial key editor read: 296, avg1.70ms, **max202.37ms** (a plausible explanation for one extreme letter stall).
- Verified composing edit: 283, avg0.31ms.
- Idle word observation: 296, avg **0.07ms**, max0.16ms.
- Space learning dispatch: 7 avg0.03ms.
- Space finish/commit: 7 avg0.09ms.
- Approved learning worker: 40 avg0.37ms.
- Space precommit boundary: 7 avg1.26ms.
- Space after commit: 7 avg4.43ms.
- Deferred idle word verification: 36 avg7.48ms (one 211.50ms outlier).
- Background learning overflow drops 0.
- Stale untappable chips hidden **42**.
- Outdated prediction frames discarded **0**; recovery retries **0**.
These counters are nested and do not directly measure touch-to-display latency. Different sessions/configurations limit strict comparisons.

Comparison to user's Build 413 audit: letter avg14.15ms ->3.55ms (approximately 75% lower); idle-word observation 11.64ms ->0.07ms; Space29.44ms ->31.22ms (not improved); suggestion entry4.91ms ->7.44ms (more expensive); 413 Calm OFF vs 414 Calm ON. Rise/theme/chip binding remain small. Never describe 415 as a tested fix.

## Source changes in Build 415
1. **Reversible safe editor sample skip**, default ON. On an already-active ordinary composing word, a subsequent single ASCII letter no longer issues a separate `getTextBeforeCursor(1,0)` just for punctuation/cursor recovery. The pure `FastKeySample415.skipInitialBeforeRead` gate rejects first letters, punctuation, apostrophes, nonalphabetic, selection, editing-in-middle, private/numeric/symbol/no-prediction fields. `prepareCursorEdit` and `ComposingMutation340.append` continue their editor/session verification before changing text; failing/ambiguous operations use the prior recovery. Toggle **Safe composing editor sample ON/OFF**, pref `orivra_safe_sample_415`. This reduces one IPC opportunity, but cannot eliminate host editor stalls.
2. **Faster FULL prediction wake after a genuine typing pause**, default ON. `IdlePredictionPaint410` now has configurable quiet interval; Build 415 chooses **145ms** rather than original **210ms**. An uninterrupted typing burst still defers full/expensive model and UI updates. `orivra_quick_wake_415` OFF restores 210ms. This aims to reduce the apparent "lost predictions" delay after typing stops. It does **not** provide live full suggestions during uninterrupted fast typing; do not claim otherwise. Future work may require an independently tested lightweight preview that does not block keys.
3. **Cached haptic settings**: vibration enabled/style read once at each editor activation and reused on ACTION_DOWN. OFF/ON user preference remains independent and applies on reopening a text field. The audit now measures the haptic touch call (`HAPTIC_TOUCH_415`) to compare physical vibration cost. No direct vibrator permission requested.
4. **Avoid full layout rebuild on Backspace Shift change** in normal alphabet mode: use `refreshShiftCase398` in place; existing symbol/numeric fallback unchanged.
5. **New non-PII audit counters** in `PredictionDispatcher`: jobs scheduled, pending jobs preempted, worker results delivered. No words/suggestions stored. Coupled with 414 stale-chip/frame counters, distinguishes predicted work that never reaches the bar. Worker callbacks can be dropped later by IME validation even if dispatcher delivered, so these are not final visible-paint counts.
6. Additional Space metrics: `SPACE_SHIFT_415` for post-commit caret/sentence state, `SPACE_REFRESH_415` for prediction scheduling. Space averaged 31.22ms in 414, while measured pre/commit/post pieces accounted for much less; use these stages before assuming its dominant cause.
7. All modern full settings, V4 backup, SentenceFlow and learning, split async decoder, Rise panel and 414 fixes remain. Separate APK identity: `com.chroomareborn.keyboard.build415speedtrial`, versionCode 415, versionName `1.0.69-build415-quick-wake`, label `Orivra 415 Speed Test`. Doesn't inherit data/settings automatically from 414.

## Verification status
- Structural inspection of modified Java source files shows balanced braces/parentheses; source contains app ID/version, haptic caching, safe editor gate, fast quiet policy, separate settings switches, and guarded build script. Structural checks do NOT establish Android compilation.
- Equivalent local copies of the 415 pure Java helper/test sources compiled using `javac --release 17` and yielded **FastKeySample415Test PASS (13 checks)** and **FastIdlePrediction415Test PASS (49 checks)**; these are local copies, not a byte-for-byte GitHub checkout or Android build.
- On-phone build script `scripts/run_orivra415_core_tests.sh` tests the actual clone before Gradle: inherited ComposingMutation413FastPathTest (14 checks), IdleWordCandidate414Test (7 checks), FastKeySample415Test (13), and FastIdlePrediction415Test (49). The actual branch hasn't yet been compiled on user's phone; no claim of lag resolution.
- No Android emulator or device measurement is available from this chat environment.

## Samsung phone compilation
Android Code Studio with prior JDK17, Gradle 8.9 wrapper from previous exported Build 414 and native ARM64 AAPT2 at `$HOME/.androidide/aapt2`:
```bash
cd ~
git clone --depth 1 --single-branch --branch candidate/build415-fast-type-preview-recovery-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Speed415
bash ~/Orivra-Speed415/scripts/build_orivra415_android_code_studio.sh
```
If already cloned but not exported, `cd ~/Orivra-Speed415 && git pull --ff-only && bash scripts/build_orivra415_android_code_studio.sh`.
The guarded script checks exact branch, refuses to overwrite an existing nonempty export, exports source to `/storage/emulated/0/AndroidIDEProjects/Orivra415Speed`, copies Gradle wrapper from `Orivra414Live`, passes native AAPT2 override and confirms APK output-metadata app ID/version.
Expected APK after BUILD SUCCESSFUL + identity passed: `/storage/emulated/0/AndroidIDEProjects/Orivra415Speed/app/build/outputs/apk/debug/app-debug.apk`. Do NOT install if build fails.

## Device test protocol and future work
A. Record Build 414 haptic ON vs OFF impression, with same editor/settings and optionally audit. Do not infer outcome before user tells us.
B. For Build 415, FULL prediction, Idle paint ON, Calm same as chosen comparator, Stable frame ON, Deferred idle word ON, Quick prediction recovery ON, Safe composing editor sample ON; perform normal/fast typing here using nonsensitive text, verify no missing/doubled letters, verify active suggestion chips are tappable/correct and update after 145ms pause. Use Gboard for daily if trial unacceptable.
C. Toggle Quick prediction recovery OFF, reopen editor, repeat; distinguish faster prediction recovery vs extra stutter. Toggle Safe composing editor sample OFF independently and repeat to catch any composing regression.
D. Turn audit ON, RESET counters, type 30–60 seconds of same general text, copy report. Compare average and max letter/Space with 414; audit Haptic, initial editor reads, Space cursor/sentence and prediction scheduling, model jobs scheduled/preempted/completed, stale chip hides, discarded frames. If still >100ms initial editor read outliers, consider a guarded combined `getSurroundingText` or editor snapshot cache that preserves correctness and privacy.
E. If prediction jobs complete yet chips stop appearing, the problem is UI acceptance/snapshot validation. If very few jobs complete because rapid input constantly postpones/cancels requests, implement carefully throttled lightweight prefix preview (possibly worker-calculated) without heavyweight FULL rendering mid-typing. Preserve safe stale-editor tap checks.
