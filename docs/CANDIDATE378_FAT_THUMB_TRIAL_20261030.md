# Orivra Candidate 378 — Fat-thumb safety and width A/B trial

**Parent:** Candidate 376, `2b920e50ed42ddeca76732013b73ec2abcf310ef` (verified prior Android 35 emulator and release). **Research input:** Research 377 pure-Java guard, CI run 41161374821 passed. New candidate is a separate package: `com.chroomareborn.keyboard.build378trial`.

## Requested features
1. **Thumb drift tolerance:** full-cell alphabet keys remain the initial hit targets. A transient pointer-bound guard permits slight slips beyond the original letter while canceling large slides. Both MOVE and final UP are verified. Never changes which key was initially pressed.
2. **Two-thumb protection:** preserves `HintKeyPressOrder353` ACTION_DOWN order, waits to rebuild until releases and fails closed on another pointer, cancellation or stale editor. New Android tests explicitly verify reverse UP order and cancellation.
3. **Better edge keys:** 10% more weight for Q/P and A/L; 8% for Z/M. The fixed width is redistributed from interiors (about 3% narrower), total row width unchanged. Shift, Delete, Space and Enter never change sizes.
4. **Two independent reversible settings:** both on by default *only in the new test app*, applied on next editor focus, not mid-keypress. Turning drift OFF preserves Candidate 376's MOVE-only 10dp legacy behavior. Turning edges OFF restores equal letter widths.

## Privacy and safety
- No key logging, touch-location logging, external service, network permission, data collection, text content in diagnostics, or changed backup policy.
- Does not override pointer tracking for secondary-symbol holds or auto-write any prediction.
- Distinguish synthetic/Android 35 emulator checks from Samsung Android 17 real typing.
- No compatibility guarantee with Candidate 376 data: separate install identity. Keep Candidate 354, Candidate 376 and protected Preview #178.
- Do **not** move top-bar gestures into this build; that is a separate future workstream.

## Validation gates
- CI restricts exact file diff against Candidate 376, verifies isolation, identity and unchanged core predictor/gesture sources.
- Runs all prior synthetic tests plus 101k thumb-guard assertions and 50k edge-weight assertions.
- Requires at least 234 passing, unskipped Android 35 instrumentation tests, Android packaging, permissions, signature and pinned dictionary before releasing APK.
- Device trial: ordinary two-thumb fast typing; keys at Q/P and A/L; long-hold symbol; slip out/in; one finger lifts before other; Space cursor, Backspace, Enter and prediction bar; alternate toggles A/B. Stop if unintended letters or regressions occur.
