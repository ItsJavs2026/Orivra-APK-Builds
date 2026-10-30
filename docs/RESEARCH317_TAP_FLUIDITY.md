# Orivra Research 317 — responsive typing-feel study

The user compared working Candidate 310 with Gboard and reports that Gboard
feels smoother and more reactive (with one observed repeated `j` in `jjust`).

Source investigation found two different common key paths:
- Basic keys trigger their action on ACTION_DOWN.
- Alphabet-hint letter keys commit a character on ACTION_UP to preserve
  deliberate long-press symbols, with safe cancellation and no duplicate
  normal key insertion. Changing them to ACTION_DOWN without an editor
  rollback transaction would risk accidental letters and double input.
- Every ordinary space previously called `refreshView()`, removing/recreating
  the entire key layout. That is unnecessary when Shift and layout remain
  unchanged and may cause interaction jank.
- Keycap animation was 55ms down / 95ms release, .958 press scale.

Changes in this branch:
1. Normal letter release first dispatches the text edit and only then starts
   the release animation. The long-press and cancel decision is unchanged.
2. Spacebar only asks for a full keyboard rebuild when automatic Shift
   actually changes. Other spacebar gestures and next-word suggestions remain.
3. Reduce visual compression to .978 and anim duration to 32ms/56ms.

All existing touch guards remain. No early ACTION_DOWN letter insertion
because hold-symbol semantics must remain correct. No typing telemetry or
user text recording. Static 100k seeded motion regressions plus Android
35 emulator tests are required. Real Samsung feel must be evaluated by
the user after an explicitly approved APK, not inferred from CI.
