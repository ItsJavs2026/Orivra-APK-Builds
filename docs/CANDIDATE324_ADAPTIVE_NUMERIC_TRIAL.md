# Candidate 324 — combined Android trial artifact (not yet phone verified)

Source pinned from Research 324: combines opt-in word-use adaptation and
contextual suggestions (Research 321), the isolated research-only V2
backup codec (322), and banking numeric pad (323). The existing V1
export/import UI is unchanged; V2 research codec is not exposed.

Separate applicationId com.chroomareborn.keyboard.build324trial,
versionCode 324. Candidate 319/Build 310 are not overwritten.
The new APK still needs manual dictionary backup/import because separate
packages cannot read each other's private data.

Actions workflow verifies the exact qualified source and only metadata
edits, Java regressions, full Android emulator tests, debug signature,
assets, SHA256 and absence of INTERNET permission before an APK artifact
is retained for testing. It does not auto-install on the user's device.
