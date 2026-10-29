#!/usr/bin/env bash
# GitHub Actions Android emulator ONLY. Tests the actual installed Orivra
# InputMethodService, not a hand-built visual mock. No user data involved.
set -euo pipefail
PKG=com.chroomareborn.keyboard.build293trial
OUT="$PWD/visual-qa"
mkdir -p "$OUT"

gradle --no-daemon --stacktrace :app:assembleDebug
APK=app/build/outputs/apk/debug/app-debug.apk
test -s "$APK"
adb install -r "$APK"
# Android's InputMethodManager may not index a newly installed keyboard
# synchronously with package installation. Launch the debug-only fixture once
# to resolve its process, then query all registered IMEs with retry.
adb shell am start -W -n "${PKG}/com.chroomareborn.keyboard.MainActivity" \
  --ez orivra_ui_capture true >/dev/null
sleep 4
IME=""
for attempt in 1 2 3 4 5 6 7 8 9 10; do
  adb shell ime list -a -s | tr -d '\r' | tee "$OUT/installed-imes.txt"
  IME="$(grep -F "$PKG/" "$OUT/installed-imes.txt" | grep -F 'RebornImeService' | head -n1 || true)"
  if [[ -n "$IME" ]]; then break; fi
  echo "Waiting for Android IME service registration, pass $attempt/10"
  sleep 4
done
if [[ -z "$IME" ]]; then
  echo "FAIL: Android still did not list Candidate's IME after registration retries"
  echo "--- package registrations ---"
  adb shell dumpsys package "$PKG" | grep -E -C 3 'RebornImeService|android.view.InputMethod|BIND_INPUT_METHOD' || true
  echo "--- Android input-method manager state ---"
  adb shell dumpsys input_method | grep -E -C 2 'build293trial|RebornImeService|mMethodMap' | head -n 100 || true
  exit 1
fi
echo "Found IME $IME"
adb shell ime enable "$IME"
adb shell ime set "$IME"
# Do NOT force-stop the package here: the InputMethodService runs in the
# same package, and killing it can leave the editor displaying fallback Gboard.
# Bring the already-open fixture to foreground, retaining the selected IME.
adb shell am start -W -n "${PKG}/com.chroomareborn.keyboard.MainActivity" \
  --ez orivra_ui_capture true
sleep 4
selected="$(adb shell settings get secure default_input_method | tr -d '\r')"
echo "Selected Android input method: $selected"
if [[ "$selected" != "$IME" ]]; then
  echo "FAIL: Orivra no longer selected (fallback keyboard likely)"
  adb shell dumpsys input_method > "$OUT/input-method-fallback.txt" || true
  exit 1
fi

# ADB's uiautomator dumps accessibility-visible Android views, including the
# actual InputMethodService window, and provides screen bounds for touch.
dump_ui() {
  local label="$1"
  local success=0
  for attempt in 1 2 3; do
    if timeout 30s adb shell uiautomator dump /sdcard/orivra-ui.xml >/dev/null 2>&1 \
      && adb pull /sdcard/orivra-ui.xml "$OUT/${label}.xml" >/dev/null; then
      success=1
      break
    fi
    sleep 2
  done
  if [[ "$success" != 1 ]]; then
    echo "FAIL: Android hierarchy dump failed ($label)"
    exit 1
  fi
}
screenshot() {
  local label="$1"
  adb exec-out screencap -p > "$OUT/${label}.png"
  python3 - "$OUT/${label}.png" <<'PY'
import sys,struct
with open(sys.argv[1],'rb') as f:
    head=f.read(24)
assert head[:8]==b'\x89PNG\r\n\x1a\n', 'Invalid Android PNG'
w,h=struct.unpack('>II',head[16:24])
assert w>=480 and h>=720, (w,h)
print('Captured real Android screen:',sys.argv[1],w,h)
PY
}
# Find an accessibility text/content-description match, click its visible
# center, or fail. No hard-coded pixel positions or screenshot fabrication.
tap_node() {
  local xml="$1" field="$2" needle="$3"
  local coords
  coords="$(python3 - "$OUT/${xml}.xml" "$field" "$needle" <<'PY'
import sys,re,xml.etree.ElementTree as ET
xml,attr,fragment=sys.argv[1:]
root=ET.parse(xml).getroot()
for node in root.iter('node'):
    d=node.attrib
    if fragment in d.get(attr,'') and d.get('bounds'):
        x1,y1,x2,y2=map(int,re.findall(r'\d+',d['bounds']))
        if x2>x1 and y2>y1:
            print((x1+x2)//2,(y1+y2)//2)
            sys.exit(0)
sys.exit(2)
PY
)" || { echo "FAIL: could not find $needle in $xml"; exit 1; }
  echo "Tapping $needle at $coords"
  adb shell input tap $coords
}

# An underprovisioned GitHub emulator can surface a Pixel Launcher ANR
# over the TEST app even though Orivra is installed and selected correctly.
# Recover ONLY this known launcher popup. Orivra ANRs remain a hard failure.
for pass in 1 2 3 4; do
  dump_ui "startup-$pass"
  screenshot "startup-$pass"
  result="$(python3 - "$OUT/startup-$pass.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter('node')]
text=' '.join((x.get('content-desc','')+' '+x.get('text','')) for x in nodes)
if 'Orivra' in text and "isn't responding" in text: print('app-anr')
elif 'Pixel Launcher' in text and "isn't responding" in text: print('launcher-anr')
elif 'Expand theme information and controls' in text: print('ready')
elif 'Orivra emulator keyboard screenshot test editor' in text: print('editor-only')
else: print('unknown')
PY
)"
  echo "Visual QA startup state [$pass]: $result"
  if [[ "$result" == "app-anr" ]]; then
    echo "FAIL: Orivra ANR, do not suppress or ignore"
    adb logcat -d -v brief -t 220 | grep -E -i 'chroomareborn|RebornImeService|ANR' | tail -n 90 > "$OUT/orivra-anr.txt" || true
    exit 1
  fi
  if [[ "$result" == "ready" ]]; then break; fi
  if [[ "$result" == "launcher-anr" ]]; then
    # Close only Android's launcher, not Orivra. The fixture is already
    # foreground. A new attempt will recheck the actual IME visibility.
    tap_node "startup-$pass" text "Close app"
    sleep 2
  elif [[ "$result" == "editor-only" ]]; then
    # UIAutomator dumps the active ACTIVITY hierarchy and omits the IME's
    # separate window even when Orivra is visibly on the screen. Focus once,
    # then proceed to REAL screenshot-based checks instead of looping forever.
    tap_node "startup-$pass" content-desc "Orivra emulator keyboard screenshot test editor"
    sleep 2
    break
  else
    adb shell dumpsys input_method > "$OUT/input-method-state-$pass.txt" || true
    adb shell dumpsys window > "$OUT/window-state-$pass.txt" || true
    echo "Unexpected screen state; retaining screenshot and retrying"
    sleep 2
  fi
done


# Android 35's uiautomator dump can exclude keyboard-window controls even
# though they are visible in screencap. Verify the REAL selected IME using
# settings, then measure the screenshots and inject actual screen taps.
verify_selected() {
  actual="$(adb shell settings get secure default_input_method | tr -d '\r')"
  if [[ "$actual" != "$IME" ]]; then
    echo "FAIL: Android switched away from Orivra to $actual"
    exit 1
  fi
}
dp_px() {
  python3 - "$1" "$density" <<'PY'
import sys
print(round(float(sys.argv[1])*float(sys.argv[2])))
PY
}
density="$(adb shell wm density | tr -d '\r' | grep -oE '[0-9]+' | tail -n1)"
density="$(python3 - "$density" <<'PY'
import sys
print(float(sys.argv[1])/160.0)
PY
)"
screen_w="$(adb shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | head -n1 | cut -dx -f1)"
test -n "$screen_w"

verify_selected
dump_ui collapsed
screenshot collapsed
collapsed_top="$(python3 scripts/qa/inspect_ime_screens.py "$OUT/collapsed.png" --density "$density")"
echo "Verified actual Orivra collapsed keyboard top: $collapsed_top px"
# The palette button has been REMOVED from the prediction strip.
# The existing right caret preserves short-tap expanded predictions and
# long-press (500ms) theme settings. Swipe with zero displacement for 750ms
# to exercise its real Android long-press touch handling.
tap_x="$(dp_px 18)"
tap_y="$((collapsed_top+$(dp_px 24)))"
settings_x="$((screen_w-$(dp_px 20)))"
settings_y="$tap_y"
adb shell input swipe "$settings_x" "$settings_y" \
  "$settings_x" "$settings_y" 750
sleep 2
verify_selected
dump_ui expanded
screenshot expanded
expanded_top="$(python3 scripts/qa/inspect_ime_screens.py "$OUT/expanded.png" --mode expanded --collapsed "$collapsed_top" --density "$density")"
echo "PASS: Orivra real panel grew by $((collapsed_top-expanded_top)) px"

# In the expanded two-row panel, the collapse arrow is at the right-hand
# end of its own 28dp header immediately below the 48dp prediction strip.
collapse_x="$((screen_w-$(dp_px 22)))"
collapse_y="$((expanded_top+$(dp_px 48)+$(dp_px 14)))"
adb shell input tap "$collapse_x" "$collapse_y"
sleep 2
verify_selected
dump_ui recollapsed
screenshot recollapsed
python3 scripts/qa/inspect_ime_screens.py "$OUT/recollapsed.png" \
  --mode recollapsed --collapsed "$collapsed_top" --density "$density"
echo "PASS: real theme panel collapsed to original zero-reserved-row position"

# Tap the real Q key in the first keyboard row, following the 48dp
# predictions strip. Read the activity's EditText through UiAutomator.
key_y="$((collapsed_top+$(dp_px 83)))"
adb shell input tap "$tap_x" "$key_y"
sleep 2
verify_selected
dump_ui typed
screenshot typed
python3 - "$OUT/typed.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter('node')]
texts=[n.get('text','') for n in nodes]
assert any('likethis heythereq' in x for x in texts), (
    'Real IME Q tap did not reach the editor', texts[:24])
print('PASS: actual Orivra key delivered Q to disposable editor')
PY
echo "PASS: real Orivra IME visible, full-width prediction row, theme toggle via right-caret long hold, and key touch"

# An actual Android touch swipe across Orivra's Backspace key. The
# disposable editor currently contains "likethis heythereq". Swipe-left
# one word: it should become exactly "likethis " (including its space).
# Capture during motion for subsequent visual inspection of the preview.
back_x="$((screen_w-$(dp_px 22)))"
back_y="$((collapsed_top+$(dp_px 222)))"
release_x="$((back_x-$(dp_px 55)))"
echo "Testing whole-word Backspace swipe from ($back_x,$back_y) to ($release_x,$back_y)"
adb shell input swipe "$back_x" "$back_y" "$release_x" "$back_y" 1100 \
  > "$OUT/backspace-swipe-command.txt" 2>&1 &
swipe_pid=$!
sleep 0.83
screenshot backspace-preview
wait "$swipe_pid"
sleep 1
verify_selected
dump_ui backspace-result
screenshot backspace-result
python3 - "$OUT/backspace-result.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter('node')]
texts=[n.get('text','') for n in nodes]
assert any(x=='likethis ' for x in texts),(
    'Expected one swipe to delete heythereq as one connected word',texts[:30])
print('PASS: real leftward Backspace swipe deleted the complete connected word')
PY
echo "Backspace highlight motion-frame screenshot saved as backspace-preview.png."

