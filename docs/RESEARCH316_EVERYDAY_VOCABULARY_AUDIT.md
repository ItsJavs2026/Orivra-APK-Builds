# Build 316 research: everyday English source-vocabulary audit

## Method
Compared candidate Build 315 source vocabulary with the top ranked ordinary
alphabetic terms in the public `first20hours/google-10000-english` list.
The reference is website-heavy, not representative phone conversation text.
Its original corpus and licensing do NOT authorize wholesale reuse in an APK.
Reference is used *only* by CI as a read-only, pinned external research input.

The static-source approximation (2,194 distinct letter-only words) suggested
62.6% coverage of alphabetic terms in the first 1,000 ranked reference entries
(974 eligible words). That is a rough diagnostic, not a claim of actual
runtime lexicon coverage or full-sentence model accuracy.

Research 316 adds a conservatively curated English vocabulary of general
grammar, communication, work, technology, and everyday-action words.
Existing frequencies unchanged; new words have minimal fuzzy evidence (1). This adjustment eliminated a one-case top-three regression in the 60-case joined-three-typos holdout group.

A Java reflection audit measures *actual runtime* map coverage against the
pinned list and prints missing counts for before/after. The full 147+340
SentenceFlow fixture suite must be compared before adoption, with no
known-word or full sentence top-3 regression accepted.

The optional user-opt-in ESDB dictionary is exact-only and does not currently
solve everyday typing corrections: word recognition and typo generation are
distinct problems.

This is research source only; do not change the Samsung Build 310 keyboard
or release until Android and emulator tests qualify.

## Verified pinned CI A/B at experimental frequency 1

- Actual initialized base vocabulary: 2,298 entries; Build 316: 2,542.
- Top-1,000 pinned frequency reference recognized: 627 -> 780 (+153).
- Existing 147-case benchmark: all six category top-1 and top-3 counts unchanged.
- Independent 340-case holdout: exact and one-typo joins improved by one top-1/top-3 case each; all other categories remained non-regressing, including 26/60 top-three for joined-three-typo.
- Frequency source is web-oriented; these measurements do not establish everyday messaging accuracy or real-device feel.
