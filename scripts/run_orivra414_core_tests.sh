#!/usr/bin/env bash
# Pure Java smoke tests: no Android SDK, network, emulator or user data.
set -euo pipefail
SOURCE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$HOME/.cache"
TEMP="$(mktemp -d "$HOME/.cache/orivra413test.XXXXXX")"
trap 'rm -rf "$TEMP"' EXIT
javac --release 17 -d "$TEMP" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/ComposingMutation340.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/ComposingMutation413FastPathTest.java" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/IdleWordCandidate414.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/IdleWordCandidate414Test.java"
java -cp "$TEMP" com.chroomareborn.keyboard.ComposingMutation413FastPathTest
java -cp "$TEMP" com.chroomareborn.keyboard.IdleWordCandidate414Test
