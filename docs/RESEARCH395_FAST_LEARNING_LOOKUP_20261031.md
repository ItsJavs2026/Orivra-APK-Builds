# Research 395 — Remove redundant main-thread regex from loaded learning

**Baseline:** User reports sluggish keyboard as though a foreground app were heavily loaded. Research 394 on Android 35 (synthetic merged shape 304 corrections/242 contexts/326 frequencies) measured p95 **9.159 ms** for `PersonalWordFrequency328.matching`, **7.214 ms** for `PersonalContextLearning319.suggest`, compared with empty-store 0.047/0.030 ms; learned-correction lookup p95 0.212 ms. These are emulator measurements, NOT Samsung S26 Ultra measurements.

This branch starts from successful Research 394 commit `73e9561a584414969a4170b6a26d45be8b2b7035`. It includes already qualified Research 393 external-chat-composer recovery after Send and 36,000-character/240-send/2,500-generation stress. It adds only:
1. `FastStoredLearningPolicy395.definitelyNotPrefix`: quickly discard canonical lowercase frequency keys that cannot match the current prefix before expensive repeated apostrophe/word regex validation. Noncanonical, uppercase, malformed, curly apostrophe or legacy entries always use the unchanged strict validator.
2. `FastStoredLearningPolicy395.canonicalContextTarget`: pure ASCII lowercase/internal-straight-apostrophe validation, matching exactly `target.equals(AcceptedContextPolicy319.acceptedWord(target))` without regex or lowercase allocation for each stored key.
3. No caching of editor text, no changed ranking, no consent changes, no API/network, no new permissions and no migration writes. The live SharedPreferences store remains the single authority; learned changes still appear immediately.

## CI qualification

- At least 200k pure Java randomized parity scenarios (including malformed legacy keys), plus Android parity against the original strict validators and same-run before-vs-after lookup timing.
- Full 36k long-key, 240 chat sends, 2,500 predictor generations, 30k row swipes and 273 prior Android tests; new Android tests lift total target to >=279.
- Profile p50/p95/p99/max for frequency and context under same synthetically loaded 304/242/326 store with same-run strict-reference and optimized methods. Do not use timing-only success; all result lists must match, overlapping counts and live learning must be retained. Hard runner-wide p95 thresholds are brittle; inspect measured percentiles before deciding a separate trial APK.
- No Candidate 391 installed app changes and no release from research. Preserve user V4 originals and merged learning. Candidate 391 phone responsiveness remains the on-device control.
