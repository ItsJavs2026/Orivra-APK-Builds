# Orivra Part 5 — Build 302 compressed offline language-pack checkpoint

2026-10-29, source-only **research**, NOT a new user APK. Continue from the physical Samsung-confirmed **Candidate 300**, which resolved the user's Google Keep prediction suppression. Avoid overwriting stable Preview #178, Build 262, Candidate 293, Candidate 298, or Candidate 300. Keep Google Keep privacy rules intact.

## Baseline and objectives
- Source branch `research/build302-compressed-offline-pack-20261029`, forked from qualified `research/build301-esdb-exact-only-ab-20261029`.
- Build 301 passed exact-only synthetic English A/B research against Candidate 300: 667 fixed synthetic cases, 14 additional correct top-1 outcomes on an older 100-case test, zero previously correct cases lost in individually checked sets. This is NOT a real-world accuracy estimate.
- Build 301 language pack was runtime-injectable in a JVM test but NOT packaged in an APK or used by the live IME.
- Build 302 focuses on **reproducible asset generation, integrity checks, safe background load and emulator performance**, with no user-facing release.

## New source and safeguards
- `CompressedLanguagePack.java`: gzip-decompress with a strict 1,350,000 byte output cap, check full SHA-256 on canonical uncompressed official wordlist, require exactly 78,942 alphabetic words after existing bounded filter, measure load time; corrupt/missing/oversized optional asset falls back to no secondary pack. No network, clipboard, editor or user-text access.
- `OfflineLanguagePackWorker.java`: Android `AssetManager` load on low-priority worker thread, main-thread completion notification, single-start and cancellation-safe lifecycle. Deliberately **not connected to `RebornImeService`** until memory and keyboard acceptance checks.
- `CompressedLanguagePackTest.java`: seven pure-Java invalid / tampered / oversized / missing source assertions. No actual typed data.
- `OfflineLanguagePackAndroidTest.java`: three research Android tests that require the generated asset actually be present in the test APK, verify source SHA-256 and word count, asynchronous load with main-thread callback, and baseline versus experimental warm prediction p95 on a set of fixed synthetic strings. Includes exact-word protection and preservation of already-correct readings; only logs aggregated metrics, never editor contents. Test estimates are **Android 35 emulator**, NOT Samsung Android 17 results.
- `app/src/main/assets/dictionaries/ESDB_COPYRIGHT.txt`: full original ESDB copyright and redistribution terms with pinned upstream identity.
- `scripts/build-esdb-wordpack.sh`: offline reproducible local asset generator requiring exact upstream commit, canonical source SHA-256 and byte count. No network calls.
- `.github/workflows/research302-pack-native-benchmark.yml`: pins upstream `en-wl/wordlist` SHA `1e5b7d3a72f47a71da5d28686c1dd4b397178485`, rebuilds US size-60 A1 list with specialty categories excluded, source SHA `a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600`, then creates `gzip -n -9` asset at `dictionaries/esdb-en-US-60.esdbpack` in GitHub Actions. Workflow runs pure Java guards and native Android 35 emulator test suite, verifies built APK includes both gzip and license and has no INTERNET permission, and **does not upload or publish APKs**.
- `RebornImeService.java`, `EditorSuggestionPrivacyPolicy.java`, application ID and Android manifest remain unchanged and are explicitly checked against Candidate 300 in CI.

## Key safety distinction
Only the CI Android debug test build contains the experimental gzip asset. A checkout of this research branch without running `scripts/build-esdb-wordpack.sh` intentionally lacks the asset and must fall back to Candidate 300 behavior. No physical handset APK exists or should be claimed. The source code includes an optional loader but **does not activate it in the live keyboard**.

## CI checkpoints
- Initial [run 40972469666](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40972469666) failed because a Java-only job incorrectly required the still-to-be-generated gzip asset. The workflow condition was corrected; this failure did not exercise Android typing or predictors.
- Corrected [run 40972532494](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40972532494) Java regression job **passed**. Its final emulator result and measured size/latency must be checked before claiming Android qualification.

## Next steps
1. Verify final emulator job, actual packed gzip size, source integrity, Android load time, warm p95, and any regressions. On failure, correct and rerun; keep stable Candidate 300 unchanged.
2. When verified, evaluate if activation can occur safely on a low-priority worker without blocking the first key event. Add a hard feature flag and privacy gates; never auto-enable an experimental dataset in user builds.
3. Independently test a second set of out-of-distribution phrases, personal names, URLs, non-ASCII and ambiguous compounds. Do not tune the engine to remembered benchmark answers.
4. After asset attribution, memory checks, emulator regression and safe feature gating, a separately installable Candidate 303 can be considered for user testing. This is NOT yet a verified or released APK.

## Android asset filename diagnosis and fix
The first Android run (40972532494) found all three new asset tests failed with FileNotFoundException: `dictionaries/esdb-en-US-60.txt.gz`. The second strict APK preflight (40973130817) directly verified Android AAPT had repackaged the asset under `assets/dictionaries/esdb-en-US-60.txt`, dropping the `.gz` suffix. The payload was a valid 280 KiB gzip archive (SHA-256 `c7ae38d8c2d3e83652ecacc947067e823ab6daec9993baaf7e2dc24501794098`). Corrective change: name the *still gzip-compressed* asset `esdb-en-US-60.esdbpack` to prevent AAPT special handling. Keep source SHA checks, gzip magic and decompression intact; rerun full Android tests before claiming success.
