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
