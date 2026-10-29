# Orivra Part 5 — Build 297 offline vocabulary research

Date: 2026-10-29. Research only. No APK released or merged, no real Samsung Android 17 acceptance.

## Baseline and provenance

- Forked `candidate/build295-major-prediction-integration-20261029` at `73b71da9eadcac125217cb8c7f8f145c58a2ab30`, not the failed-to-gain Build 296 decoder branch.
- Protected installed versions: Preview #178, Build 262, user-confirmed Safe Enter and prior Backspace/touch fixes; installed Candidate 293 unchanged. Never overwrite release signing or user data.
- Existing Build 294 single-arrow prediction strip, collapsed theme panel, typography, contrast, and typing animations are present in inherited source and untouched in this experiment.
- `RebornImeService.java`, Android manifest, app identity, Android build gradle, `SentenceRescue.java`, Spanish research sources, theme layouts, and input editor policy are unchanged versus 295.

## Implementation

- `LexiconFamilyExpander.java`: curated generic offline word family coverage. Adds verified full English words, common compounds and morphological forms. Candidate verbs get a third-person form only when base + past + -ing forms already occur in the boot lexicon. Source word snapshots prevent accidental repeated suffix chains. No private typing, telemetry, network or runtime user-text learning.
- `PredictionLite.java`: calls enrichment once before creating the immutable boot indexes. No change to decoder thresholds, ranking algorithm, multi-typo rescue, or user-accept behavior.
- `LexiconFamilyExpanderTest.java`: 12 assertions on existing word preservation, evidence-constrained forms, invalid form exclusion and idempotency.
- `SentenceFlowBlind297Audit.java`: 20 fresh ordinary synthetic English sentences (joined exact and three typo variants) plus 20 separate whole-word controls, total **100**. This QA fixture was frozen at `fc47a8da836d1d398d224db510869e3e805b68ce` **before** the enrichment code was committed.
- `SentenceFlowIndependentAudit.java`: original 80-case Build 296 audit retained unchanged, now considered a diagnostic/development set because its failures were inspected when selecting new vocabulary.
- `research297-offline-lexicon-ab.yml`: pins the untouched 295 source and compares both variants on exactly the same 147 + 340 + 80 + 100 = **667 synthetic cases**, both top-1 and top-3. Rejects protected category top-1 loss, any category top-3 loss, aggregate top-1 regression, excessive measured p95, and any correct-to-wrong individual audit result. It retains raw case evidence.
- A QA parser defect discovered in the first run incorrectly omitted one blank-suggestion row in per-case comparisons (reported 99 instead of 100). Fixed to retain empty last columns and assert all 100 rows exist.

## Measured CI research outcome

Final [corrected 667-case A/B workflow](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40956537552): PASS.

| Dataset | Candidate 295 top-1 | Build 297 top-1 | Top-3 |
|---|---:|---:|---|
| Existing 147-case English fixture | 117/147 | 117/147 | 132/147 unchanged |
| 340-case held-out fixture | 247/340 | 247/340 | 264/340 unchanged |
| Earlier 80-case diagnostic audit | 31/80 | 66/80 (+35) | improved; see raw CI artifact |
| New frozen 100-case prospective audit | 33/100 | 33/100 (+0) | unchanged |

Individual comparisons: on the 80-case development audit, 35 wrong-to-right, 0 right-to-wrong, and 2 wrong-to-different-wrong. On the new 100-case prospective audit: 0 wrong-to-right, 0 right-to-wrong, and 5 wrong-to-different-wrong. Thus no demonstrable generalization gain on the frozen new sample. Do NOT describe the +35 development gain as representative real-world accuracy.

Timing caution: the 340-case suite p95 moved from 34.54ms to 37.33ms on the Java GitHub runner. The older 80-case first-pass cold-start p95 also increased. These are not actual Samsung keypress latency or memory measurements; boot dictionary size and p95 require more work.

New native [18-Java-suite and 14-Android-35-emulator workflow](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40956223044) started. **Verify its final conclusion and emulator job before claiming Android pass.** No test APK is published.

## Unfinished quality work

1. The prospective 100-case failure profile exposes much broader missing vocabulary, unmodeled inflected forms, disjointed token boundaries, and unstable preference for short pseudo-splits. This patch alone is NOT a major release-quality prediction breakthrough.
2. Research a correctly attributed SCOWL/ESDB or other redistributable general vocabulary, preferably as a bounded secondary exact-word-only lexicon with no speculative fuzzy expansion. Review licence and total package size first. Do not use restricted Google N-gram lists as if unrestricted.
3. Measure vocabulary coverage and independent held-out top-one/top-three, exact unmodified words, false correction counts, startup cost, p95 and heap growth. Keep experimental new words out of sensitive fields.
4. Improve context/segmentation using evidence, not manual rules that simply memorize test sentences. Keep the shortspan Build 296 zero-gain rerank experiment separate.
5. Retest Android UI safe Enter, Backspace swipe, spacebar cursor, theme controls, and prediction strip; then compare compact and foldable screen dimensions in emulators. Physical Android 17 Samsung testing and other real devices must be reported separately.
6. No public Build 297 APK until a genuinely broader accuracy gain, acceptable performance, emulator qualification, separate signing/app ID, and deliberate release decision.

## Evidence / integrity notes
Repository commits and workflow results are distinct from a user's on-device installation. The benchmark text is synthetic and must never be represented as a representative population sample. The 100-case audit was frozen before the change, but the result now becomes development feedback and should not be reused as a fresh blind gate for a future candidate.
