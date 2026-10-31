# Orivra Build 409 — prediction latency isolation
Checkpoint: 2026-10-31

Correct source: ItsJavs2026/Orivra-APK-Builds
Branch: candidate/build409-prediction-isolation-20261031
Parent: candidate/build408-calm-prediction-reply-20261031
Previous private Chrooma-Reborn candidate-402 branch was an older Alpha, missing settings. DO NOT use it as the full Orivra release.

## Current user report
On Samsung Galaxy S26 Ultra, Android 17, full-settings Build 408 is somewhat smoother but still has noticeable typing delay and is not usable as daily keyboard. Lag is especially evident with unfamiliar words, background music, and SystemUI notification quick replies. User switched to Gboard. No quantitative profiler trace yet. Do not claim the issue is fixed.

## Hypothesis from code
The async SentenceFlow worker may contend for CPU on unknown words. The main-thread renderSuggestions() still computes split suggestions, measures and changes prediction chip text/layout/listeners, and updates the expanded Rise panel. Build 408 reduced auto-expansion and throttled paints but did not eliminate these paths. The keyboard typing path also makes InputConnection calls, so the root cause is not yet proven.

## 409 mode experiment
A new Prediction diagnosis button appears at the top of the complete settings UI. It cycles:
OFF (default): no scheduled prediction suggestions or chip-row painting, ordinary key composition and learning paths retained.
WORKER ONLY: sample the existing background word prediction worker but discard output without rendering chips.
FULL: regular Build 408 calm prediction and full suggestion-bar rendering.
Cycle setting in the launcher activity and reopen the text field to activate. No typed text or personal logs are stored. The modes allow an imperfect but useful isolation of background CPU versus display work versus keyboard/editor work.

Changes:
- PredictionIsolationMode409.java: pure policy for modes.
- PredictionIsolationMode409Test.java: 14 pure Java assertions; locally recreated helper/test passed with javac 21, not Android compilation.
- RebornImeService.java: worker-only/off shortcuts in showSuggestions and renderSuggestions; candidate row cleared only once; pending generation gate remains; debug mode read on editor startup.
- PredictionDispatcher.java: optional lazy warming, to avoid startup lexicon work in the isolated IME.
- MainActivity.java: full settings preserved; new diagnostic selector and explanation.
- app/build.gradle.kts: applicationId com.chroomareborn.keyboard.build409isolationtrial, versionCode 409, versionName 1.0.63-build409-prediction-isolation, label Orivra 409 Isolation Test.
This is SOURCE ONLY. Build 409 has not yet been compiled or tested on Samsung.

## Phone-first compile instructions
Android Code Studio on S26 Ultra: Java 17, Gradle 8.9 wrapper; native ARM64 AAPT2 at $HOME/.androidide/aapt2. Standard Maven AAPT2 fails for ARM64, use override.

cd ~
git clone --depth 1 --single-branch --branch candidate/build409-prediction-isolation-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Isolation
mkdir -p /storage/emulated/0/AndroidIDEProjects/Orivra409Isolation
git -C ~/Orivra-Isolation archive HEAD | tar -xf - -C /storage/emulated/0/AndroidIDEProjects/Orivra409Isolation
cp -r /storage/emulated/0/AndroidIDEProjects/Orivra408Calm/gradle /storage/emulated/0/AndroidIDEProjects/Orivra409Isolation/
cp /storage/emulated/0/AndroidIDEProjects/Orivra408Calm/gradlew /storage/emulated/0/AndroidIDEProjects/Orivra409Isolation/
cd /storage/emulated/0/AndroidIDEProjects/Orivra409Isolation
bash gradlew -Pandroid.aapt2FromMavenOverride="$HOME/.androidide/aapt2" :app:assembleDebug --no-daemon --max-workers=2
cat app/build/outputs/apk/debug/output-metadata.json

Expected packageId com.chroomareborn.keyboard.build409isolationtrial and versionCode 409. Install only after BUILD SUCCESSFUL and identity verification. Do NOT uninstall existing candidate, wipe its app data, or remove Gboard. Separate test package does not inherit learned word data automatically.

## Testing
Use same harmless difficult input, fast normal sentence, Space/Backspace, background music and quick replies in each setting. Test OFF, then WORKER ONLY, then FULL, reopening text field between modes.
OFF smooth, WORKER ONLY slow: worker CPU possible.
OFF & WORKER ONLY smooth, FULL slow: prediction chip UI/popup possible.
OFF still slow: InputConnection/typing or other system-side work may contribute.
This experiment alone is not proof; use profile samples if necessary.

Current status: user device compilation and test pending.

## Additional verified hypothesis
The inherited PredictionUiBudget408.delayAfterKey() implementation returns MIN(150 ms, MAX(1 ms, 360 ms minus time since burst start)). After about 360 ms of continuous typing, each further key can re-arm the full prediction refresh with only 1 ms until the pending callback executes and resets the burst. This means Calm mode does not guarantee that heavy suggestion updates wait for an actual typing pause. This behavior is source-verified; its effect on Samsung touch responsiveness is NOT yet measured. Preserve the full mode's 408 implementation in 409 to compare against OFF and WORKER ONLY; a future follow-up should test a true idle-only paint policy (e.g. 180-250 ms after the last key with no forced 1 ms update).

## Additional direct user observation (2026-10-31)
The same noticeable typing delay also occurs **in the ChatGPT Android app's ordinary chat composer** while the user is messaging the assistant. It is not limited to SystemUI notification quick replies or music playback. Add the **ChatGPT conversation field** to the first and highest-priority Samsung test: same harmless phrase in OFF, WORKER ONLY, and FULL modes, reopening the field after each toggle. Notification replies while music plays are the second test. Avoid blaming SystemUI or audio alone; cross-editor IME behavior is now more plausible.

## Verification and 2026-10-31 audit follow-up
- Independently compiled PredictionIsolationMode409.java and PredictionIsolationMode409Test.java using local `javac --release 17`; the run reported **14/14 checks passed**. This checks only the pure Java mode policy, NOT the complete Android IME or its on-device behavior.
- GitHub commit `80cb02c073fdfa945b512ded6e86040fdbfce009`: WORKER ONLY also samples committed-character editors when `composing` is empty by reading a current word at the cursor during the deferred refresh pass (not during a key touch), scheduling `scheduleEditing` and dropping its result. Before this patch, such an editor could falsely look fast because the background worker did no prediction work.
- The selection callback and direct editor operations can still do Android InputConnection IPC in all modes, so OFF is not a zero-overhead bare IME. OFF vs WORKER ONLY vs FULL remains diagnostic rather than proof of cause.
- Changes are committed to the branch; the Android 409 APK still **has not been built or tested**. Do not use a stale exported project snapshot after new commits: cloning now will capture this patch, but existing phone clones must `git pull --ff-only` then re-export before building.

## Simplified, guarded Android Code Studio build (2026-10-31)
New checked-in script: `scripts/build_orivra409_android_code_studio.sh`. It exports current Git HEAD into a new `AndroidIDEProjects/Orivra409Isolation` folder, copies the working 8.9 wrapper from the existing `Orivra408Calm` project, uses the native ARM64 AAPT2 override, builds debug APK, and verifies packageId and versionCode. It **refuses to overwrite** a nonempty 409 project folder and does not modify 408/407, existing app data, or GitHub Actions.
Phone commands:
```bash
cd ~
git clone --depth 1 --single-branch --branch candidate/build409-prediction-isolation-20261031 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Isolation
bash ~/Orivra-Isolation/scripts/build_orivra409_android_code_studio.sh
```
If the user has ALREADY cloned an earlier state of this 409 branch, run `cd ~/Orivra-Isolation && git pull --ff-only` first so that the helper/script and latest service corrections are present. If the destination Orivra409Isolation already contains files, the script deliberately stops rather than destroying any work; inspect existing project before deciding how to refresh it.
APK expected at `/storage/emulated/0/AndroidIDEProjects/Orivra409Isolation/app/build/outputs/apk/debug/app-debug.apk`. Still source-only as of this note: no real Samsung APK compile yet.

## User installation update — 2026-10-31
User reported **"Installed"** after using the Android Code Studio Build 409 workflow. This supports successful installation on their Samsung Galaxy S26 Ultra, but does not itself verify the exact build metadata, active keyboard selection, or any runtime performance.
Next device test: select Orivra 409 Isolation Test, confirm Prediction diagnosis OFF (default), reopen the ChatGPT compose field, manually type normal sentences and unfamiliar joined/misspelled text. Report whether keys have latency with no suggestion-bar processing. After that compare WORKER ONLY and FULL in the same composer, then notification quick reply with music. Gboard remains fallback. No lag fix yet verified.

## Samsung device diagnostic result — 2026-10-31, mode OFF
User tested Build 409 in the ChatGPT app with `Prediction diagnosis: OFF` and reported **"Smooth — close to Gboard"**. This is direct handset feedback, not a measured latency trace. It strongly implicates work associated with enabling prediction, but cannot yet distinguish background SentenceFlow computation from suggestion chip rendering, UI callbacks, or different work triggered in FULL.
**Next required comparison**: switch Prediction diagnosis from OFF to WORKER ONLY (one tap), reopen the ChatGPT field, manually type the same ordinary and difficult nonsensitive sentences; note whether typing remains smooth. WORKER ONLY computes composing/cursor word suggestions on a worker and discards them without rendering. Keep Calm prediction display ON. If smooth, run FULL for contrast; if laggy, profile/optimize the worker. Do not change app source or infer definitive cause until results. Keep Gboard installed.

## Samsung device diagnostic result — 2026-10-31, WORKER ONLY smooth
After the OFF-mode "Smooth — close to Gboard" result, the user selected **A**, meaning **WORKER ONLY is also smooth, still close to Gboard**, while typing in ChatGPT on the Galaxy S26 Ultra. This is real user-reported feel, not quantified timing. The background word prediction worker running without painting suggestion results did not produce noticeable lag in that comparison. The **suggestion rendering, main-thread candidate merges, view update, or other FULL-only work** is now the leading hypothesis, but WORKER ONLY does not execute every context-specific path that FULL does, so this is not proof of a rendering-only cause.
NEXT: user should tap Prediction diagnosis once to switch WORKER ONLY → FULL, keep Calm prediction display ON, reopen ChatGPT text field, type normal and difficult text, and report Smooth / Slight delay / Clearly laggy or freezes. If FULL triggers lag, narrow down main-thread candidate handling (renderSuggestions, split predictions, long-phrase resizing, Rise animations, listener/background updates, and synchronous InputConnection reads). Preserve user learning and existing Gboard; avoid touching stable candidate.

## Samsung device diagnostic result — 2026-10-31, FULL mode lag
User tried Prediction diagnosis FULL with Calm mode ON in ChatGPT and reports typing delay **"in the middle of B and C"**, where B = slight noticeable delay and C = laggy/delayed keys or occasional freezes. OFF and WORKER ONLY were smooth, close to Gboard. This strongly narrows the leading suspect to FULL-only work (main-thread candidate merging/selection/editor queries, prediction-bar updates and expansion), not the sampled background word predictor alone. WORKER ONLY does not exercise every FULL path, so root cause not proven.
Prioritize reducing cost and frequency of UI-thread operations in renderSuggestions and FULL-only pre-render merges. Preserve functionality and document the test as subjective user feedback rather than measured frame times. Continue from correct full-featured repository. Do not claim resolved or ask user to reinstall 409.
