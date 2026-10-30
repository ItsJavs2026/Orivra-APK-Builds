# Orivra Candidate 319 — isolated phone trial packaging

Based on exact Research 319 commit `baf8dac52322460484460f49dae20fd152803249`.
Only changed Android package ID, version code/name, visible candidate
branding, package-scoped latency metrics and test package expectation.
No source changes to English predictions, contextual learning, key haptics,
Google Search, Backspace, spacebar cursor or privacy policy.

GitHub Actions must pass 40+ Android emulator tests, verify an APK with
applicationId com.chroomareborn.keyboard.build319trial and versionCode 319,
verify debug signing, no INTERNET permission, attributed ESDB asset and SHA
before publication of any trial.

Separate package means it will NOT replace Candidate 310 and will NOT
inherit its app-private learning until user performs a manual export/import.
Contextual pairs are not yet included in the legacy backup.
