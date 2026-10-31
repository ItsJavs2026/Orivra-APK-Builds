# Orivra Candidate 396 — Responsive Typing + Reliable DOWN Gesture

Research baseline `a8802b51806f960de5d89d49759cc33002cbc965` combines **Research 393** guarded empty-chat-composer reset, Research 394 loaded-learning profiling, **Research 395** fast stored-learning lookups and Research 396 exact DOWN-center handling. The user reports Candidate 391 real-phone import succeeded but keyboard feels heavy and DOWN from QWERTY/ASDF sometimes stops after replying.

## Empirical qualification before phone testing

- Research 393 passed **269/269 Android 35 tests** with 36k consecutive editor key writes, 240 external Send/refocus cycles, 2500 prediction generations and 30k swipe cases.
- Research 394 passed **273/273** and measured loaded p95 on Android emulator: 326 frequencies **9.159 ms**, 242 contexts **7.214 ms**, compared to empty 0.047/0.030 ms respectively.
- Research 395 passed **279/279**, 213,817 synthetic parity assertions, and same-run emulator p95 of old->optimized **68.564->2.063 ms frequency** and **58.848->2.838 ms context** (same runner, alternated operation order, not Samsung timing). It preserves outputs for synthetic imported/corrupt legacy keys and doesn't cache private words.
- Research 396 adds strict idle one-chip center selection and lowers explicit DOWN recognition threshold to 32dp; its required Android suite >=287 with 1200 simulated chat Send/refreshes and 120k DOWN/tap stress. Do not release unless Research396 AND this independent Candidate396 CI both pass fully.

## Phone trial protocol

Candidate 396 is an *independent* application ID `com.chroomareborn.keyboard.build396trial`; existing Candidate 391, 378, 386 remain untouched. Import a **new V4 backup exported from Candidate 391** into 396, verify the import, then export a new 396 V4 and check checksum and exact learned records with the offline verifier. Keep both files and all older apps. New app preferences start OFF; enable middle ASDF gesture and reopen a chat composer before comparing.

Test actual downward swipe on a single physically centered idle chip, 2- and 3-chip rows, one-/two-/three-letter prefix Space acceptance, short vs deliberate vertical movement, 20+ Send/replies, 3+ long paragraphs and loaded personalized suggestions. Compare with 391 using CandidateLatencyMetrics opt-in p95 typed-key UI work and suggestion view times in both apps. Record screenshots only; no private text required.

## Safety and limitations

No Internet, new telemetry, extra learning, word list re-ranking or file modification. Recovery code never writes host editor on detecting an externally cleared field. Existing center candidate must still be current and caret/editor/session verified; async changes can cause a fail-closed rather than inserting wrong words. Android emulator success is not full Samsung Android 17 IME dispatch validation.
