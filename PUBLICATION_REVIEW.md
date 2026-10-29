# Build 286 source publication review

## Included
- Exactly 36 production Java files from the uploaded `candidate/gboard-reference-layout-build286-20261028` branch archive.
- 13 selected synthetic Java test files (not private benchmark/research datasets).
- Three Android manifest/resource files.
- One small offline English bigram TSV from SwiftFloris, plus its bundled Apache 2.0 LICENSE, NOTICE and provenance README. Attribution is preserved. Third-party NOTICE describes other upstream components **not bundled here**; its inclusion does not mean those components were copied.
- Clean public-only Gradle files and one manual-only GitHub Actions workflow.

## Excluded
Private Git history, all private Actions workflows, scripts, research/evaluation corpora, debug QA Activity, screenshots, recordings, user typing, keystores, signing credentials, local.properties, and internal documents.

## Limitations
- Static checks for known private-key formats, GitHub token formats, AWS access IDs, and selected user identifiers are not a comprehensive security audit.
- Bundled license notices are based on source repository attribution and are not a legal opinion or an independent verification of every upstream provenance claim.
- Source compilation has not yet run; successful Gradle CI does not establish Samsung Android 17 correctness.
- The public trial uses an isolated package ID. Debug signing may rotate between CI runs.
- This ZIP is intended for owner review and upload, not for direct APK installation.
