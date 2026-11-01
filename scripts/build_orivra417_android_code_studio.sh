#!/usr/bin/env bash
# Build 417: self-contained phone-side, pinned ESDB source => real APK asset.
# NO GitHub Actions, root, remote text input or changes to older Orivra builds.
set -euo pipefail

SOURCE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKSPACE="/storage/emulated/0/AndroidIDEProjects"
PROJECT="$WORKSPACE/Orivra417OfflineEnglish"
WRAPPER_SOURCE="$WORKSPACE/Orivra414Live"
AAPT2="$HOME/.androidide/aapt2"
BRANCH="candidate/build417-offline-esdb-onphone-20261031"
EXPECTED_ID="com.chroomareborn.keyboard.build417dictionarytrial"
PIN="1e5b7d3a72f47a71da5d28686c1dd4b397178485"
EXPECTED_RAW_SHA="a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600"
PACK_REL="app/src/main/assets/dictionaries/esdb-en-US-60.esdbpack"
APK_PACK_REL="assets/dictionaries/esdb-en-US-60.esdbpack"
APK_COPYRIGHT_REL="assets/dictionaries/ESDB_COPYRIGHT.txt"
ESDB_SOURCE="${ESDB_SOURCE_DIR:-$HOME/.cache/orivra417-esdb-pinned}"

echo "=== Orivra Build 417: real offline English pack ==="
if [[ ! -d "$SOURCE/.git" ]] ||
   [[ "$(git -C "$SOURCE" branch --show-current)" != "$BRANCH" ]]; then
  echo "ERROR: Clone and build from exact 417 source branch $BRANCH" >&2
  exit 1
fi
echo "Source commit: $(git -C "$SOURCE" rev-parse --short HEAD)"
for cmd in git tar gzip sha256sum wc python3 javac java unzip cmp; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: Missing $cmd in Android Code Studio terminal." >&2
    exit 1
  fi
done
python3 - <<'PY'
import sqlite3, sys
v = sqlite3.sqlite_version_info
if v < (3, 33, 0):
    sys.exit("ERROR: SQLite 3.33+ required to generate pinned ESDB. Found "+sqlite3.sqlite_version)
print("ESDB prerequisites: Python",sys.version.split()[0],"SQLite",sqlite3.sqlite_version)
PY
if [[ ! -f "$WRAPPER_SOURCE/gradlew" ]] ||
   [[ ! -f "$WRAPPER_SOURCE/gradle/wrapper/gradle-wrapper.jar" ]] ||
   ! grep -q 'gradle-8.9-bin.zip' "$WRAPPER_SOURCE/gradle/wrapper/gradle-wrapper.properties"; then
   echo "ERROR: Working Orivra414Live Gradle 8.9 wrapper not found." >&2
   exit 1
fi
if [[ ! -x "$AAPT2" ]]; then
   echo "ERROR: ARM64 AAPT2 missing: $AAPT2" >&2
   exit 1
fi
if [[ -e "$PROJECT" ]]; then
   echo "ERROR: Refusing to overwrite existing project $PROJECT" >&2
   exit 1
fi

# The upstream source is PUBLIC; a pinned and previously fetched copy works
# without network. A custom ESDB_SOURCE_DIR is allowed but must match the SHA.
if [[ ! -d "$ESDB_SOURCE/.git" ]]; then
  mkdir -p "$ESDB_SOURCE"
  git -C "$ESDB_SOURCE" init -q
fi
if [[ "$(git -C "$ESDB_SOURCE" rev-parse HEAD 2>/dev/null || true)" != "$PIN" ]]; then
  echo "Fetching the official pinned English Speller Database source once."
  echo "Source: https://github.com/en-wl/wordlist @ $PIN"
  if ! git -C "$ESDB_SOURCE" fetch --depth 1 \
          https://github.com/en-wl/wordlist.git "$PIN"; then
    echo "ERROR: Cannot fetch pinned public ESDB. Internet may be required" >&2
    echo "or supply ESDB_SOURCE_DIR pointing to the exact verified commit." >&2
    exit 1
  fi
  git -C "$ESDB_SOURCE" checkout --detach -q FETCH_HEAD
fi
if [[ "$(git -C "$ESDB_SOURCE" rev-parse HEAD)" != "$PIN" ]]; then
  echo "ERROR: Upstream source commit mismatch." >&2
  exit 1
fi

# Build an isolated staging project; never damage prior APK or partial export.
mkdir -p "$WORKSPACE"
STAGING="$(mktemp -d "$WORKSPACE/.Orivra417-build.XXXXXX")"
cleanup() { rm -rf "$STAGING"; }
trap cleanup EXIT
git -C "$SOURCE" archive HEAD | tar -xf - -C "$STAGING"
cp -r "$WRAPPER_SOURCE/gradle" "$STAGING/"
cp "$WRAPPER_SOURCE/gradlew" "$STAGING/"

echo "Generating exact source-verified dictionary using Python (make not required)..."
bash "$STAGING/scripts/build-esdb-wordpack.sh" "$ESDB_SOURCE"
ASSET="$STAGING/$PACK_REL"
test -s "$ASSET"
test -s "$STAGING/app/src/main/assets/dictionaries/ESDB_COPYRIGHT.txt"
test "$(gzip -dc "$ASSET" | wc -c | tr -d '[:space:]')" = 1022018
ACTUAL_RAW_SHA="$(gzip -dc "$ASSET" | sha256sum | cut -d ' ' -f 1)"
if [[ "$ACTUAL_RAW_SHA" != "$EXPECTED_RAW_SHA" ]]; then
  echo "ERROR: Dictionary expanded checksum mismatch!" >&2
  exit 1
fi

echo "Running inherited 413–416C regressions..."
bash "$STAGING/scripts/run_orivra416c_core_tests.sh"
TEST_CLASSES="$(mktemp -d "$WORKSPACE/.Orivra417-tests.XXXXXX")"
cleanup_tests(){ rm -rf "$TEST_CLASSES"; }
trap 'cleanup; cleanup_tests' EXIT
javac --release 17 -d "$TEST_CLASSES" \
  "$STAGING/app/src/main/java/com/chroomareborn/keyboard/ExactOnlyLanguagePack.java" \
  "$STAGING/app/src/main/java/com/chroomareborn/keyboard/CompressedLanguagePack.java" \
  "$STAGING/app/src/testsupport/java/com/chroomareborn/keyboard/OfflinePackAsset417Test.java"
java -Xmx128m -cp "$TEST_CLASSES" \
  com.chroomareborn.keyboard.OfflinePackAsset417Test "$ASSET"

cd "$STAGING"
echo "Building with existing Android Code Studio Gradle wrapper and ARM64 AAPT2..."
bash gradlew -Pandroid.aapt2FromMavenOverride="$AAPT2" \
  :app:assembleDebug --no-daemon --max-workers=2

APK="app/build/outputs/apk/debug/app-debug.apk"
METADATA="app/build/outputs/apk/debug/output-metadata.json"
if [[ ! -s "$APK" || ! -s "$METADATA" ]] ||
   ! grep -Fq "\"applicationId\": \"$EXPECTED_ID\"" "$METADATA" ||
   ! grep -Fq '"versionCode": 4170' "$METADATA"; then
  echo "ERROR: 417 APK metadata or build output missing/mismatch." >&2
  test ! -f "$METADATA" || cat "$METADATA"
  exit 1
fi
echo "Checking compressed dictionary bytes INSIDE the APK..."
ZIP_PACK_SHA="$(unzip -p "$APK" "$APK_PACK_REL" | sha256sum | cut -d ' ' -f 1)"
SOURCE_PACK_SHA="$(sha256sum "$ASSET" | cut -d ' ' -f 1)"
if [[ "$ZIP_PACK_SHA" != "$SOURCE_PACK_SHA" ]]; then
  echo "ERROR: APK wordpack bytes missing/different from verified source." >&2
  exit 1
fi
unzip -p "$APK" "$APK_COPYRIGHT_REL" > "$TEST_CLASSES/apk-copyright.txt"
cmp "$TEST_CLASSES/apk-copyright.txt" \
    "app/src/main/assets/dictionaries/ESDB_COPYRIGHT.txt"

# Publish verified result atomically; source/older builds are untouched.
mv "$STAGING" "$PROJECT"
echo
echo "SUCCESS: Orivra 417 APK compiled, verified wordpack AND copyright present."
echo "App ID: $EXPECTED_ID"
echo "Words: 78,942 exact English spellings (not 79k fuzzy suggestions)."
echo "APK: $PROJECT/$APK"
echo "Generated pack SHA-256: $SOURCE_PACK_SHA"
echo "Activates only after OFF-by-default opt-in; check runtime status on device."
echo "Keep Build 416/416C and Gboard available for A/B fallback."
