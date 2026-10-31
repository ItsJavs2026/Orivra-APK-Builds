# Research 394 — Loaded-learning UI profiling, not an APK

User reports Orivra Candidate 391 still functions but *feels progressively heavy/sluggish* following an imported V4 merge. The keyboard also intermittently loses next-word suggestions after chat replies. This branch is only an **instrumentation/performance experiment** derived from Research 393 `af8e408103c078c375f7a2ff21bbfd8b291af8e2`, preserving all production files unchanged.

## Source audit

The IME's ordinary on-key prediction path already dispatches expensive full predictions to the single `PredictionDispatcher` worker with a 12/20/28 ms debounce. However, some personalization work **still runs synchronously on the IME UI thread**, including `PersonalLearning.preferredCorrection` scanning up to 384 correction preferences, `PersonalWordFrequency328.matching` scanning up to 400 frequency entries on a new prefix, and `PersonalContextLearning319.suggest` scanning up to 320 context keys. Imported learning may materially increase these hot-path costs. This is a hypothesis; only measurement can establish whether these calls are meaningful jank contributors on emulation or Samsung.

## Stress profile

Use isolated synthetic SharedPreferences with **304 correction keys, 326 approved public words, and 242 context keys** matching the user's private V4 union **counts only**, with no actual user vocabulary. On Android main looper sample 900 calls each to correction, frequency, context lookups on empty and full store, reporting p50/p95/p99/max in milliseconds. Read-only integrity test runs 5000 more mixed lookups and proves no lost records or count changes. Newly accepted correction must be visible without app restart. All benchmark logs contain only numeric timing and aggregate names, never learned words/fields.

Must pass all Research 393 269 Android tests plus four new tests (>=273), zero failures/skips, plus 150k reset policy synthetic assertions. The performance values are **not** Samsung touch-to-pixel latency. No hard wall-clock p95 assertion on a busy shared GitHub runner. Compare baseline vs loaded, then determine if an opt-in cache or worker-side snapshot would be justified and safely test it before any new APK.

Retain Candidate 391, both original 378/386 backups and the newly merged verified V4.
