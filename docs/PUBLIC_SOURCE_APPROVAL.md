# Reviewed source snapshot — owner approval needed

**HOLD:** No Orivra application source has been copied here yet.

## Intended source origin
- Private source of truth: `ItsJavs2026/Chrooma-Reborn`
- Proposed Candidate snapshot: Build 286 commit `df9f4aa2b17806fba7a8feeea6d9b055f913f0b6`
- Public repository is a new repository, **not** a fork or history mirror.

## Proposed files for *review*, not yet published
- All build-required `app/src/main/java/com/chroomareborn/keyboard/*.java` (these **will become public** if approved)
- `app/src/main/AndroidManifest.xml`, `app/src/main/res/**`
- Only the shipping `app/src/main/assets/lm/swiftfloris_en_bigrams.tsv`, retaining `third_party/swiftfloris/{LICENSE,NOTICE,README.md}`
- Selected synthetic unit tests, if required, but **no** private research/history

## Explicitly excluded
- Entire `research/**`, `evaluation/**`, `scripts/**`, internal `docs/**`, and other private workflows
- Stable signing scripts, keys, tokens, secrets, key stores and historic Git commits
- SymSpell frequency data, Norvig / Google ngram data, EWT / dialogue research datasets, screenshots, screen recordings, user keystrokes
- The debug-only editor QA activity unless separately approved

## Approval and security gates
1. Review each proposed file, permission, attribution and data license before publishing.
2. Manual build workflow; no cross-repository private checkout, `pull_request_target`, secrets or network access to personal data.
3. Output package `com.chroomareborn.keyboard.publictrial`, never `.candidate` or `.stablepreview`.
4. Validate Java regressions, Android debug APK, package ID and SHA-256 before distributing.
5. Debug signing changes across GitHub-hosted runs. Develop a durable trial signer without committing private keys; do not uninstall Preview #178 or the existing Candidate.

*This is a source publication checklist, not proof that Build 286 compiles or that an APK is ready.*
