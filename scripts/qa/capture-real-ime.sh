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
# Restart the temporary fixture AFTER selecting Orivra. This prompts Android
# to build its actual IME window for the now-active editor.
adb shell am force-stop "$PKG"
adb shell am start -W -n "${PKG}/com.chroomareborn.keyboard.MainActivity" \
  --ez orivra_ui_capture true
sleep 4

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
    # Focus the visible scratch field to ask Android to show the active IME.
    tap_node "startup-$pass" content-desc "Orivra emulator keyboard screenshot test editor"
    sleep 2
  else
    adb shell dumpsys input_method > "$OUT/input-method-state-$pass.txt" || true
    adb shell dumpsys window > "$OUT/window-state-$pass.txt" || true
    echo "Unexpected screen state; retaining screenshot and retrying"
    sleep 2
  fi
done

dump_ui collapsed
screenshot collapsed
python3 - "$OUT/collapsed.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[x.attrib for x in ET.parse(sys.argv[1]).getroot().iter('node')]
def present(needle):
    return any(needle in (x.get('content-desc','')+' '+x.get('text','')) for x in nodes)
assert present('Expand theme information and controls'), 'No visible theme expand control in REAL IME'
assert present('Orivra emulator keyboard screenshot test editor'), 'QA editor is missing'
assert not present('Collapse theme information and controls'), 'Theme panel should start collapsed'
print('PASS: actual IME visible, theme starts collapsed')
PY

tap_node collapsed content-desc "Expand theme information and controls"
sleep 1
dump_ui expanded
screenshot expanded
python3 - "$OUT/expanded.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter('node')]
data=[x.get('content-desc','')+' '+x.get('text','') for x in nodes]
assert any('Collapse theme information and controls' in x for x in data), 'Expanded theme cannot collapse'
assert any('Theme: App Match' in x for x in data), 'Selected theme not shown on its own header row'
assert any('Tap to select the next theme' in x for x in data), 'Theme selector not visible in control row'
print('PASS: REAL IME has separate theme header, controls, and collapse affordance')
PY

tap_node expanded content-desc "Collapse theme information and controls"
sleep 1
dump_ui recollapsed
screenshot recollapsed
python3 - "$OUT/recollapsed.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[x.attrib for x in ET.parse(sys.argv[1]).getroot().iter('node')]
def present(needle):
    return any(needle in (x.get('content-desc','')+' '+x.get('text','')) for x in nodes)
assert present('Expand theme information and controls'), 'Theme did not reopen correctly after collapse'
assert not present('Collapse theme information and controls'), 'Extra theme row remains after collapse'
print('PASS: actual IME collapses both theme rows')
PY

# One actual keyboard-key tap; the separate emulator EditText fixture starts
# at "likethis heythere". This catches touch dispatch/real-input failures.
tap_node recollapsed text "q"
sleep 1
dump_ui typed
screenshot typed
python3 - "$OUT/typed.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter('node')]
texts=[n.get('text','') for n in nodes]
assert any('likethis heythereq' in x for x in texts), (
    'Real IME key tap did not reach the editor', texts[:24])
print('PASS: actual Orivra keyboard delivered q to the QA editor')
PY

echo "PASS: real Orivra keyboard screenshots, expand/collapse and key dispatch"
