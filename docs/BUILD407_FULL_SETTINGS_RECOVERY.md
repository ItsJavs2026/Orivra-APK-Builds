# Orivra Build 407 — full-settings source recovery (2026-10-31)

## Critical correction to prior Build 407 workflow
The private `ItsJavs2026/Chrooma-Reborn` branch `orivra/candidate-402-performance` came from an older Alpha 1.0 prototype; its MainActivity was only ~6 KB with a handful of settings and its IME was ~40 KB. A locally compiled debug APK `com.chroomareborn.keyboard.dev407` successfully installed, but its setup page was the old Chrooma Reborn Alpha and lacked the user's Build 400 settings. That is NOT a complete Build 407.

The actual detailed Orivra candidate code lives in PUBLIC `ItsJavs2026/Orivra-APK-Builds`, branch `candidate/build400-center-down-space-order-20261031` (MainActivity ~72 KB, IME ~238 KB) and later `candidate/build401-notification-responsive-20261031` (MainActivity ~63 KB, IME ~244 KB). Both have a full settings surface for theme/height, haptics, instant visual feedback, symbol holds, swipe controls, personal dictionary, personal learning, learning backups, experimental dictionary and diagnostics. The 401 branch introduces notification response changes that must be tested on Samsung.

**Canonical new full-settings recovery branch:** `candidate/build407-full-settings-recovery-20261031`, created directly from the 401 branch. Changed only version/package labels and the launcher title/picker text. It does NOT yet contain all the unrelated fixes previously made on private `orivra/candidate-402-performance`; port them only after comparing against this much more advanced base and confirming they are needed. No 407 full-settings APK has been compiled or installed yet. This is a test baseline, not a finished release.

**Safety:** the new app ID `com.chroomareborn.keyboard.build407fulltrial` is a separate sandbox; it does not migrate personalized settings or learned words automatically. User should keep Build 400, Gboard, and any exported V4 backups; explicit user-controlled import only after verification.

## Mobile Android Code Studio build recipe
Phone: Samsung S26 Ultra / Android 17; IDE package `com.tom.rv2ide`. JDK 17; Gradle 8.9; compiled native Android AAPT2 at `$HOME/.androidide/aapt2`. Prior APK build command succeeded with:
`bash gradlew -Pandroid.aapt2FromMavenOverride="$HOME/.androidide/aapt2" :app:assembleDebug --no-daemon --max-workers=2`.
Original Maven AAPT2 was incompatible with ARM64, giving `Syntax error: "(" unexpected`.

Clone full source and export a separate IDE project (do NOT overwrite old project):
```bash
cd ~
git clone --branch candidate/build407-full-settings-recovery-20261031 --single-branch git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Full
mkdir -p /storage/emulated/0/AndroidIDEProjects/Orivra407Full
git -C ~/Orivra-Full archive HEAD | tar -xf - -C /storage/emulated/0/AndroidIDEProjects/Orivra407Full
cp -r /storage/emulated/0/AndroidIDEProjects/Orivra407/gradle /storage/emulated/0/AndroidIDEProjects/Orivra407Full/
cp /storage/emulated/0/AndroidIDEProjects/Orivra407/gradlew /storage/emulated/0/AndroidIDEProjects/Orivra407Full/
cd /storage/emulated/0/AndroidIDEProjects/Orivra407Full
bash gradlew -Pandroid.aapt2FromMavenOverride="$HOME/.androidide/aapt2" :app:assembleDebug --no-daemon --max-workers=2
cat app/build/outputs/apk/debug/output-metadata.json
```
Expected new package `com.chroomareborn.keyboard.build407fulltrial`, versionCode 407, versionName `1.0.61-build407-full-settings-recovery`, UI title `Orivra — Build 407 Full Settings Test`. Verify actual build log/metadata before installing. Exported IDE snapshot is separate from Git clone; refresh manually after pulling code updates.

## Next engineering steps
- Confirm full settings appear and keyboard can be selected. Compare UI against Build 400; independent install starts with empty prefs and personal learning.
- Avoid touching user's original installed keyboard or data. Test only non-sensitive text.
- Review 401 notification-memory implementation against 402–407 proposed fixes; preserve the richer 401 functionality and regressions. Add targeted fast-path/memory patch only with tests.
- Samsung notification shade with background music, slow/fast typing, Backspace gestures, predictions, palette and learning/export tests are pending; no performance claims yet.
