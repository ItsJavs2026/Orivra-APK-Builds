# Orivra Build 292 — Unified Safe Enter + Backspace Swipe

## Motivation
User reported Build 291's Safe Enter fixed accidental sending in Google Messages, but Backspace swipe deletion did not behave as expected for a single unspaced word such as "heythere".

## Verified version distinction
- Build 288: Backspace crash fixed (user-tested).
- Build 289: Has Backspace swipe delete and earlier six-check success report (installation status later clarified as Build 288; do not overstate which APK was installed).
- Build 290: Adds swipe safety policy; source-only candidate.
- Build 291 (branch build291-enter-only-from288): Contains safe Enter, intentionally **does not contain BackspaceSwipePolicy**.
- Build 292 (branch build292-safe-enter-swipe-unified): Combines Build 290's swipe path and Build 291's safe Enter path in a new package.

## Intended safe Enter semantics (carried from the user's working Build 291)
- Tap Enter: inserts a newline only in an editor advertising multiline support.
- Tap in a single-line/nontext field: safe no-op (no synthetic KEYCODE_ENTER or performEditorAction).
- Long press >650ms: arms SEND only if editor advertises IME_ACTION_SEND.
- Release after arming: invokes the advertised SEND action, only if still in the same editor.
- Drag away / cancel / view detach / editor switch: do not send.
- Do not log or upload message contents.

## Intended Backspace semantics
- Short tap: delete one character.
- Hold: repeatedly delete characters.
- Swipe left: delete complete words on release; swipe longer for multiple words.
- Unspaced "heythere" is ONE word; a left swipe should delete the whole 8-character token. It should NOT depend on spaces surrounding the word.
- Swipe left then back to cancel: delete nothing.
- Invalid swipe or editor switch: fail closed, never delete from a different editor.
- The swipe uses word granularity, **not** partial letter-by-letter deletion inside a word. Implement that as an explicit different gesture only if requested.

## Android-specific regression
- Never mutate the keyboard view hierarchy during touch dispatch (Android 17 Build 287 null LinearLayout child crash).
- Suggestion strip/prediction panel must remain responsive after deleting a composing word.
- Check inputConnection.getTextBeforeCursor behavior when composing a fresh word and when editing existing text.

## Mandatory physical test matrix (use disposable text)
1. In a safe local note, type **heythere** without spaces. Swipe left from Backspace; the entire word should disappear.
2. Type **hey** without spaces. Swipe left once; expect the entire word removed.
3. Type **hello world**. Swipe once; expect `hello ` to remain.
4. Type **one two three**. Longer swipe; delete multiple whole words.
5. Type **heythere**. Tap Backspace once; expect `heyther`.
6. Hold Backspace on unspaced **heythere**; expect character repetition only while held.
7. Start swipe and slide back; no deletion.
8. Select text, swipe delete; remove selection without adjacent words.
9. Type **hello** then **Enter** in multiline note; expect newline, never SEND.
10. Test Google Messages in a **self-addressed draft**: tap Enter; must not send accidentally.
11. In a safe self-addressed test, hold Enter until **Send** is displayed; release to intentionally send if supported.
12. Drag away, multi-touch, or switch focus mid-Enter hold; must not send.
13. Repeated rapid Backspace and Enter operations; no stuck keys, blank predictions, or crash.
14. Close and reopen keyboard, switch app, revisit typing; no stale pending action in another field.

## Qualification and release rules
- This build has separate package ID: com.chroomareborn.keyboard.build292trial.
- Automated Java suites: EnterKeyPolicyTest, BackspaceSwipePolicyTest (including 12,000 deterministic text samples and 'heythere' cases), plus existing layout/haptic/prediction/cursor tests.
- GitHub Actions validates Gradle compile, APK signature, package identity, permissions, and hash.
- Do not mark Build 292 stable until it passes both CI and Samsung testing.
- Do not overwrite Build 288, Build 289, Build 290, Build 291, or Preview #178.
