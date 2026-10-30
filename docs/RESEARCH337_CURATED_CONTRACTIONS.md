# Research 337 — known everyday contractions must actually learn

Source gap: EnglishApostrophePolicy recognizes curated tap-only forms
such as can't, ain't, that's, we're, I'm, y'all, but
PredictionLite.publicWordForContext previously checked only WORDS,
which does not contain apostrophized forms. Consequently:
- PersonalWordFrequency328 refused completed contractions.
- Phrase/split acceptance could not reinforce contraction frequency.
- V3 export/import could not retain their observed frequency counts.

Solution: reuse ONLY the existing bundled exact contraction list as
public vocabulary. Keep all arbitrary possessives, names, account data,
URLs and malformed apostrophes excluded from automatic learning.

No change to tap-only behavior, word thresholds, numeric/PIN editor
privacy, V1/V2/V3 format, permissions, or installed Candidate 333.
Pure Java regression tests cover accepted forms and rejected names;
Android instrumentation verifies opt-in frequency and V3 roundtrip.

Research only until Java, Android compilation and Android 35 emulator
tests all pass. No user install requested for this intermediate build.
