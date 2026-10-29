# Orivra SentenceFlow — Baseline, First-Letter Lexicon Consistency, and Next Development

**Checkpoint:** 2026-10-29  
**Isolated experimental branch:** `candidate/build294-sentenceflow-starter-lexicon-20261029`  
**Protected UI baseline:** `candidate/build294-suggestion-visual-motion-20261029` (single arrow, full-width suggestions, theme chip contrast, keyboard motion; unchanged).  
**Frozen prediction baseline SHA:** `ab02b22d458613e76d2e285ad00cb886bd6a25dc`  
**Source fix SHA:** `103cf184bdc579c6e32840ee86943dc041b8bd04`  
**Passing A/B workflow:** [GitHub Actions 40939107301](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40939107301).

## Why this change

The original predictor exposed common correctly spelled words as first-letter completions (for example, `q` offered `question`), but some of those completions were missing from the exact-word dictionary used by the full-word prediction path. A complete word could be wrongly segmented into unrelated fragments.

The boot dictionary now imports all **clean alphabetical, length 2–24** first-letter completions at modest frequency, without overriding any established frequency. This happens BEFORE prefix/length indexes are built. Apostrophes, hyphenated forms, shortcuts, and sensitive-field behavior remain unchanged. This is a general source-of-truth consistency rule, not a one-word fix.

## Verified testing

The fixed 147-case benchmark uses only synthetic text and reports top-one and top-three exact matches. Tested both versions on the SAME GitHub Actions runner with the SAME fixtures, plus a separate starter-lexicon consistency test and the existing prediction smoke regression suite.

| Test category | Cases | Baseline top-1 | Candidate top-1 | Baseline top-3 | Candidate top-3 |
|---|---:|---:|---:|---:|---:|
| Correctly spelled words | 16 | 15 | **16** | 15 | **16** |
| One-typo words | 30 | 28 | **29** | 28 | **29** |
| Joined text, no typo | 30 | 24 | 24 | 30 | 30 |
| Joined text, one typo | 30 | 22 | 22 | 28 | 28 |
| Joined text, two typos | 30 | 13 | 13 | 19 | 19 |
| Reported difficult examples | 11 | 9 | 9 | 9 | 9 |
| **Total** | **147** | **111 (75.5%)** | **113 (76.9%)** | **129 (87.8%)** | **131 (89.1%)** |

**Additional checks:**
- All 72 plain first-letter completion words in the test were safe as complete input (178 assertions).
- Original `PredictionSmokeTest` 78 assertions passed.
- A/B comparison gate verifies category counts, no overall top-one regression, no loss on known words, and no top-three regressions in joined-text groups.
- No predictor UI, installed app identity, APK, network permission, or personal data were changed.

These percentages describe **this fixed small synthetic corpus**, not a representative model accuracy estimate. CI timing varies; do not assert an Android handset latency improvement from a Java GitHub runner.

## Open weak spots (not verified fixed)

- Joined phrases with two typos: **13/30 top-one** and **19/30 top-three**.
- Examples remaining difficult include `howsotgkikf` -> `how's it going` and `okayniwweregettijgsokewjere` -> `okay now we're getting somewhere`.
- Current dictionary-anchored rescue has incomplete context support and avoids high-confidence promotion without evidence.
- Bilingual EN/ES, stronger next-word predictions, punctuation/capitalization recovery, and longitudinal personalization still need measured development.

## Next development gates

1. Expand the deterministic quality benchmark with **independent held-out phrases**, varied word boundaries, negative controls, and a second test sample. Never build a change solely around one remembered user sentence.
2. Prototype multi-typo connected-text rescue in another isolated branch; measure improvements and reject any regression in known-word safety, top-three accuracy, or unacceptable latency.
3. Preserve one-tap explicit selection (never silently autocorrect a whole sentence), editor-session safety, Safe Enter and whole-word Backspace.
4. Only integrate a qualified candidate into a separately versioned Samsung test APK after the UI and predictor gates are green. Do not overwrite stable Preview #178, Build 262, Build 291, public Build 292, or published Candidate 293.

**Evidence:** [Full synthetic baseline GitHub run](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40938466147) · [A/B experiment and artifact](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40939107301)
