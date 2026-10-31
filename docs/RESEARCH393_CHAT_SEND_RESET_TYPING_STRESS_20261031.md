# Research 393 — Chat send/reset continuity and long-typing stress

Based on **Candidate 391**, the latest separately installed phone-positive APK at commit `133d1c2cbe3da72d2b6739ee2b24bcead1f1e78e`. The user reports: predictions sometimes disappear and ASDF/QWERTY gestures stop working after sending ChatGPT messages, then recover intermittently; sustained typing feels laggy. Two screenshots were submitted. This branch is **research-only**, not a release or a phone-verified repair.

## Source-level finding

`onUpdateSelection` formerly restored suggestions only if the host's selection changed, or if host-ended composition was recognized. A chat host can clear its composer on Send while preserving the same Android editor instance and reporting a caret at zero with stale Orivra `composing`, `latestSnapshot`, `editingInMiddle` or missing idle ticket. That can fail both the visible prediction state and the key-row gesture's `LetterRowIdleMode388.start` eligibility.

`ChatComposerReset393.shouldRecover` allows **only** a zero/zero caret with verified empty `getTextBeforeCursor(1,0)` and `getTextAfterCursor(1,0)` and stale state to restore Orivra's **local** composing/gesture metadata, cancel stale prediction generations and repaint next-word options. It **never** performs `commitText`, `setComposingText`, `deleteSurroundingText`, file access or learning writes. Nonempty drafts, selections, private fields, numeric editors and missing InputConnections remain guarded. No polling or background editor reads; extra 1-character reads occur only for zero-caret selection callbacks where Orivra already suspects stale UI. A chat host that sends **no** IME event cannot be automatically detected; report this limitation honestly.

## Mandatory sustained-typing qualification before any APK

- At least 25,000 synthetic character insertions through Android `BaseInputConnection`, with 150+ sentence boundaries, 200+ caret jumps, 100+ resets/refocus sequences, and end-of-document verification, without changing user data.
- 1,000+ dispatcher generations under rapid updates; reject superseded callbacks; confirm final generation and avoid worker accumulation.
- 20,000+ row-gesture policy cases, mixed DOWN/LEFT/RIGHT and small-word Space boundaries, plus long-message rollback to an empty editor.
- Key/suggestion timing observations on emulator, reporting p50/p95/p99 and max using synthetic data only. Do **not** misrepresent these as Samsung latency, and do not use a hard universal speed threshold on shared runners. Fail on lost text or missing final prediction, not just nominal timing.
- Full Android 35 regression after adding tests, zero failed/errors/skipped, and compare benchmark logs to Candidate 391 baseline on the **same** GitHub runner when possible.

Keep **Candidate 391** and both original V4 learning backups unchanged. If a successor APK is justified, build as a new package and independently qualify; the opt-in memory-only `CandidateLatencyMetrics` on the phone is best evidence for actual p95 lag.
