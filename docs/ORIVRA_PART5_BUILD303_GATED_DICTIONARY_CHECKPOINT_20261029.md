# Orivra Part 5 — Build 303 opt-in offline English wordpack checkpoint

Date 2026-10-29. **Source-only research; no Candidate 303 APK has been published or installed yet.** Never overwrite physical Samsung-approved Candidate 300 (working Google Keep predictions), stable Preview #178 or Build 262. Distinguish Java unit tests, Android emulator qualification and actual on-handset acceptance.

## Provenance and implemented change

- Isolated branch `research/build303-gated-dictionary-activation-20261029` from source-qualified `research/build302-compressed-offline-pack-20261029`; retains original Candidate 300 application ID in research source, and the Google Keep privacy rule.
- Build 302's verified offline ESDB pack: 78,942 lowercase words generated from pinned ESDB/SCOWL v2 revision `1e5b7d3a72f47a71da5d28686c1dd4b397178485`. Canonical original wordlist SHA-256 `a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600`; deterministic gzip payload SHA-256 `c7ae38d8c2d3e83652ecacc947067e823ab6daec9993baaf7e2dc24501794098`, 280 KiB with neutral `.esdbpack` extension. Full ESDB licensing notice included.
- `OfflineDictionaryActivationPolicy.java`: pure-Java privacy gate. Default disabled. Separate opt-in `keyboard_feedback/orivra_offline_exact_dictionary_v1`. Never activate in sensitive editor fields. Reject stale callbacks and missing/corrupt packs.
- `RebornImeService.java`: checks the opt-in preference when a new editor starts; begins `OfflineLanguagePackWorker` load on a low-priority worker thread only for non-sensitive text. Leaves baseline suggestions active until verified pack is ready; then atomically updates the existing research-only exact-word dictionary hook. Clears the pack when disabled, on private fields and on destruction. Does not read or store note text in the loader; does not send or persist user text.
- `MainActivity.java`: accessible opt-in experimental vocabulary switch, clear “reopen a text field to apply” messaging and warning not to enable on production-critical typing. Current research-only app identity unchanged.
- `OfflineDictionaryActivationPolicyTest.java`: **41** pure-Java assertions passed covering opt-in, protected fields, duplicate loading, stale callbacks and malformed assets. Existing **43** Google Keep/privacy assertions, **11** segmentation assertions and **seven** compressed asset-integrity assertions passed.
- `OfflineDictionaryActivationAndroidTest.java`: two additional Android 35 emulator tests for default-off preferences and actual `EditorInfo` flags, including Google Keep normal text and password/email exclusions.
- `.github/workflows/research303-gated-dictionary-native.yml`: pins official ESDB, rebuilds and preflights real APK asset, runs existing keyboard/security tests plus the two new Android tests, and deliberately does **NOT** upload or release any APK.

## Qualification gate

[Run 40974663679](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40974663679): Java job **success**; Android 35 emulator test job in progress at checkpoint creation. Verify final job conclusion, number of completed tests and asset integrity before upgrading status. Android 35 is not the user's Samsung Android 17 device. The Java language-engine 667-case A/B benchmark remains from previously qualified 301 work; the core predictor/segmentation code is unchanged in 303.

## Next steps

1. Confirm emulator qualification and archive pass/failure evidence.
2. Consider separate **Candidate 303** package identity, independent signing, matched source hash and bundled ESDB license only if the Android quality gates pass. Never alter installed Candidate 300. Default OFF after installation.
3. On actual Samsung S26 Ultra Android 17, test baseline with switch OFF first, then opt-in with ON, reopening Keep and Samsung Notes text fields. Compare suggestions for joined text, correct words and known typos. Record actual p95/load and privacy effects separately; protect credentials and email fields.
4. Only after real Samsung acceptance, consider any stable release. Spanish still research-only, no glide typing claims.
