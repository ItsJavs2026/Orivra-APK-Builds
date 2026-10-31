#!/usr/bin/env bash
# Pure Java smoke tests: no Android SDK, network, emulator or user data.
set -euo pipefail
SOURCE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$HOME/.cache"
TEMP="$(mktemp -d "$HOME/.cache/orivra416test.XXXXXX")"
trap 'rm -rf "$TEMP"' EXIT
javac --release 17 -d "$TEMP" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/ComposingMutation340.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/ComposingMutation413FastPathTest.java" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/IdleWordCandidate414.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/IdleWordCandidate414Test.java" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/FastKeySample415.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/FastKeySample415Test.java" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/IdlePredictionPaint410.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/FastIdlePrediction415Test.java" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/SentenceCapitalization.java" \
    "$SOURCE/app/src/main/java/com/chroomareborn/keyboard/SpaceShiftFastPath416.java" \
    "$SOURCE/app/src/testsupport/java/com/chroomareborn/keyboard/SpaceShiftFastPath416Test.java"
java -cp "$TEMP" com.chroomareborn.keyboard.ComposingMutation413FastPathTest
java -cp "$TEMP" com.chroomareborn.keyboard.IdleWordCandidate414Test
java -cp "$TEMP" com.chroomareborn.keyboard.FastKeySample415Test
java -cp "$TEMP" com.chroomareborn.keyboard.FastIdlePrediction415Test
java -cp "$TEMP" com.chroomareborn.keyboard.SpaceShiftFastPath416Test
