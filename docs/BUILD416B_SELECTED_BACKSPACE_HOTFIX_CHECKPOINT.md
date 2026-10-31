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


## Follow-up Samsung A/B: Build 416 Fast Space Shift OFF, haptics OFF (2026-10-31)
User sent another REAL Samsung Build 416 performance audit, **717 seconds**, same FULL/Idle/Calm/Rise/Frame/Fast compose/Background Space learning/Deferred idle/145ms quick wake/Safe editor sample/Shaded Surface settings as prior report; **Fast Space shift OFF, haptics OFF**. Prior ON report was 870s and also haptics OFF.

| Metric | ON report | OFF report |
|---|---:|---:|
| Letter handler | n=212 avg3.31ms max35.32ms | n=121 avg2.86ms max34.54ms |
| Space handler | n=9 avg25.46ms max49.47ms | n=6 avg33.12ms max44.07ms |
| Space cursor/sentence stage | n=8 avg14.89ms max19.97ms | n=4 avg24.17ms max29.42ms |
| Space scheduling substage | n=8 avg0.03ms | n=4 avg0.03ms |
| Space before-commit checks | n=8 avg2.29ms | n=4 avg1.32ms |
| Space after-commit handling | n=8 avg5.13ms | n=4 avg3.37ms |
| Suggestion scheduling | n=410 avg6.61ms max126.20ms | n=290 avg6.73ms max129.18ms |
| Suggestion render | n=446 avg2.11ms max38.17ms | n=288 avg1.96ms max45.61ms |
| Keyboard layout refresh | n=47 avg16.55ms max67.17ms | n=39 avg15.10ms max107.65ms |
| Predictor worker | n=189 avg5.44ms max40.32ms | n=112 avg2.67ms max24.36ms |
| Word-at-cursor | n=195 avg1.82ms max20.90ms | n=175 avg1.36ms max15.91ms |
| Key initial editor read | n=60 avg0.73ms max1.19ms | n=39 avg0.88ms max8.32ms |

Additional OFF report: backspace n20 avg3.18ms max31.82ms; idle word observation n121 avg0.06ms max0.19ms; 35 stale chips hidden; 0 discarded frames/retries/learning overflow; prediction worker 113 scheduled / 1 preempted / 112 delivered. **Fast Space path ON 8 hits/0 fallbacks; OFF 0 hits/4 fallbacks**. ON-vs-OFF comparison controls haptics OFF but is **not same input, not paired/randomized and extremely small n** (Space stage 8 vs 4) and elapsed durations are unequal. The average whole Space handler is 7.66ms (~23.1%) faster ON; its cursor/sentence stage 9.28ms (~38.4%) faster ON; meanwhile the worst full Space handler maximum is higher ON. This is preliminary support that bypassing the extra editor read is useful, but not proof of consistent touch latency or all-host correctness.

### Correct action
- Mark previous interpretation "fast path has not demonstrated improvement" as **superseded by the first matched-haptics ON/OFF comparison**. Do not rewrite original audit numbers.
- Continue ON only if there are no capitalization, selection or long-draft editing errors. Repeat multiple A/B samples with at least 30 Space presses per arm and similar harmless content. Reset audits each arm; reopen editor after changing toggle.
- Investigate the remaining 14.89ms ON cursor/sentence stage via separate aggregate timing around `cursorHasTextAfter() / getTextAfterCursor(1,0)` before attempting to eliminate that live-editor safety check.
- The user's **long selected text fails to delete with Orivra Backspace** on Build 416. A separate `hotfix/build416b-selected-backspace-20261031` source branch has a single guarded `commitText("",1)` selected-range deletion path. This report is clearly **Build 416**, not a 416B fix verification. Continue testing 416B on disposable text. Dictionary Build 417 remains independent.
