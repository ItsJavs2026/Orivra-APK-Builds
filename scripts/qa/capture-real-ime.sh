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
# Fail with an actionable Android service listing, not an unexplained
# pipefail when ime list/grep uses a different component-name spelling.
adb shell ime list -s | tr -d '\r' | tee "$OUT/installed-imes.txt"
IME="$(grep -F "$PKG/" "$OUT/installed-imes.txt" | grep -F 'RebornImeService' | head -n1 || true)"
if [[ -z "$IME" ]]; then
  echo "FAIL: Android did not report Candidate's InputMethodService in ime list"
  echo "--- package registrations ---"
  adb shell dumpsys package "$PKG" | grep -E -C 3 'RebornImeService|android.view.InputMethod|BIND_INPUT_METHOD' || true
  echo "Packages containing Orivra:"
  adb shell pm list packages | grep -F 'chroomareborn' || true
  exit 1
fi
echo "Found IME $IME"
adb shell ime enable "$IME"
adb shell ime set "$IME"
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
