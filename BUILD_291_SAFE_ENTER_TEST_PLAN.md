# Orivra Build 291 — Safe Enter (isolated from Build 288)

## Why this candidate exists
The user reports that in Google Messages, tapping Orivra Build 288 Enter unexpectedly sent a message. This is caused by Build 288 forwarding the editor's supplied action (including IME_ACTION_SEND) on a single Enter press.

## Source ancestry and verified status
- Parent: build288-backspace-render-crash-fix. This candidate intentionally does **not** include Build 289 swipe-delete or Build 290 safety changes.
- Build 288: user confirmed the Backspace rendering crash was fixed and later clarified that Build 288 is currently installed.
- Build 289: earlier six-item test response was reported, but exact install/version status should not be assumed after later clarification.
- Build 290: source candidate; not confirmed on device.
- Build 291: source-only candidate until GitHub workflow succeeds and Samsung testing is performed.

## Behavior
- Ordinary Enter tap: call InputConnection.commitText("\n",1), **only** for editors advertising TYPE_CLASS_TEXT with TYPE_TEXT_FLAG_MULTI_LINE. Never call performEditorAction or send a KEYCODE_ENTER fallback for a normal tap.
- Single-line, numeric or unknown editor: tap is a safe no-op rather than a possible submit.
- Long press: after 650 ms, if the editor advertises IME_ACTION_SEND, key temporarily displays "Send" and confirms arming with a haptic tick.
- Only a release while still in the same editor/input session calls performEditorAction(IME_ACTION_SEND). Holding alone never sends.
- NO_ENTER_ACTION prevents replacing ordinary Enter, but is compatible with a separate deliberate Send gesture.
- Moving >22 dp, another finger, cancelled touch, field switch or keyboard restart cancels the queued operation.
- The UI may be updated during a held gesture (key label only); editor action/newline happens only in a posted callback after ACTION_UP, preserving the Build 288 Backspace crash avoidance pattern.
- No INTERNET, contact or accessibility permissions; no message text is recorded.

## Automated qualification
- EnterKeyPolicyTest: SEND/non-SEND flags, multiline vs single-line, gesture cancellation/NaN, 32 hold/release/focus action combinations and single-tap never SEND regression.
- Existing Build 288 keyboard layout, haptic, suggestion and cursor regression tests.
- Gradle :app:assembleDebug, APK signing, APK identity, permission and SHA256 checks.
- Separate app ID: com.chroomareborn.keyboard.build291trial (versionCode 291).
- Artifact: Orivra-Build291-Safe-Enter-Experimental-APK.

## Physical qualification — avoid sending to real recipients
1. In a local Notes-style **multiline** field, type "hello". Tap Enter; expect one newline, no send action.
2. In a harmless single-line text field, tap Enter; expect no unintended form submission. A no-op is deliberate.
3. In Google Messages, open a conversation with yourself or another **safe test destination**. Type an obviously disposable test draft. Tap Enter once; verify it does not send.
4. Still in the safe test conversation, press and hold Enter for >650 ms. Confirm the key displays "Send" before releasing. Send should occur only on intentional release, and only if Google Messages advertises the SEND editor action.
5. Start a long press, then drag away and release: must not send.
6. Start a long press, interrupt with a second finger and release: must not send.
7. Switch text-field focus while Enter is pressed: must not change or submit the new field.
8. Verify Build 288's Backspace tap/hold and ordinary letter keys still work.
9. Rapid Enter taps in a local multiline notes field: no duplicate newline and no keyboard crash.
10. If possible, verify in an app that does not advertise SEND: long press must not submit anything.

## Release discipline
- Do not promote to stable or claim Google Messages behavior is fixed until physical tests pass.
- Do not send real messages to unwilling recipients to test.
- A GitHub Actions success is compilation/policy qualification, not a Samsung Google Messages test.
