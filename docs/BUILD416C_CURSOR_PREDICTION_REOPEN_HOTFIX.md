# Orivra 416C — cursor-word prediction taps after keyboard hide/reopen

Date: 2026-10-31. Branch: `hotfix/build416c-cursor-chip-reopen-20261031`
Base: `hotfix/build416b-selected-backspace-20261031`
**Status:** source hotfix committed and static source verified. Pure Java and Android tests are included but have NOT been executed against user's Samsung in this chat. No APK is supplied or claimed to be installed. Preserve Build 415, Build 416 and Gboard as fallbacks.

## Reproduction from user
On Build 416 (not 416B), suggestion chips initially seemed to work. The user hid the Samsung keyboard, reopened it at the same word-under-cursor and **could no longer select/click a prediction to correct that word**. Earlier user phrase "predictions being forgotten" now specifically means visible suggestions are unresponsive, not loss of the personal word dictionary. The earlier third report (620 seconds, FULL/Idle Paint ON, 145ms wake, Fast Space shift OFF/haptics OFF) had 159/163 prediction workers delivered, 59 stale side chips hidden, 0 discarded frames; these metrics did NOT count tap validation failures.

## Confirmed static source defect (not yet physical device proof)
`RebornImeService.showSuggestions()` uses `captureEditingTicket(word)` for committed words under cursor, sets `visibleEditingTicket`, renders safe options, and the actual tap in `acceptSuggestion()` demands `isEditingTicketCurrent(visibleEditingTicket)`.
But `RebornImeService.applySuggestionsNow412()` **unconditionally reset `visibleEditingTicket=null` on EVERY frame**, including the very frame displaying the committed-word options. This made side corrections visible yet unusable, independently of whether offline model completed. Hide/reopen invokes `invalidateEditingTicket()` in the input view lifecycle, so the newly captured ticket must stay alive through the next valid paint. The older reset destroyed it again.

Additional lifecycle defect: `predictionSwipeEnabled372` was reloaded in `onStartInput()` but **not** `onStartInputView()`. Switching experimental prediction swipes OFF and hiding/reopening the same editor without a new `onStartInput()` did not necessarily apply the switch.

## 416C fix and strict safety
- Add `CursorWordChipTicket416C.preserve(...)`: only retain a committed-word ticket when frame input matches its word exactly (case sensitive), ticket's editing revision equals current revision, selection start/end unchanged and collapsed, and editor is not composing, selected or sensitive. Pure policy makes no InputConnection read on paint.
- Replace blanket `visibleEditingTicket=null` in `applySuggestionsNow412()` with `reconcileEditingTicketForFrame416C(input)`. Invalidate ticket for all nonmatching contexts, empty/next-word rows, selections, new editor generations, private fields, and active composing rows.
- Actual correction tap **still** runs `CursorEditSnapshotGuard.current()` on the live before/after text, then `CursorWordCorrectionPolicy.plan()`; no bypass of stale-editor safety. This is NOT a "force click stale chip" fix.
- Reload `PredictionSwipe372.PREF` in `onStartInputView()` as well as `onStartInput()`; existing toggle should now work after hide/reopen even in same editor session.
- Add aggregate audit counts: `Cursor-word ticket retained at paint`, `Cursor-word ticket cleared at paint`, `Cursor-word suggestion tap applied`, `Cursor-word suggestion tap rejected`, `Suggestion-swipe touch release rejected`. No text, app name, cursor position, dictionary words, or usernames recorded.
- Keep 416 Space shortcut, 415 performance optimizations and 416B selected-text Backspace hotfix unchanged. This is a separate, reversibly installed package. Build 417 dictionary research unaffected.

## Tests in source (NOT yet executed on Samsung)
- `CursorWordChipTicket416CTest.java`: 210 Java synthetic assertions on exact word, contractions, changed revision, changed caret/selection, composing, private fields, empty frame, multiple safe frames.
- `CursorWordChipTicket416CAndroidTest.java`: Android `BaseInputConnection` editing a synthetic word, ticket survives a valid frame, invalidates across hide/reopen revision, fresh ticket is usable, altered editor text remains rejected.
- `scripts/run_orivra416c_core_tests.sh`: inherited 413, 414, 415, 416, 416B pure-Java tests plus 416C. Note that a phone build will automatically run this suite before Gradle; a pass is not actual Samsung IME behavior.

## Android Code Studio build on Galaxy phone
Distinct applicationId `com.chroomareborn.keyboard.build416ccursortrial`
VersionCode `4162`, version `1.0.72-build416c-cursor-tap`
App label `Orivra 416C Cursor Tap Fix`.
```bash
cd ~
git clone --depth 1 --single-branch \
 --branch hotfix/build416c-cursor-chip-reopen-20261031 \
 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Cursor416C
bash ~/Orivra-Cursor416C/scripts/build_orivra416c_android_code_studio.sh
```
Known guard checks source branch, Gradle 8.9 wrapper from installed working `Orivra414Live`, local ARM64 AAPT2 and separate app identity/version metadata. It refuses to overwrite nonempty export folder.
If actual local output checks pass, APK is `/storage/emulated/0/AndroidIDEProjects/Orivra416CCursor/app/build/outputs/apk/debug/app-debug.apk`. This path is an intended output, not a verified downloadable file from ChatGPT.

## Minimal on-Samsung acceptance test
1. Keep Build 416 and Gboard installed. In Orivra 416C Settings, **Swipe prediction selection OFF**, reopen the keyboard to apply it; FULL/Idle Paint defaults can remain as tested. For first test use disposable normal text in ChatGPT or Notes, e.g. `I made a typinng mistake.` with an intentionally misspelled common word.
2. Place caret in/at end of `typinng` so the center chip displays the currently typed word and one or more real side corrections. Tap a DIFFERENT suggestion, not the original center word. Verify only target word changes, no adjacent text or double spaces.
3. Restore fresh test draft, put caret back on same word, **hide keyboard** then reopen it without changing word/caret, wait for strip and tap a different correction. The correction must still be accepted (the prior Build 416 failure).
4. Repeat with caret in middle of a sentence, across new app/editor, after a long draft, selection/highlight, and in a private editor (no suggestions). Verify Backspace selection fix from 416B using throwaway highlighted paragraph.
5. With audit ON/reset: check `Cursor-word ticket retained at paint` >0 for valid edit frames, `Cursor-word suggestion tap applied` increases on a successful correction, `rejected`/swipe rejection counts as appropriate; if tapping still fails, report whether tap visually presses and the exact aggregate counters. Counter "applied" means editor returned true, so verify visible text independently.
6. If errors, do NOT disable editor snapshot checks. Use 416 or Gboard and investigate on-device event ordering or stale selection echoes; this patch may not fix every host's IME callbacks.

## Remaining issue / workstreams
- This is **tap interaction** recovery, not an offline dictionary extension nor proof of learned word persistence. Continue 417 large dictionary only after UI bugs are isolated.
- After this source hotfix is qualified, add safe aggregate count for cursor correction render with missing ticket and stale async callback suppression if live complaints persist.
- No physical-touch-to-frame latency evidence can be inferred from timing-only audits.

## 2026-10-31 — UX preference: prediction-bar swipe defaults OFF
User said prediction-bar swipes are no longer used; they prefer the independent prediction swipes on actual QWERTY/ASDF letter keys.
- Updated the **416C source before phone installation**: default for `PredictionSwipe372.PREF` is now **false** in `RebornImeService` initial field, `onStartInput`, `onStartInputView`, and the settings toggle text/click handler.
- The gesture remains available if explicitly switched ON; no preference overwrite/migration. An existing saved ON setting still wins. **Normal prediction-bar taps remain ON** and continue to verify live editor state. `LetterRowPredictionSwipe386.PREF` behavior and mode defaults are not modified.
- **Separate app package = separate preferences.** The user may need to enable their preferred letter-row swipe mode in 416C Settings after install; settings from the old 416 package do not automatically transfer.
- Current branch includes this change. If user already cloned the branch before this commit, they must update the clone before running the build; the build script exports exactly local HEAD and does not automatically fetch remote commits.
- This change is small and statically verified in GitHub, but no compiled 416C APK / Samsung test yet.
