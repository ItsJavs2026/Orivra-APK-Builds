# Orivra Candidate 295 — Major Prediction Development Checkpoint
**Date:** 2026-10-29  
**Integrated development branch:** `candidate/build295-major-prediction-integration-20261029`  
**Status:** Source integration and A/B regression qualification; **NOT an APK release**.

## Stability and ownership protections

Candidate 295 is descended from the verified Candidate 294 UI baseline. Its native `RebornImeService.java`, Safe Enter guards, Backspace swipe/editor transactions, the one-arrow settings layout, theme controls, and typing animations have **not been altered by this prediction development branch**. The installed Candidate 293 package and stable Preview #178 / Builds 262, 288, 291 and 292 remain untouched.

No new network permissions or cloud language-model connections have been added. The English production `PredictionDispatcher` still uses the existing offline `PredictionLite`. The new Spanish router is **experimental and explicitly language-selected; it is not wired into the active IME yet**.

## Selected English improvements

1. **Lexicon consistency:** All plain alphabetic first-letter completion words are registered in the exact-match boot dictionary before search indexes are constructed. This protects an already correctly spelled completion (e.g. `question`) from being treated as an unknown connected phrase.
2. **Short-span typo recovery:** The `SentenceRescue` decoder now considers a 4-letter span containing two edits **only** with matching first and last letters and sufficient original bounded edit evidence. Ordinary exact words retain their early-return protection.
3. **Evidence-gated long sentence rescue:** Candidate promotion on long joined text uses a cautious, slightly lower transition-coherence margin only with bounded character-edit support. It must not replace a letter-exact joined prediction with a proposed sentence requiring extra character edits.
4. **Top-three evidence ranker:** The bounded pure Java `SentenceFlowEvidenceRanker` cannot invent candidates; it only considers supported existing top-three alternatives. Its isolated test passed, but **it did not produce standalone benchmark gains**. It remains behind the original English predictor's conservative paths.
5. **Independent quality framework:** Pinned old predictor and new predictor run on identical fixed synthetic inputs, with top-one/top-three, timing diagnostics, category-specific no-regression checks, and raw evidence retained in Actions artifacts.

### English synthetic A/B results (same runner, frozen starter baseline)

| Category | Cases | Baseline top 1 | Selected top 1 | Baseline top 3 | Selected top 3 |
|---|---:|---:|---:|---:|---:|
| Recognized correct words | 16 | 16 | 16 | 16 | 16 |
| One-typo words | 30 | 29 | 29 | 29 | 29 |
| Joined exact, no typo | 30 | 24 | **27** | 30 | 30 |
| Joined, one typo | 30 | 22 | 22 | 28 | 28 |
| Joined, two typos | 30 | 13 | **14** | 19 | **20** |
| Previously difficult examples | 11 | 9 | 9 | 9 | 9 |
| **All fixed tests** | **147** | **113** | **117** | **131** | **132** |

Additional held-out 340 examples: top-one **246 → 247**; top-three **263 → 264**. The extra data are independent synthetic sentences, 60 sentence targets with zero to three typo variations plus 50 standalone word targets and their mistakes. **This is not population-level typing accuracy or an Android latency benchmark.**

**Evidence:** [Candidate 295 pinned English A/B run](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945887995), including a separate 30k deterministic ranking test, 72 first-letter starter-word checks (178 assertions), and existing 78 smoke assertions.

## Spanish offline prediction foundation — research only

The independent `SpanishPredictionLite` contains generic curated Spanish vocabulary, phrase transitions, accent-aware Unicode normalization, bounded single-word corrections, and connected-word beam-search alternatives. `BilingualPredictionRouter` requires explicit `ENGLISH` or `SPANISH` mode; the default native keyboard is **unchanged English**.

- Safety: no memory of typed strings, no editor access, no network, no implicit cross-language substitution.
- The Spanish module recognizes correctly spelled complete words separately from joined inputs, and NFC/NFD normalized inputs have matching output.
- Conservative accent **display-only** evidence may select `cómo` following a context that explicitly supports it. Decoder paths/ranks are not changed by the accent display rule.
- More language coverage and robustness are needed before enabling Spanish in a phone APK.

### Spanish synthetic A/B (initial prototype vs qualified isolated refinement)

| Category | Cases | Initial top 1 | Selected top 1 | Initial top 3 | Selected top 3 |
|---|---:|---:|---:|---:|---:|
| Joined exact | 40 | 31 | **33** | 38 | **40** |
| Joined, one typo | 40 | 20 | **21** | 27 | **28** |
| Joined, two typos | 40 | 11 | **12** | 12 | **13** |
| Correct whole words | 30 | 29 | **30** | 29 | **30** |
| One-typo words | 30 | 25 | **26** | 27 | **28** |
| **All Spanish research** | **180** | **116** | **122** | **133** | **139** |

Additional checks: 2,500 seeded Spanish strings, English router-output equivalence, Unicode normalization, and invalid-input safety. Spanish two-typo joined sentences at **12/40 top-one** are too weak for a general availability claim.

**Evidence:** [Candidate 295 pinned Spanish A/B](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945955025).

## Rejected experiments (keep out of qualified build)

- Broadly adding context-conditioned 2-edit short-word candidates caused **seven regressions** in a no-typo joined English category; the no-regression gate rejected it.
- Lowering English rescue threshold further from 0.085 to 0.055 did not improve either accuracy benchmark and produced less predictable timing. The qualified build retains 0.085.
- Changing Spanish beam scores with extra accent bonuses harmed no-typo Spanish segmentation (**31 → 20** top-one); rejected. Only display-only accent changes qualified.
- An existing top-three re-ranker alone produced **zero accuracy improvements**; it is not credited as an accuracy gain.

## Integrated release qualification

- [English A/B 487 cases](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945887995) — PASS.
- [Spanish A/B 180 cases](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945955025) — PASS (research only).
- [Native Android/IME 18 Java suites + 14 emulator tests](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945787279) — verify **actual run conclusion**; do not infer an outcome from source or earlier branches.
- [Actual installed Android IME screenshot, theme controls, key taps, Backspace gesture](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40945836275) — verify **actual run conclusion**, then inspect saved screenshots.

**No Candidate 295 APK was released.** A fresh test APK must receive a deliberate application ID/version and QA; do not overwrite the existing stable installations or reuse the ephemeral Candidate 293 debug key.

## Remaining high-value work before the next big user install

1. Confirm integrated emulator and UI runs pass and review screenshots. Investigate any failure before modifying the keyboard.
2. Improve connected English sentences with multiple typos without sacrificing exact sentence safety, now using the 487-case gate plus added independent examples.
3. Improve Spanish short-word edit handling without the regression found in the broad contextual decoder. Keep bilingual support disabled in the native IME until it has safe explicit selection controls.
4. Validate next-word context, capitalization, punctuation, three suggestion chips, and keypress latency under realistic typing sequences.
5. Re-run native Android integration and physical Samsung Android 17 acceptance for Safe Enter/Backspace and UI on a deliberately versioned **major** Candidate.
6. Once confident, produce one substantial user-facing APK with clear changes and test instructions instead of many tiny installs.

## Research integrity
Synthetic tests are not random-user samples. Ranking success is a measurable comparison between two sources on an explicit corpus, not a real-world language-accuracy claim. CI compilation, emulator keyboard interactions, Samsung device feedback, and public APK publication are **separate evidence levels**. Keep every distinct.
