# Orivra Part 5 — Build 307 portable learning experiment

Date 2026-10-29. **Research-only source; no new user APK or phone installation.** Preserve Samsung-accepted Candidate 304 (centered idle prediction strip) and Candidate 300 Google Keep rollback. Candidate 305 already published, 30/30 Android emulator tests, but user has **not yet reported installing or approving it**:
https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/tag/candidate-305-20261029

## Problem and strict privacy boundary
The V1→V2 learning preference schema stays compatible **within one Android application ID** (`orivra_personal_learning_v1`, `accepted:<typed>:<accepted>`), but isolated test builds 304 and 305 have separate Android private app storage. V1 history in one package cannot silently appear in another; do not say it was migrated. A user-driven, reviewable export/import is the intended workflow for **future builds that both implement it**. It cannot retroactively expose Candidate 304's stored preferences without explicit access and an approved export mechanism.

The feature MUST NOT automatically read other apps' files, copy raw typed text, keep full sentence context, export passwords, access the network or silently enable learning. Backups are plaintext and may contain the user's personally saved names and accepted correction pairs; warn the user and require explicit system document picker selection and confirmation before import.

## Source additions
- `LearningTransferCodec.java`: pure-Java deterministic versioned UTF-8/LF file, SHA256 integrity check, whole-file validation before mutation, max 100KB/250 manually saved words/300 explicit accepted word-correction pairs, max accepted count 1000. Reject duplicate straight/curly spellings, emails, URL fragments, multiple words, digits, malformed records, tampered files, and unknown format versions. Preserve manual display casing and curly apostrophes.
- `LearningTransferCodecTest.java`: **46 synthetic checks passed**, including 250-word/300-pair maximum and round trips; CI https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40989041156 .
- `LearningTransferStore.java`: Android package-private storage adapter. Reads only `PersonalDictionary.savedWords` plus `accepted:` keys from the V1-compatible preference namespace. Import performs whole-bundle preflight, applies missing words and stronger counts with **max, not addition**, preserving previous display spellings and Learning ON/OFF setting. Idempotent reimport; cap checks before mutation.
- `LearningTransferDocumentIO.java`: Android Storage Access Framework `ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT`, with user-selected URI, no storage permission, bounded 100KB read and strict UTF-8 decoder. Runs on a separate Activity executor, not the live IME or suggestion worker.
- `MainActivity.java`: two explicit controls: **Export my saved words + learned corrections** and **Import previous Orivra learning backup**. Export shows plaintext-sensitive-data warning and requires user selection; import parses in a worker, previews word/pair counts, requires approval before writing, never enables learning automatically. No clipboard, saved document URI, raw text logging, or automatic import.
- `LearningTransferStoreAndroidTest.java`: three synthetic tests for merge and idempotent import without inflated counts, preserving disabled state, rejecting malformed documents without writes, and verifying system document picker intent scope. All pre-existing preference values restored in `finally`.

## Tests and status
- Build 306 conflict-aware correction policy: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40988582233 **SUCCESS**, 266 Learning V2.1 checks and **32/32 Android 35 emulator tests**. Candidate 305 remains unaltered.
- Build 307 pure-Java codec CI: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40989041156 **SUCCESS**, 89 original contraction checks, 266 conflict checks, and 46 portability checks.
- Build 307 full source and Android QA: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40989781829 **SUCCESS**. Verified Java original contraction **89** assertions, conflict/case **266** assertions and portable codec **46** assertions. Android 35 Pixel 6 emulator **35 tests started, 35 tests finished, BUILD SUCCESSFUL in 2m59s**. Three new Android backup tests exercised idempotent merge, retained Learning OFF state and system document picker intent boundaries using synthetic values, restoring prior local preferences. Protected prediction, editor, language, manifest and package sources are byte-identical to qualified Candidate 305 SHA `ab6a8d7e675533271981dca6fe0d146419ffe338`. No GitHub release/write privileges, no public APK created and no Samsung physical device test yet.
- A SHA-256 checksum detects corruption, **not maliciously forged backups**. Users must only import files they trust. Text backups are not encrypted. The Android file picker may offer user-controlled local or cloud document providers.

## Next
1. Inspect Android 35 emulator result; correct compilation/QA failures and rerun before any release.
2. After green emulator, review and harden two-file import transaction and lifecycle cancellation, potentially add a small preview of exact saved words without exposing text to logs. Never auto-back up.
3. If a user-facing isolated trial is desired, ship with a distinct application ID and QA; test importing/exporting only synthetic, nonsensitive words first. Treat manual Samsung acceptance as required.
4. Continue separate English/Spanish research and safer deliberate learning. English capitalization and curly apostrophes are already supported in Candidate 305 research; Spanish mixed-language routing remains research-only.
