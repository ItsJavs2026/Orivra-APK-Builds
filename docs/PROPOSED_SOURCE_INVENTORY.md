# Build 286 source-publication review — owner sign-off required

## Snapshot and exposure
- Private source: `ItsJavs2026/Chrooma-Reborn`, exact commit `df9f4aa2b17806fba7a8feeea6d9b055f913f0b6`.
- This is a **new public repo with no imported history**.
- Publishing the following files lets anyone download, fork and inspect this *version of the keyboard source*. Keeping the other repository private does **not** protect any source copied here.
- The Build 286 Android version has **not** passed the full Gradle CI build because its previous runner did not start; a public trial compile is still required.

## Proposed allowlist (not yet copied)

**36 production Java source files**, under `app/src/main/java/com/chroomareborn/keyboard/`:

- `AppPalette.java`
- `AppPaletteText.java`
- `CandidateLatencyMetrics.java`
- `CandidatePreviewReport.java`
- `CursorContextCorrectionRanker.java`
- `CursorEditSnapshotGuard.java`
- `CursorFallbackBoundaryPolicy.java`
- `CursorMovementPolicy.java`
- `CursorReleaseSuggestionPolicy.java`
- `CursorSwipeBatch.java`
- `CursorWordCorrectionPolicy.java`
- `KeyboardLayoutSpec.java`
- `KeyboardSizing.java`
- `MainActivity.java`
- `NextWordCandidatePolicy.java`
- `NextWordControlLabels.java`
- `NextWordPhraseModel.java`
- `NextWordSeedModel.java`
- `NextWordTrigramConfidenceModel.java`
- `NextWordTrigramModel.java`
- `PersonalDictionary.java`
- `PersonalDictionaryPolicy.java`
- `PersonalLearning.java`
- `PersonalNextWordModel.java`
- `PredictionDispatcher.java`
- `PredictionLite.java`
- `PunctuationSpacing.java`
- `RebornImeService.java`
- `SentenceCapitalization.java`
- `SentenceRescue.java`
- `SentenceSuggestionCommit.java`
- `SentenceTapOutcome.java`
- `SpacebarCursorGesture.java`
- `SuggestionDisplayPolicy.java`
- `SymSpellCandidateBridge.java`
- `TouchIntelligence.java`

**Required resources and attribution**:
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/method.xml`
- `app/src/main/assets/lm/swiftfloris_en_bigrams.tsv` (approximately 12 KB) with upstream Apache 2.0 attribution
- `third_party/swiftfloris/LICENSE`, `NOTICE`, `README.md`
- Optional synthetic tests for SpacebarCursorGesture and KeyboardLayoutSpec

**Already in public builder, locally authored (not copied from private source)**:
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`
- `.github/workflows/build-public-trial.yml` (manual-only, no private checkout)
- Publication documents and `.gitignore`

## Excluded entirely
- `research/**`, `evaluation/**`, `scripts/**`, internal `docs/**`, internal Actions workflows, `app/src/debug/**`
- Git metadata/commit history, screenshots, screen recordings, user/personal typing
- SymSpell frequency corpus, EWT/dialogue research data and other unreviewed corpora
- Production keystores, passwords, signing secrets

## Current review findings
- All 36 production Java files were inspected by automated scans for common private-key formats, GitHub token patterns, cloud access IDs, literal credential assignments, personal email strings, user-specific account references and network-library patterns. **No alerts in those checks**; the tests are indicative, not a comprehensive security or copyright audit.
- Android manifest explicitly does not request INTERNET permission and sets `allowBackup=false`. No production signing file is needed for public debug compilation.
- The shipping language seed is attributed to SwiftFloris, including a LICENSE and NOTICE; Apache 2.0 permission and attribution must be maintained.
- The base `PredictionLite` is a compact offline bootstrap engine; experimental datasets remain excluded.
- The public debug APK will use `com.chroomareborn.keyboard.publictrial`, not `.candidate` or `.stablepreview`.
- Public CI debug signing keys can rotate; first build must be treated as a standalone testing installation only.

## Human decision
**Not approved for publication yet.** The owner should confirm they are willing to make the enumerated 36 production Java source files and the required asset/resources public. If approved, copy them as *fresh files*, never a fork or history mirror, then run public CI. No APK is available from this public repository yet.
