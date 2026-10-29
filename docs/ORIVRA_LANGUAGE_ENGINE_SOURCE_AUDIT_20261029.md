# Orivra — Language-engine implementation audit (Part 5, 2026-10-29)

**Research-only branch**. This document is a source-and-literature audit, **not a declaration that any language feature is newly available on the phone**. Preserve separately qualified Candidate 298 and all stable installs.

## Confirmed current code (do NOT rewrite)

- **English offline engine**: `PredictionLite.java` already contains a boot dictionary, exact-word protection, indexed edit-distance candidates, beam segmentation, multi-typo sentence rescue, common phrase transitions, and top-3 evidence-based alternatives. Its `suggestions()` input filter currently accepts only `[A-Za-z]+` of length <=65 and is not a general spaced Unicode sentence editor.
- **More words**: `LexiconFamilyExpander.java` was introduced in research 297. The independent 100-case sample showed **zero gains**, despite +35/80 improvements on an older diagnostic set. More appended hand-curated words alone is not a proven scalable solution.
- **Next word**: `NextWordSeedModel`, `NextWordPhraseModel`, `NextWordTrigramModel`, `NextWordTrigramConfidenceModel`, `PersonalNextWordModel` and `NextWordCandidatePolicy` exist. Do not claim all models are active in the exact same runtime path; verify `RebornImeService` use and preference gates before changing.
- **Editor/IME**: `RebornImeService` already provides IME composition, cursor-safe middle-word correction, Safe Enter, Backspace swipe, one-arrow UI and suggestion commits. `PredictionDispatcher` uses a single background worker and ~40ms debounce.
- **Personal data**: opt-in `PersonalLearning`, separately managed `PersonalDictionary`, private app storage and existing secure-field suppression. Do not silently log or upload typed text.
- **Spanish**: `SpanishPredictionLite` and `BilingualPredictionRouter` already exist as explicit-language, offline research. Spanish NFC normalization, accent-aware display, beam alternatives and limited tests exist. The router is NOT called by the live `RebornImeService` in Candidate 297/298. Mixed EN/ES prediction is NOT ready.

## Research-backed technical priorities

### P0: Unicode-safe editor/predictor boundary (research first)

`PredictionLite.suggestions()` has `input.matches("[A-Za-z]+")`, while Spanish handles NFC and combining accents separately. Build a *non-destructive* `LanguageInputNormalizer` test layer:
- NFC for recognized language tokens without modifying editor text;
- distinguish lowercase lookup keys from original display, `ñ` vs `n`, and acute accent from unaccented forms;
- classify whole-word, connected-word, spaced phrase, apostrophe, newline, punctuation, URLs/emails, digits, emoji and mixed scripts;
- never turn a URL/password into a spelling correction, never alter unselected editor text;
- honor UTF-16/grapheme boundaries before applying a cursor replacement.

Use Android ICU `BreakIterator.getWordInstance(Locale)` for *existing* word boundaries, not as a solution to intentionally missing spaces. Android API >=24 supports this. Unicode UAX #29 defines boundary rules including apostrophes, combining marks and emoji ZWJ sequences. Relevant references:
- https://developer.android.com/reference/android/icu/text/BreakIterator
- https://www.unicode.org/reports/tr29/
- https://developer.android.com/reference/android/view/inputmethod/InputConnection

### P1: Dictionary packs, not one giant always-fuzzy hash map

Separate frequency-ordered active words from a larger exact-only lexicon; use a bounded and measured candidate index. Adding all dictionary words as fuzzy candidates makes short tokens and names vulnerable to false corrections and increases CPU. Add a contextual unambiguous promotion path only after independent A/B tests. Normal words must win over nonsensical segmentation. Design vocabulary packs with file identity, corpus version, attribution, and memory budget.

AOSP LatinIME dictionary formats and n-gram data are useful references; copying platform code requires separate license checks:
- https://android.googlesource.com/platform/packages/inputmethods/LatinIME/
- https://github.com/mudita/MuditaOS-K-Keyboard-Dictionary

SCOWL 2020.12.07 may supply a redistributable *spelling vocabulary* with notices, but it is not a trustworthy word-frequency/usage probability source. Review its entire `Copyright` and any merged upstream licenses before shipping:
- https://sources.debian.org/src/scowl/2020.12.07-4/Copyright
- https://wordlist.aspell.net/

### P2: Context-first candidate recall, not another unqualified rerank

SymSpell's compound correction and word segmentation illustrate bounded edit candidate generation with language model evidence. Orivra already includes a `SymSpellCandidateBridge` and multiple decoding approaches, so inspect reuse before introducing an incompatible library or recursive search. The MIT implementation and the datasets packaged by third parties have separate licensing questions:
- https://github.com/wolfgarbe/SymSpell

Use n-gram/backoff scores to disambiguate competing token boundaries. Separate candidate **recall** (whether target exists in top 8) from selection/ranking (top1/top3). Freeze blind out-of-distribution sentence sets before algorithm adjustments. Reject per-case regressions, preserve known words, and measure cold and warm latency/memory.

### P3: bilingual enablement, only after language-switch design and safety

Keep English production prediction until explicit user language choice and Android 17 testing. For Spanish, support `¿`, `¡`, accent marks and `ñ`, not automatic English folding. First build an offline explicit-locale test API, then language-specific dictionary packs and transitions. Mixed-language auto-detection should be optional and conservative; avoid converting names and code-switching words without strong context.

## Concrete acceptance metrics

- Always report exact top-1/top-3 denominators, per-case wins and losses, and incorrect *auto*/suggested replacements separately.
- Include untouched exact words, joined sentences 0/1/2/3 edits, spaced context, partial/middle edits, text with Unicode/accent, punctuation and emoji, and irrelevant URLs/emails as negative controls.
- Test candidate generation recall@8, top-1/top-3, warm p95, worst-case allocation, IME frame drops, and input-connection behavior; CI Java timings are not Samsung typing latencies.
- Stable Preview #178, Build 262, Candidate 293 and Candidate 298 must not be overwritten or merged. Prototype new language changes only on this isolated branch until their own test gates pass.

## What the user needs to supply

**Nothing further for coding or GitHub CI.** For handset acceptance of a *separately packaged* Candidate 298: install it alongside existing versions and verify in a non-sensitive local editor, then share pass/fail observations and screenshots if possible. Do not repeat already completed earlier baseline tests. The user's S26 Ultra Android 17 is the primary real device; emulator screenshots and tests are not a substitute.


## First experimental source implementation (2026-10-29)

- `LanguageInputProfile.java` now classifies ASCII and Unicode Latin strings, NFC-normalizes a separate lookup copy, preserves original editor text, distinguishes `ñ` and Spanish accents, and rejects URL/email/number/emoji or malformed-surrogate inputs for future research paths.
- `LanguageInputProfileTest.java` checks 28 pure Java assertions, including decomposed `café`, combined accents, mixed scripts, standalone symbols, punctuation, and offsets after normalization.
- [Research-only GitHub CI](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40961495431): **PASS, 28 assertions**.
- **Not wired to `RebornImeService`, `PredictionLite` or a handset APK**. This is an initial parser foundation, not language detection, automatic correction, or bilingual release functionality.
