# Orivra Part 5 — Build 306 Learning V2.1 conflict-aware ranking

Date 2026-10-29. Research-only source branch `research/build306-learning-v2-ranking-20261029`. **No new APK shipped or installed** from this branch.

## Actual accepted baselines
- **Candidate 304** remains the newest phone-confirmed Samsung Galaxy S26 Ultra Android 17 trial. User explicitly said the left-aligned idle next-word suggestions were now “much better” after installing Candidate 304. Preserve the Google Keep predictions fix from Candidate 300, Safe Enter, Backspace, and centered idle UI.
- **Candidate 305** Learning V2 + apostrophe contractions prerelease is already published and CI-qualified: https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/tag/candidate-305-20261029 . Verified Candidate 305 source SHA `ab6a8d7e675533271981dca6fe0d146419ffe338`, CI success 40983964555 with 30/30 Android emulator tests. **No Samsung 305 installation/acceptance yet.**
- Learning V2 preserves old correction preference format inside the same application package: SharedPreferences `orivra_personal_learning_v1`, keys `accepted:<typed>:<chosen>` with integer counts. Requires 2 explicit acceptance taps before suggesting a learned correction. Learning remains default OFF; protected fields, including Google Keep with NO_SUGGESTIONS, do not learn.
- Different experimental packages have isolated Android private storage. Thus Candidate 305 does **not automatically import** historic user corrections from Candidate 304; do not claim V1 data magically migrated across package identities.

## New Build 306 source
The explicit accepted correction engine had an iteration-order-dependent tie: two outputs with equal >=2 counts could become whichever happened to be read first from SharedPreferences. This risked arbitrary corrections despite conflicting user history. New changes are intentionally limited to learning ranking, not the typing, editor, spelling or privacy engines.

- `LearningV2Policy.preferred(typed,Map<accepted,count>)`: normalize straight/curly apostrophes, validate bounded counts and outputs, deduplicate legacy case/quote variants with **max** rather than sum, require an unambiguous strict winner with >=2 acceptances. No prediction or auto-commit on a tie.
- `LearningV2Policy.displayCorrection`: preserve ALL CAPS, initial letter capitalized, and conventional initial `I'm` capitalization without changing the actual stored normalized preference key.
- `PersonalLearning.preferredCorrection`: retains V1 private preference schema, selects deterministic correction via the pure policy and never records full sentences or raw typing history. Preserves existing learning-enable behavior.
- `LearningV21ConflictTest`: 266 synthetic assertions on ambiguity, casing, curly apostrophes, invalid emails/PII, disabled/malformed outcomes, and 120 shuffled insertion-order cases with both ties and clear winners.
- `LearningV21RankingAndroidTest`: two independent Android 35 tests on temporary synthetic SharedPreferences data, restoring any previous values, verifying competing V1 correction counts, explicit new acceptance and case preservation; duplicate curly/straight keys must not falsely count as extra user acceptances.
- `.github/workflows/research306-learning-v2-conflict-qa.yml`: pins and byte-compares Candidate 305 protected IME, Google Keep editor privacy, main settings, languages, package identity and manifest. Runs original contraction and 266 new Java assertions, then full Android 35 emulator tests, including two new tests. No INTERNET permission and no release writes.

## Quality status
- https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40988582233
- Java regression job **passed**.
- Android 35 emulator qualification **SUCCESS**: [GitHub Actions run 40988582233](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40988582233) confirmed **32 tests started and 32 finished**, Gradle BUILD SUCCESSFUL. The Java job passed **89 original contraction checks** and **266 new Learning V2.1 conflict/case assertions**. No user-facing APK was published; these remain Android 35 emulator and source results, not Samsung field acceptance.

## Next improvement
Personal learning and explicitly saved words should be portable across the separately installed trial APKs, but Android app sandboxing forbids silent cross-package import. Design an opt-in, reviewable **Export learning / Import learning** flow using Android's Storage Access Framework. Export only manually saved words and deliberately accepted *word-pair* corrections, never editor history/passwords. Require file-size/record-count validation, confirm before import, keep learning OFF unless user separately enables it, protect existing V1 counts and avoid repeated-import inflation. Warn that backup files are plain text and user-controlled. Build this on a separate branch after confirming 306 regression tests. Do not replace Candidate 304 or auto-import private files.
