# Orivra Candidate 300 — Google Keep predictions, 2026-10-29

## Why the user reported the bug
On Samsung Android 17 with Candidate 298, Google Keep displayed Orivra's `Private field • predictions off` instead of ordinary note predictions. This exact UI string is generated when `RebornImeService.sensitive` is true. Code inspection identified the source: `isSensitive(EditorInfo)` combined `TYPE_TEXT_FLAG_NO_SUGGESTIONS` with real password, identifier and nontext flags. The precise `EditorInfo` flags from the user's Keep field have not been captured, so the flag trigger is a **strong code-level hypothesis**, not a handset-confirmed diagnosis.

Official Android `TYPE_TEXT_FLAG_NO_SUGGESTIONS` docs say this is a dictionary suggestion UI hint rather than a credential classification. Google Play confirms Keep's application ID: `com.google.android.keep`.

## Change
Branch: `hotfix/candidate300-google-keep-predictions-20261029`, forked directly from qualified Candidate 298 at `2717ad8d01edc1a0854379c2a1e389bd64f57182`. Stable Preview #178, Build 262, Candidate 293 and Candidate 298 untouched.

- `EditorSuggestionPrivacyPolicy.java` is a pure-Java policy. Exact Keep package plus regular `TYPE_CLASS_TEXT` allows ephemeral offline suggestion chips even if `TYPE_TEXT_FLAG_NO_SUGGESTIONS` is set. All real password/visible-password/web-password/email/web-email/URI variation, nontext class and null editor protections remain.
- `RebornImeService.java`: `editorPrivacyDecision(EditorInfo)` maps real Android flags into the policy; tracks separate `editorAllowsPersonalLearning`; respects `EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING`. In Keep no-suggestions fields, explicit acceptance still applies corrections but does not record acceptance, read preferred corrections, or merge stored personal dictionary suggestions.
- `EditorSuggestionPrivacyPolicyTest.java`: 43 pure-Java assertions passed across exact Keep package, lookalike packages, normal notes, other-app no-suggestions, protected variations, nontext, and no learning.
- `KeepPredictionSecurityAndroidTest.java`: four Android instrumentation tests verify platform InputType/EditorInfo mappings for Keep notes and protected fields.
- Candidate 300 trial application ID `com.chroomareborn.keyboard.build300trial` with versionCode 300. The English predictor and Candidate 298 UI remain unchanged.

## Verification
- First full CI run: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40965231066 **success**. Java checks including 43 privacy assertions, 18/18 Android 35 emulator tests, Gradle build, package/signature/no-INTERNET verification. Actual APK was correctly packaged as versionCode 300, but initial GitHub release uploaded it with a misleading `Orivra-Candidate-298.apk` asset filename and inaccurate release-note version text.
- Packaging correction: workflow relabeled the APK and checksum to Candidate 300, corrected notes, and safely removed obsolete filenames. Second fully qualified CI run: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40966031942 **success**.
- Final public prerelease: https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/tag/candidate-300-20261029
- Exact final direct APK: https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/download/candidate-300-20261029/Orivra-Candidate-300.apk
- Final APK SHA256: `7abe7a854d5f5699065d71053540c6cfe856e6a0d21e31001e18e95022634ec2`. Release assets verified through GitHub API to contain only correctly labeled Candidate 300 APK and checksum.

## Awaiting physical device acceptance
This is a debug-signed experimental trial, NOT a production-stable release. Emulator qualification is not Samsung Android 17 or live Google Keep acceptance. Have user install alongside existing versions, activate **Orivra Candidate 300**, and test a disposable Google Keep note (title and body). Confirm actual prediction bar, explicit one-tap candidate, no crashes and passwords still private. If Keep still shows private-field message, request a screenshot of the keyboard and optionally diagnose `EditorInfo` class/variation/flags *without* collecting note text. Never weaken true password/editor privacy for compatibility.

## Unrelated research
Independent Build 299 language work remains isolated on `research/build299-prediction-failure-attribution-20261029`. Frozen 100-case diagnostic attributed 60 cases to absent lexical coverage, and official ESDB/SCOWL size-60 source study found 88,662 ASCII-filtered words that would cover the missing 20 standalone word controls. **These wordpack tests have not been merged into Candidate 300.**

## Samsung acceptance update (same day)
The user installed and tested the Keep hotfix on their physical phone, then explicitly confirmed **"Fixed! Thanks!!"** after reporting that Google Keep had been hiding Orivra predictions. Treat this as **Google Keep compatibility accepted by the user on their Samsung S26 Ultra Android 17**, not as acceptance of every editor, language accuracy, or long-term stability. Do not reopen this particular issue without new contrary evidence. Candidate 300 remains the protected phone-tested baseline for subsequent research.
