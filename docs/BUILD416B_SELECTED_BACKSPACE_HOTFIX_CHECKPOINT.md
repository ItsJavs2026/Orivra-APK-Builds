# Orivra Build 416B — long highlighted selection Backspace hotfix
Date: 2026-10-31
Branch: `hotfix/build416b-selected-backspace-20261031`
Base: `candidate/build416-space-shift-fastpath-20261031`
Status: source and pure-Java/Android regression tests committed, Android build/actual Samsung result **NOT YET VERIFIED**.

## User-reported bug
Screenshot of ChatGPT reply composer on Samsung Galaxy S26 Ultra / Android 17 shows multiple lines of audit text highlighted via Android text-selection context menu. User clarified **pressing Orivra Backspace did NOT delete that highlighted text**. Confirmed user-observed behavioral bug; screenshot alone does not prove a UI event or the exact failing internal call. This is independent of the Build 416 report and of the Build 417 dictionary branch.

## Root cause in code
`RebornImeService.backspace()` entered `prepareCursorEdit(ic)`, then either manipulated the composing span or called `ic.deleteSurroundingTextInCodePoints(1,0)` / `ic.deleteSurroundingText(1,0)`, with a synthetic DEL fallback. There was no explicit highlighted-selection replacement on normal Backspace tap. Android's surrounding-text deletion APIs are not guaranteed to delete the active selected range, and a remote editor may reject them or leave the highlight in place.

## Narrow hotfix
- Branch is completely separate from working Build 415 and 416 installations, and separate from dictionary development.
- Add pure policy helper `SelectedBackspace416B.java`; checks known active selection and unchanged editor/session; **exactly one `InputConnection.commitText("",1)`**, before any composing-finalization or ordinary character deletion. A false-return or exception is treated as uncertain and never retried via character deletion or KEYCODE_DEL. This avoids deleting neighboring text when a host reports false after applying a remote edit. Unknown/stale selection is fail-closed.
- Integrate in `RebornImeService.backspace()` before `singleBackspaceMutation338` and `prepareCursorEdit`. Leave selection callbacks unsuppressed so Android can deliver the new collapsed caret position. Only an accepted editor response causes the local composition and suggestion refresh; a rejected/ambiguous response is never followed by an additional deletion.
- For non-selection Backspace, keep the unchanged 416 fast composing, emoji, hold-to-repeat, and whole-word swipe paths.
- `OrivraPerfAudit411` gains selected-text Backspace handler elapsed-time stage and aggregate `applied/uncertain/stale session` counts. **No selection text, app identity, or typed words are logged or transmitted.**
- Add `SelectedBackspace416BTest.java` with long selections, reversed selected ranges, emoji, exact neighboring text preservation, focus change, false-but-applied remote editor, rejected editor and no-selection cases; add `SelectedBackspace416BAndroidTest.java` with `BaseInputConnection` highlighting a 4,200-char synthetic passage.
- Keep FULL predictions, quick wake, safe editor sample, background learning, haptics, and 416 Space A/B settings unchanged.

## Separate package and on-phone build
App ID: `com.chroomareborn.keyboard.build416bselectiontrial`
Version code 4161, version `1.0.71-build416b-selected-delete`
Label `Orivra 416B Selection Fix`

Run in on-phone Android Code Studio terminal after verifying known working setup:
```bash
cd ~
git clone --depth 1 --single-branch \
  --branch hotfix/build416b-selected-backspace-20261031 \
  git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Select416B
bash ~/Orivra-Select416B/scripts/build_orivra416b_android_code_studio.sh
```
Script runs **actual-checkout** pure Java regression tests first, refuses overwrite of an existing nonempty `Orivra416BSelection` project, imports Gradle 8.9 wrapper from known local `Orivra414Live`, uses ARM64 AAPT2, and verifies APK metadata before announcing success. If successful, output path `/storage/emulated/0/AndroidIDEProjects/Orivra416BSelection/app/build/outputs/apk/debug/app-debug.apk`. Don't guess file existence in chat until phone build is confirmed.

## Exact Samsung manual acceptance tests
1. In an ordinary **non-sensitive** ChatGPT draft or Samsung Notes, enter a harmless text of several paragraphs.
2. Select an entire paragraph with Android selection handles, then **tap Backspace once**, not swipe or hold. Expected: precisely highlighted text disappears; text before and after stays unchanged; caret collapses at selection start.
3. Repeat a short mid-sentence word selection and a long selection spanning line breaks. Verify the undo feature if available before larger real content deletions; preferably test with throwaway sample text.
4. Test no-selection Backspace (one codepoint only), emoji deletion, holding Backspace, backspace swipe, replacing a selection by typing, caret inside text, and notification quick reply; no extra neighboring characters should be removed.
5. With performance audit reset, share the new counts `Selected-text Backspace applied`, `uncertain`, `stale session` and any relevant timing; do not share draft text. An **applied count** means the host returned true, not absolute evidence the visible text changed — confirm visually.
6. If selection remains but counter is 0, live host selection was not recognized by the service; investigate `onUpdateSelection` propagation safely before any fallback. If `uncertain` increments, the host returned false/threw, and no second deletion was attempted.
7. Switch back to Build 416 or Gboard if the experimental 416B trial misbehaves; do not uninstall or reset the prior working app or personal learning.

## Validation boundary
Source is checked in and reviewed for guarded one-shot selection commit and separate package identity. The included pure Java runner and Android instrumentation tests execute on the user's build/test environment. No verified Android APK or Samsung test results exist in this chat at checkpoint. Do NOT claim this already fixes the user's device until they confirm successful compilation and deletion behavior.
