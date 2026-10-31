# Orivra Research 382 — Editor action order with delayed letter callbacks

**No APK, no IME code changes, no Samsung validation.** Research branch based on Research 380 (CI passed). Does not touch Candidate 378 or Research 381.

## Finding

Candidate 378's alphabet key handler resolves a touch through `HintKeyPressOrder353` and then posts the accepted letter action to `suggestionHandler`. Space is currently handled separately on ACTION_UP and can both insert a literal space **and** accept a live highlighted correction. A future mixed-key barrier that sequences only touch releases is insufficient: if the resolved letter action posts an editor mutation but Space edits immediately, Space can overtake the earlier letter.

## Reproducible model

`DeferredEditorOrder382Research` uses a fake FIFO callback queue and in-memory fictional text to demonstrate the naive-ordering failure, then tests a shared queued-mutation approach:

- Release Space before earlier letter: modeled editor result must still be `a `, not ` a`
- Highlighted correction must validate the final composing word **when the queued Space action executes**, not at the time of early finger release; a toy `k` + `e` scenario becomes `keyboard `
- Already-queued callbacks must refuse an editor/session generation mismatch
- Spacebar cursor swipes cannot enqueue literal Space
- 20,000 randomly shuffled key release sequences, with every editor mutation posted and drained FIFO

**This is a model, not a production integration plan.** Real integration also needs editor-identity checks, protected-field behavior, input-session state, InputConnection exception handling, prediction correctness, long-press symbols, Backspace behavior and Samsung responsiveness. Do not integrate merely because this synthetic research passes.

## User-approved baseline

User installed Candidate 378 and said “Actually it's good so far!” Keep both fat-thumb toggles on and do not change its production touch handling without phone evidence. Top-bar work is deferred to the separately agreed next session.
