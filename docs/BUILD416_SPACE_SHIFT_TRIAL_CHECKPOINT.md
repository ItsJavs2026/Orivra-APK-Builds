# Orivra Build 416 — opt-in Space cursor/shift latency trial
Date: 2026-10-31
Branch: `candidate/build416-space-shift-fastpath-20261031`
Based on Build 415 `candidate/build415-fast-type-preview-recovery-20261031`, plus the 416 research checkpoint.
**Updated status:** user has now run Build 416 on their Samsung S26 Ultra and supplied a live Build 416 audit. Build identity/runtime functionality is supported by the handset report; an independently reviewed Gradle compile log remains unavailable. A locally reconstructed equivalent Java17 guard/test passed 46 checks, not a byte-for-byte test of the GitHub checkout.

## Latest ACTUAL user report: Build 415, device audit (1110 seconds)
Configuration: FULL, Idle paint ON, Calm ON, Rise ON, stable frames ON, Fast Compose ACK ON, background Space learning ON, deferred idle word ON, Quick Wake 145ms ON, Safe editor sample ON, haptics ON, Shaded Surface theme. Device: Galaxy S26 Ultra / Android 17. User says 415 feels improved, not a massive difference.

| Stage | Build 414 | Build 415 | Notes |
| --- | --- | --- | --- |
| Letter average | 3.55ms (n=296) | **2.55ms (n=177)** | -28%, sessions not controlled |
| Letter maximum | 204.84ms | **68.71ms** | -66% |
| Space average | 31.22ms (n=8) | **18.09ms (n=8)** | -42%; small sample |
| Space maximum | 55.54ms | **33.14ms** | -40% |
| Initial editor read | avg1.70ms max202.37ms (n=296) | **avg0.55ms max1.40ms (n=43)** | 415 gate avoids many redundant reads |
| Suggestion scheduling | avg7.44ms max197.73ms | **avg7.03ms max124.29ms** | Still occasional spikes |
| Suggestion rendering | avg2.21ms max92.15ms | **avg2.27ms max38.59ms** | Slow frames remain |
| Layout refresh | avg19.06ms max152.22ms | **avg16.74ms max122.52ms** | Still costly |
| Idle word observation | avg0.07ms | **avg0.07ms** | 414 gains preserved |
| Haptics | no separate stage | **avg0.24ms max2.39ms (n=218)** | Low cost in this audit |
| Space cursor/sentence state | no separate stage | **avg13.19ms max26.41ms (n=8)** | ~73% of total Space timer |
| Space refresh scheduling | no separate stage | **avg0.07ms max0.16ms** | Not the Space bottleneck |

Other user 415 metrics: worker n142 avg7.34ms max34.55ms; word-at-cursor n116 avg1.38ms max19.42ms; space finished/committed IPC avg0.62ms; 52 stale untappable chips hidden (intentional); 0 discarded frames; 0 refresh retries; 144 worker jobs scheduled, 7 preempted, 137 delivered; background queue overflow drops 0. Delivered does NOT mean painted. Stage timings overlap, do not sum.

## 416 change — narrow, OFF by default, independent
New pure helper: `SpaceShiftFastPath416.java` and `SpaceShiftFastPath416Test.java`, 46 logical checks on an equivalent local Java17 copy.
Setting in MainActivity: `Fast Space cursor/shift (experimental)` pref `orivra_fast_space_shift_416`, **OFF by default**; activation requires reopening editor.
- In `space()`, capture only current in-memory composition before successful finish/commit and check: current editor/session unchanged, prior editing state ordinary, no selection or trailing text, not numeric/private/symbol/caps lock, last code unit in composition is a letter.
- If enabled and those conditions pass, set `shift=false` after literal Space without the extra post-Space `getTextBeforeCursor(160,0)` call. This matches existing pure `SentenceCapitalization.shouldCapitalize()` for normal letter-ending word followed by Space; however, **host transformations/autocorrect may invalidate this assumption** and require phone regression testing.
- Otherwise use original `updateSentenceShift()` path without new capitalization changes.
- Retain the post-Space `cursorHasTextAfter()` safety check. Therefore improvement is contingent on editor read behavior and may be smaller than 13.19ms. No claim of a measured speedup.
- Aggregate audit counts new `Space quick-shift fast path used` and `Space quick-shift fallback to editor` (no words or app IDs).
- All existing 415 switches/learning/privacy/backup/keyboard controls preserved.

## Separate package, one-command phone build
Gradle identity: `com.chroomareborn.keyboard.build416spacetrial`
Version: `416` / `1.0.70-build416-space-fastpath`
Label: `Orivra 416 Space Test`
Scripts: `scripts/run_orivra416_core_tests.sh` (inherits 413/414/415 tests + 416 test); `scripts/build_orivra416_android_code_studio.sh`. Guarded export `/storage/emulated/0/AndroidIDEProjects/Orivra416Space` refuses overwrite, copies known-good 8.9 wrapper from `Orivra414Live`, needs native ARM64 AAPT2 `$HOME/.androidide/aapt2`, verifies APK metadata before installation.

```bash
cd ~
git clone --depth 1 --single-branch \
  --branch candidate/build416-space-shift-fastpath-20261031 \
  git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Space416
bash ~/Orivra-Space416/scripts/build_orivra416_android_code_studio.sh
```
Only use APK after `BUILD SUCCESSFUL` and identity verification:
`/storage/emulated/0/AndroidIDEProjects/Orivra416Space/app/build/outputs/apk/debug/app-debug.apk`.
Do not uninstall 415, Gboard, or wipe personal learning. Separate package has separate data.

## Strict A/B protocol
- On 416 default **Fast Space shift OFF**, with other settings matched to 415 (FULL, Calm ON, Idle Paint ON, Quick Wake ON, Safe Sample ON, haptics ON). Type 30–60 sec of harmless sentences; use enough spaces for useful statistics. Reset + copy audit. Confirm correct auto-cap after punctuation/newline, cursor edits, selection, keyboard stability.
- Enable Fast Space shift ON, **reopen editor**, repeat similar ordinary typing with audit reset. Check the `Space quick-shift fast path used` count is nonzero; if zero, all spaces took fallback and a speed comparison is uninformative.
- Compare Space handler and Space cursor/sentence mean/max and counts; do not compare nested timings as additive or infer significance from n=8.
- Verify new sentence capitals after `Hi. `, `Really? `, newline; Spanish `mañana `; middle-of-message selection; Space in notification quick replies while music plays; fast typing and predictions do not worsen.
- If any wrong shift/case or missed key appears, turn 416 setting OFF and reopen editor (or switch to 415/Gboard). Do not alter personal data.
- If test passes and Space is materially faster, investigate remaining layout refresh / suggestion scheduling spikes on separate 417 branch. Never add heavyweight prediction work to the keypress handler.

## Validation limits
Code and configuration have been committed to GitHub and inspected after write. A **separate locally reconstructed** implementation of `SpaceShiftFastPath416` and its Java17 tests passed all **46** assertions using local javac/java. There is no Android SDK/Gradle installed in this chat execution environment, and no access to the user's Samsung runtime, so **Android compilation and device measurements remain pending**. The on-phone script will run tests from the actual checkout.


## NEW LIVE SAMSUNG BUILD 416 AUDIT — 2026-10-31
The user reported that Build 416 was downloaded and provided a fresh ON-device performance report. This is a separate 870s run (NOT a repeated Build 415 report). Settings: FULL, Idle Paint ON, Calm ON, Rise ON, Frame ON, Fast Compose ACK ON, Background Space Learning ON, Deferred Idle Word ON, Quick Wake 145ms ON, Safe Editor Sample ON, **Fast Space shift 416 ON**, **Haptics OFF**, Shaded Surface theme.

| Stage | Build 415 (1110s) | Build 416 (870s) | Comment |
| --- | --- | --- | --- |
| Letter handler | n177 avg2.55ms max68.71ms | n212 avg3.31ms max35.32ms | Mean +30%, max -49%; different session |
| Space handler | n8 avg18.09ms max33.14ms | n9 avg25.46ms max49.47ms | +41% mean; sample small / not controlled |
| Space cursor/sentence | n8 avg13.19ms max26.41ms | n8 avg14.89ms max19.97ms | +13% mean despite fast path |
| Space suggestion refresh schedule | n8 avg0.07ms | n8 avg0.03ms | Negligible; not bottleneck |
| Space precommit checks | n8 avg1.20ms | n8 avg2.29ms | Candidate contributor |
| Space postcommit learning | n8 avg2.85ms | n8 avg5.13ms | Candidate contributor |
| Space finish/commit | n8 avg0.62ms | n8 avg0.10ms | Low; not bottleneck |
| Suggestion scheduling | n285 avg7.03ms max124.29ms | n410 avg6.61ms max126.20ms | Similar; periodic main-thread spikes |
| Suggestion rendering | n343 avg2.27ms max38.59ms | n446 avg2.11ms max38.17ms | Similar |
| Keyboard layout | n28 avg16.74ms max122.52ms | n47 avg16.55ms max67.17ms | Similar mean, reduced observed max |
| Predictor worker | n142 avg7.34ms max34.55ms | n189 avg5.44ms max40.32ms | Background cost, not key latency |
| Key initial editor read | n43 avg0.55ms max1.40ms | n60 avg0.73ms max1.19ms | Guard continues to help |
| Idle word observation | n177 avg0.07ms max0.52ms | n212 avg0.46ms max14.08ms | Still small mean |
| Word-at-cursor | n116 avg1.38ms max19.42ms | n195 avg1.82ms max20.90ms | Some slow reads |
| Haptic touch | ON avg0.24ms | OFF avg0.00ms | Confounded; not main explanation |

Additional Build 416: Backspace n20 avg6.53 max24.90ms, stale untappable chips hidden 64, discarded frames 0, retries 0, learning queue overflow 0, prediction jobs 192 scheduled / 8 preempted / 184 delivered. **Space quick-shift fast path used 8 times; fallback 0**. This establishes feature eligibility/execution, not a speedup.

### Source interpretation and action
1. The new 416 fast path skips only the 160-char `updateSentenceShift()` preceding-text read. It **does not skip** `cursorHasTextAfter()`, which calls Android `InputConnection.getTextAfterCursor(1,0)` inside the same 14.89ms stage. That IPC is a **hypothesis** for the remaining latency, not proven by a dedicated timer. The unchanged timed stage includes the suffix query plus state checks; it is incorrect to conclude that the guarded shift shortcut removed the 13ms bottleneck.
2. Build 416's mean Space handler is worse than 415's in these unmatched samples despite fast path success; no claim of a regression or gain. Haptics changed ON→OFF between runs and sample sizes differ. The next test should **hold haptics OFF**, all other 416 settings unchanged, toggle only `Fast Space cursor/shift (experimental)` **OFF**, reopen editor, reset audit and type a similar benign 30–60s sample with **30+ ordinary Spaces** if comfortable. Compare ON/OFF within 416. Expect more stage counts than ~8, and avoid judging from single outliers.
3. No arbitrary shortcut should remove `cursorHasTextAfter()` or assume the insertion caret is at the end; it guards against Space insertion in the middle of long drafts, selections, host auto-correction/reformatting and remote SystemUI editors.
4. If matched test confirms no benefit, recommend leaving 416 fast Space toggle OFF or use 415. The next candidate should add **diagnostic sub-stage timing** around `getTextAfterCursor(1,0)`, separate from `updateSentenceShift()`, before any new behavior optimization. Keep these reports aggregate-only with no text collection.
5. Maintain Build 417 dictionary research as a **separate stream** (`research/build417-full-dictionary-activation-20261031`) so word-pack activation does not confound latency experiments. Preserve 415 / 416 sources, installed package data and backups.
