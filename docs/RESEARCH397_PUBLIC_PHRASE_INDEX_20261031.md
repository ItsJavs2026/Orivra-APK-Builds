# Orivra Research 397 — Immutable public phrase index (no APK)

**Baseline:** released Candidate 396 commit `f0dbd4154ac3df1abe0634c475887bff993eff5d` (287/287 Android 35 tests; separately installable and currently being Samsung S26 Ultra phone-tested). The user reports 396 initially feels a bit smoother than 391 but wants continued improvement, particularly during long ChatGPT replies. **Do not label the phone issue fixed.**

## Observed source hotspot, not yet a measured Samsung bottleneck

`PredictionLite` has 29 fixed built-in conversational phrases. On every no-space prediction it repeatedly evaluates `phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","")` across four selection/ranking loops, even though the corpus is immutable. Its Split Mode phrase preview also repeatedly splits all 29 phrases, regex-normalizes their first tokens, and linearly scans for a typed first word. The general full-sentence decoder already runs off the IME UI thread, but Split Mode phrase preview is invoked from `renderSuggestions` on the UI thread.

## Narrow change

- Create `PublicPhraseIndex397` once for the existing PUBLIC phrase corpus, with the **exact former compact string** in phrase order and a read-only first-word index.
- Reuse the precomputed compact values in the four full-phrase ranking loops, preserving score, tie order, casing, duplicate handling, vocabulary, and suggestions.
- Reuse the original-order first-word list for Split Mode. No new phrases, suggestion ranking, user word caching, network access, fields, app identity, permissions or gesture changes.
- No persistent state, metadata collection, editor reads, user personal vocab, V4 modifications, or new background threads.

## Qualification gate

1. Compare every cached compact normalization and original-order first-word list against the exact old Java regex implementation. Run >=120,000 synthetic deterministic lookups, including prefixes, absent words and duplicate phrase cases.
2. Run Android instrumentation on the **actual** 29-phrase corpus, including original-case display parity and immutability, preserving all prior Android 35 regression tests (expected >=290 with zero failure, errors, skips).
3. Compare old vs new same-JVM phrase lookup timings as diagnostics only, not a Samsung latency or globally validated improvement. Do not assert universal time cutoffs.
4. Verify production files outside `PredictionLite` and new `PublicPhraseIndex397` stay identical to Candidate 396. Especially preserve `RebornImeService`, learning/V4 logic, keyboard gesture and editor safety.
5. Research only: **NO APK, NO release, NO update to installed Candidate 396**, unless independently qualified and later approved as a successor.

## Caution

This targets one specific source inefficiency, not proof of the remaining subjective lag cause. Candidate 396's optional memory-only `CandidateLatencyMetrics` key and suggestion p95 readings and prolonged Samsung reply tests are the correct next real-world evidence. Preserve Candidate 391, Candidate 396, and verified V4 backups.
