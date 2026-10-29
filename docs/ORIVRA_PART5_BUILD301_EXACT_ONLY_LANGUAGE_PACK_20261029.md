# Orivra Part 5 — Build 301 exact-only ESDB language research

Date: 2026-10-29. Status: English language research **passed 667 synthetic A/B QA gates** but **NOT shipped as an APK**. Candidate 300 remains the phone-confirmed baseline including working Google Keep suggestions on Samsung S26 Ultra Android 17.

## Important existing baseline
- Branch `hotfix/candidate300-google-keep-predictions-20261029`. User explicitly confirmed Google Keep predictions **fixed** on their phone.
- Candidate 300 package ID `com.chroomareborn.keyboard.build300trial`; stable Preview #178, Build 262, and Candidate 293 / 298 must not be overwritten.
- Source comparison is pinned to immutable Candidate 300 SHA `25316b3c429c94b463d0a30db8367789c5ac8792`. Later Candidate 300 doc changes do not alter this production source.
- Existing English prediction pipeline (`PredictionLite`, `SentenceRescue`, `SentenceFlowEvidenceRanker`) and Android editor/Keep privacy policies are preserved.

## Research data provenance
Official [English Speller Database, formerly SCOWL](https://github.com/en-wl/wordlist), v2 revision `1e5b7d3a72f47a71da5d28686c1dd4b397178485`, generated with `./scowl word-list 60 A 1 --categories=`. Its Copyright file is preserved in CI. The original 108,764-entry text export is around 1.02 MB; restricted research `ExactOnlyLanguagePack` retains **78,942** lowercase ASCII alphabetic tokens of 2–24 chars. No use-frequency probabilities are assumed, and the wordlist is downloaded in CI only—NOT packaged inside an APK yet.

## Root cause and solution
Earlier 100-case English audit showed 60 cases had at least one word absent from the ~2,298-word active predictor lexicon, leaving exact words such as sunrise, charger, headphones and astronomy exposed to incorrect substitutions. The first 301 approach inserted all secondary exact words into the normal beam, unintentionally multiplying alternative segmentations for typo inputs.

**Rejected first design**: [CI run 40968955715](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40968955715) showed 147-case top1 117→107, 340-case top1 247→229, and at least one correct-to-wrong individual case. Never merge that design.

**Qualified conservative design**:
- `ExactOnlyLanguagePack.java`: bounded, sorted immutable words with binary-search exact membership, no fuzzy candidate scanning or invented word frequencies.
- `PredictionLite.java`: an optional research-only injection hook; protect a complete exact word from bogus segmentation; keep the original fuzzy candidates and beam completely unchanged.
- `ExactOnlySegmentation.java`: only consider zero-letter-edit resegmentation after existing ranking, when the baseline already looks overfragmented and is one or fewer edits away, original typed letters are all ASCII, 16–65 letters long, result uses 3+ words with at least 2 well-established original-dictionary tokens, at most 2 novel long words, and fewer word boundaries than baseline.
- Never change a letter-exact baseline split just to merge a valid compound. The benchmark exposed `fresh water` vs `freshwater` ambiguity; regression guard added for that.
- `ExactOnlySegmentationTest.java`: **11 synthetic pure-Java assertions** passed, including positive sunrise and negative corrected-two-typo / ambiguous-compound safeguards.
- No unconditional suggestions, user-text storage, network permissions, or implicit tap acceptance.

## Final A/B evidence
Verified [CI run 40970156414](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40970156414) **SUCCESS**, 667 locked synthetic cases with pinned baseline and exact same QA sources. All individual tracked correct-to-wrong regression assertions passed.

| Dataset | Baseline Candidate 300 top-1 | Experimental 301 top-1 | Baseline → Experimental top-3 |
|---|---:|---:|---:|
| Original 147 | 117/147 | 117/147 | 132/147 → 132/147 |
| 340 synthetic holdout | 247/340 | 248/340 | 264/340 → 265/340 |
| Earlier 80 diagnostic | 66/80 | 66/80 | unchanged |
| Previously frozen newer 100 | 33/100 | **47/100** | improved |
| 20 newer standalone exact words (subset of 100) | 8/20 | **20/20** | 8/20 → 20/20 |

In the 80+100 individually compared audit examples: **14 wrong-to-right, 0 right-to-wrong**, and 0 correct-to-wrong in either dataset. Better joined unspaced outputs include `thetrainarrivedbeforesunrise` and `ilovelearningaboutastronomy`; `fresh water` is preserved. This remains a **synthetic fixture comparison**, not a valid estimate of real-world language accuracy.

Measured research Java resource use: around **76–81 ms pack load** and approximately **5.2–5.4 MiB additional heap** on the GitHub Linux Java 17 runner. These are not Samsung keypress latency, frame time, background memory, Android APK size or startup performance.

## Android qualification and release boundary
Source-only native Android 35 emulator regression workflow: [run 40970548089](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40970548089). **Verify final result before claiming Android pass.** The workflow compiles Android with the new Java classes, validates 18 instrumented tests and preserves `RebornImeService.java`, `EditorSuggestionPrivacyPolicy.java`, manifest and build application ID byte-identical to Candidate 300. It deliberately uploads and publishes **no APK**.

The ESDB data is **not bundled**, so even if Android CI passes, the standalone research source by itself does not add those 78,942 words to installed Orivra.

## Next actual tasks
1. If Android CI passes, design a reviewable, compressed APK wordlist asset with proper attribution, bounded application startup memory, and load-time fallback in a background worker. Do not enable it blindly in Candidate 300.
2. Add a distinct Android benchmark that measures actual pack initialization and p95 warm keystroke effects, without logging typed content. Build a standalone Candidate 302 only after package license/signing/privacy checks and real-world failure gates.
3. Add independent negative controls for real but ambiguous word combinations and user names; prevent false words and accidental correction.
4. Research contextual language data and Spanish EN/ES routing separately; existing Spanish research engine is **not** live.
5. Leave the user-confirmed Google Keep fix untouched.
