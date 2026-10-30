# Orivra Research 340 — composing safety, tapped-word sequences and long-text latency

Date: 2026-10-30. Research-only branch, derived from the pinned Candidate 339
source commit `3bd2e877dc66a40efa172f7fa9e448ca476a2147`.

## Reported issues
1. The caret/highlight sometimes appears to lose the active word and adds an extra
   letter. Device reproducer not yet available, so this is a defensive fix of a
   concrete double-write risk, **not a claimed Samsung fix**.
2. Full-sentence/multiword predictions should learn from ordinary individual key
   taps and unfamiliar words, not only explicit suggestion taps.
3. Long messages occasionally produce a typing lag spike that then disappears.

## Source changes (isolated)

- `ComposingMutation340` checks same-session editor text before/after one
  composing write. A remote editor returning false *after applying the write*
  is not retried, preventing one specific duplicate-letter path. If the editor
  retained an active composition, it is finished without changing visible
  text before new input. Fallback commit is allowed only when there was no
  verified mutation. Ambiguous/stale operations are not blindly retried.
- `RebornImeService` suppresses reentrant selection callbacks while that
  composition transaction executes. Existing external cursor edits, live
  correction tickets, Backspace final-letter handling and gestures remain.
- `PrefixCandidateCache340` deduplicates main-thread personal frequency
  preference lookups during immediate, split and asynchronous render passes.
  Source learning writes, opt-in toggles and new editors clear the cache.
- Split phrase/next-word lookups are reused for the same composing snapshot.
- The existing `PersonalNextWordModel` now observes genuinely typed,
  successfully committed words at Space boundaries (with editor privacy,
  opt-in, selection and same-session gates). New words can contribute to
  **in-memory** bigram/trigram suggestions after repeated observed sequences.
  One occurrence is insufficient. This transient memory clears on editor
  changes and opt-out, and never auto-replaces typed text.
- `CandidateLatencyMetrics` additionally measures synchronous typed-key UI
  work, with opt-in in-memory duration statistics only (median/p95/max).
  No key identity, text, editor content or app identifiers are recorded.

## Compatibility and privacy

- Original V1, V2 and V3 backup readers/writers remain unchanged.
- V3 remains the complete **persistent** learning backup for existing supported
  personal dictionary, accepted corrections, contextual and word-frequency
  evidence. **Research 340's new unfamiliar-word session observations are not
  persisted or included in V3.** A separate reviewed persistence/portability
  design is required before claiming lasting learning of arbitrary new words.
- No new Android permissions; no INTERNET permission.
- No changes to stable Preview #178, Build 262, Candidate 333 or released 339.
- This research branch retains Candidate 339's Android app ID and must **not**
  be installed as a separate phone trial. A qualified, separate package would
  be required for any future device candidate.

## Regression work

- `ComposingMutation340AndroidTest`: successful composing insertion,
  false-but-mutated editor result, false-and-unchanged fallback, moved caret,
  ambiguous double mutation, stale input session, positive no-op, snapshot
  window, and real Android BaseInputConnection.
- `PrefixCandidateCache340AndroidTest`: repeated prefix one loader call,
  invalidation and immutable cache snapshots.
- `TappedSequence340AndroidTest`: previously unseen word learned after
  repeat word-by-word sequence, private/malformed input rejected and cross
  editor context reset.
- Dedicated GitHub Actions Android 35 emulator workflow, retaining previous
  instrumentation suite; Android 17 Samsung-specific behavior remains unknown.

## Device testing after a separately packaged candidate is qualified

1. Export and keep Candidate 333/339's V3 backup; retain prior rollback APK.
2. Type/correct a highlighted word, move the caret, continue typing and verify
   neither a doubled letter nor a replaced entire word occurs.
3. Repeatedly enter a few harmless ordinary multiword phrases with individual
   taps and Space, observe only suggestions (no automatic insertions).
4. Draft an unsent long message, note subjective pauses, and consult the
   opt-in timing summary without exporting any message contents.
5. Verify Backspace, Google Keep/Search Enter, banking numeric Amount field
   (without payment), shift/caps and privacy protections.

**Qualification status:** Source commits are research only until CI and
emulator results are confirmed. No Candidate 340 APK or phone test is claimed.
