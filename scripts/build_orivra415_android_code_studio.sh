#!/usr/bin/env bash
# Orivra Build 415: one-command local APK build inside Android Code Studio.
# Run after cloning the candidate/build415-fast-type-preview-recovery-20261031 branch.
# No root, GitHub Actions, user-data edits, or SDK changes.
set -euo pipefail

SOURCE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKSPACE="/storage/emulated/0/AndroidIDEProjects"
PROJECT="$WORKSPACE/Orivra415Speed"
WRAPPER_SOURCE="$WORKSPACE/Orivra414Live"
AAPT2="$HOME/.androidide/aapt2"
EXPECTED_BRANCH="candidate/build415-fast-type-preview-recovery-20261031"
EXPECTED_ID="com.chroomareborn.keyboard.build415speedtrial"

echo "Orivra 415 — fast typing and quicker prediction recovery APK"
if [ ! -d "$SOURCE/.git" ]; then
  echo "ERROR: Run this script from the Git-cloned source (not a Git archive)." >&2
  exit 1
fi
BRANCH="$(git -C "$SOURCE" branch --show-current)"
if [ "$BRANCH" != "$EXPECTED_BRANCH" ]; then
  echo "ERROR: Unexpected source branch: $BRANCH" >&2
  exit 1
fi
echo "Branch: $BRANCH"
echo "Commit: $(git -C "$SOURCE" rev-parse --short HEAD)"
bash "$SOURCE/scripts/run_orivra415_core_tests.sh"

if [ ! -f "$WRAPPER_SOURCE/gradlew" ] ||
   [ ! -f "$WRAPPER_SOURCE/gradle/wrapper/gradle-wrapper.jar" ] ||
   ! grep -q "gradle-8.9-bin.zip" "$WRAPPER_SOURCE/gradle/wrapper/gradle-wrapper.properties"; then
  echo "ERROR: The known-good Gradle 8.9 wrapper is missing from Orivra414Live." >&2
  exit 1
fi
if [ ! -x "$AAPT2" ]; then
  echo "ERROR: Native Android-compatible AAPT2 not found at $AAPT2" >&2
  exit 1
fi
if [ -d "$PROJECT" ] &&
   [ -n "$(find "$PROJECT" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
  echo "ERROR: $PROJECT already contains files. Refusing to overwrite a previous test build." >&2
  echo "Use a fresh project path or deliberately archive the old working copy first." >&2
  exit 1
fi

mkdir -p "$PROJECT"
# Export only tracked files; no .git history, SSH credentials, or untracked files.
git -C "$SOURCE" archive HEAD | tar -xf - -C "$PROJECT"
cp -r "$WRAPPER_SOURCE/gradle" "$PROJECT/"
cp "$WRAPPER_SOURCE/gradlew" "$PROJECT/"

cd "$PROJECT"
echo "Compiling from exported commit: $(git -C "$SOURCE" rev-parse --short HEAD)"
bash gradlew -Pandroid.aapt2FromMavenOverride="$AAPT2" \
    :app:assembleDebug --no-daemon --max-workers=2

METADATA="app/build/outputs/apk/debug/output-metadata.json"
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK" ] || [ ! -f "$METADATA" ]; then
  echo "ERROR: Build returned without the expected APK and metadata." >&2
  exit 1
fi
if ! grep -Fq "\"applicationId\": \"$EXPECTED_ID\"" "$METADATA" ||
   ! grep -Fq "\"versionCode\": 415" "$METADATA"; then
  echo "ERROR: APK metadata identity mismatch. Do not install." >&2
  cat "$METADATA"
  exit 1
fi

echo
echo "APK BUILT — identity check passed."
echo "File: $PROJECT/$APK"
echo "Metadata:"
cat "$METADATA"
echo
echo "Do not uninstall Gboard or older Orivra versions."
echo "This diagnostic build is a separate app with independent local data."
