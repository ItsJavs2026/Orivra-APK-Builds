# Candidate 339 source and byte-identical release

Qualified source: 3bd2e877dc66a40efa172f7fa9e448ca476a2147
Full APK/emulator run: 41075739289 — 66 Android instrumentation tests passed
Signed APK artifact: Orivra-Candidate-339-Android35-Passed

Publisher downloads the original tested APK, checks SHA256, Android
Signature Scheme v2, version 339, application ID
com.chroomareborn.keyboard.build339trial, offline dictionary attribution,
and absence of INTERNET permission. Never rebuild during release.

The user's five Candidate 333 screenshots show a composing word being
backspaced to 'a', an empty-predictions message, and an extra press.
Research 338 conclusively identifies the finishComposingText-only path,
reproduces it in a BaseInputConnection test, and replaces it with
verified composing-text deletion. Real Samsung testing still needed.

The trial keeps V1/V2/V3 backups. Export V3 from Candidate 333 first,
install 339 separately, import V3, and test only in a draft message.
