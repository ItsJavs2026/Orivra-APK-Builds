#!/usr/bin/env bash
# Screenshot-only visual QA. Synthetic example text; Candidate 373 behavior.
set -euo pipefail
PKG="com.chroomareborn.keyboard.build373trial"
IME="$PKG/com.chroomareborn.keyboard.RebornImeService"
OUT="$PWD/placement_screenshots"
mkdir -p "$OUT"
APK=$(find app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -type f -print -quit)
test -s "$APK"
adb install -r "$APK"
# Android 35's InputMethodManager can register a freshly installed IME
# after adb install has already returned "Success".
registered=0
for attempt in $(seq 1 24); do
  if adb shell ime list -s | tr -d '\r' | grep -Fq "$IME"; then
    registered=1
    break
  fi
  sleep 1
done
adb shell ime list -s
if [ "$registered" -ne 1 ]; then
  echo "FATAL: installed IME never registered after PackageManager scan"
  adb shell pm list packages | grep chroomareborn || true
  exit 1
fi
adb shell ime enable "$IME"
adb shell ime set "$IME"
adb shell settings get secure default_input_method
DIM=$(adb shell wm size | grep -Eo '[0-9]+x[0-9]+' | tail -n1)
W=$(printf '%s' "$DIM" | cut -dx -f1)
H=$(printf '%s' "$DIM" | cut -dx -f2)
if [ -z "$W" ];then W=1080;fi
if [ -z "$H" ];then H=2400;fi
echo "Visual QA emulator pixels: $W x $H"
take() {
  local name="$1"
  adb shell screencap -p "/sdcard/$name.png"
  adb pull "/sdcard/$name.png" "$OUT/$name.png"
  file "$OUT/$name.png"
}

# Real settings screen. Scroll past the lengthy historical candidate notes
# until both new controls are physically visible in a single screenshot.
adb shell am force-stop "$PKG" || true
adb shell am start -n "$PKG/com.chroomareborn.keyboard.MainActivity"
sleep 3
found=0
for i in $(seq 1 28); do
  adb shell uiautomator dump /sdcard/orivra_ui.xml >/dev/null 2>&1 || true
  adb shell cat /sdcard/orivra_ui.xml > "$OUT/ui.xml" 2>/dev/null || true
  if grep -q 'Swipe prediction selection:' "$OUT/ui.xml" &&
     grep -q 'Space accepts highlighted correction:' "$OUT/ui.xml"; then
    echo "Both visible on settings screen after $i checks"
    found=1
    break
  fi
  adb shell input swipe "$((W/2))" "$((H*78/100))" \
    "$((W/2))" "$((H*28/100))" 175
  sleep 0.35
done
if [ "$found" -ne 1 ]; then
  echo "FATAL: could not locate both settings controls in same view"
  exit 1
fi
take "candidate373_settings_buttons"

# The app's built-in Android debug-only screenshot editor. This uses the
# installed real IME, not a fabricated keyboard/mockup, and no user text.
adb shell am force-stop "$PKG" || true
adb shell am start -n "$PKG/com.chroomareborn.keyboard.MainActivity" \
    --ez orivra_ui_capture true
sleep 4
adb shell input tap "$((W*26/100))" "$((H*20/100))"
sleep 2
adb shell uiautomator dump /sdcard/orivra_keyboard.xml >/dev/null 2>&1 || true
adb shell cat /sdcard/orivra_keyboard.xml > "$OUT/keyboard.xml" 2>/dev/null || true
echo "Selected IME:"
adb shell settings get secure default_input_method
take "candidate373_keyboard_prediction_strip"
test -s "$OUT/candidate373_settings_buttons.png"
test -s "$OUT/candidate373_keyboard_prediction_strip.png"
echo "PASS: captured actual Android emulator placement screenshot files"
