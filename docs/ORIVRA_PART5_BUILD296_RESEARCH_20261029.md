# Orivra Part 5 — Verified recovery and isolated Build 296 research

Date: 2026-10-29. Source-only checkpoint; NOT a release, APK, merge, or Samsung-device test.

## Source of truth, preserved milestones

- Private historical development: `ItsJavs2026/Chrooma-Reborn` main `c8b95a964805006629f7793dfb07caa7875d0baa`. Its October 28 candidate and research branches exist through Build 286.
- Public experimental source/builds: `ItsJavs2026/Orivra-APK-Builds`; Build 294 UI and newer Build 295 prediction branches are present. Do not restart from an older Build 292 handoff.
- Protected install and behavior baselines: Preview #178, Build 262; user-confirmed Build 288 Backspace crash repair, safe Enter behavior; keep installed Candidate 293 unchanged.
- Candidate 294 UI source-only: `candidate/build294-suggestion-visual-motion-20261029` at `f0f16145458894a5a677e2c015c0dedd1152a9b9`. One unified right arrow, theme only in collapsible settings, suggestion text/contrast and ~55/95 ms animations, no APK. [UI qualification](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40936640145) succeeded.
- Qualified integrated Candidate 295 research branch: `candidate/build295-major-prediction-integration-20261029` at `73b71da9eadcac125217cb8c7f8f145c58a2ab30`. English predictor remains offline and active; Spanish is RESEARCH ONLY behind explicit router and is not wired to native IME; no Candidate 295 APK.
- Verified [English 487-case quality workflow](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945887995): synthetic 147-case first set 117/147 top-1, 132/147 top-3; 340 independent holdout 247/340 top-1, 264/340 top-3.
- Verified [Spanish 180-case workflow](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945955025): 122/180 top-1, 139/180 top-3 (research only).
- Verified [Android 35 QA](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945787279): 18 Java suites and 14 Android instrumented tests. Verified [installed emulator keyboard interaction](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945836275). Neither equals actual Samsung Android 17 acceptance.

## Part 5 implementation

Branch: `research/build296-context-gated-shortspan-20261029`.
Forked directly from integrated Candidate 295. No edits to RebornImeService, app ID, APK publication, protected installs, or source branch.
- `SentenceRescue.java`: experimental bounded overflow for previously pruned four- or five-letter candidates with exactly two interior errors and matching edges, available only after a lexically supported preceding word. This is candidate generation, not posthoc reranking. No automatic text edits.
- `SentenceFlowIndependentAudit.java`: 16 generic independent synthetic sentences x four typo profiles, plus 16 complete correctly spelled dictionary words for false-correction safety, with per-input raw comparison records. These cases are never added to the predictor dictionary or ranking rules.
- `research296-context-gated-shortspan.yml`: pins untouched Candidate 295 at `73b71da9...`, compiles two offline Java predictors, runs the same 487 old+holdout examples and independent audit, compares protected top-one/top-three categories, and checks per-case wins/losses. Saves raw outputs as a CI artifact.
- [First A/B run](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40954312865) PASS, zero aggregate gains or regressions across 487+64 cases. This experiment is **not** an accuracy improvement and should not be silently merged.
- Follow-up workflow with complete-word and per-case loss checks is [here](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40954723033); inspect its final conclusion before citing as pass.

## Unresolved and next major qualification

1. Current shortspan overflow hasn't improved synthetic accuracy; treat as unselected research, do not advertise as a winning feature.
2. Prioritize fresh, license-cleared and more diverse offline vocabulary and language transition coverage; do not bake benchmark phrases into language data. Investigate why independent joined exact phrases were only 5/16 top-one and double interior short-word cases 2/16 (initial 64-case audit). Diagnose the original generator rather than endlessly tuning the bounded reranker.
3. Require blind holdouts, top1/top3, per-case wins and losses, exact-word false corrections, input safety, p95 timing, memory/CPU and Android visual/IME regression tests before merging.
4. Preserve one-arrow strip, theme panel, safe Enter, Backspace swipe/hold, spacebar cursor, capitalization, one-tap explicit acceptance, and privacy. Do not rush a new APK.
5. A later versioned APK must have a separate package identity, signer/hash checks, emulator matrix including different widths/orientations, then user-confirmed Samsung S26 Ultra Android 17 and optional second physical device testing.
6. The older `ORIVRA_MASTER_PROJECT_BACKUP_2026-10-29.md` in Library describes Build 292 and before and must not supersede newer 294/295 live repo evidence.

All numbers in this file are CI fixture counts. They are not representative population accuracy, real Samsung latency, field battery impact, or long-term personalization results.
