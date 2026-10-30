# Orivra Research 331 — complete V3 integration

This stage connects the V3 frequency codec to Android's existing explicit
Storage Access Framework import/export workflow. It does not modify the
released Candidate 327 APK.

The user can still export:
- V1: hand-saved dictionary and explicit accepted corrections
- V2: those plus small approved adjacent word associations
- V3: everything above plus the individual completed-word frequency
  counters that allow `con → continue` and `m → my`

V3 uses a distinct filename, is plaintext, and requires user confirmation.
One import control reads V1, V2 or V3 and performs full preflight before
writing anything. Each store merges max counts, not additive counts.
Import never enables Learning. The older export routines are unchanged.

No INTERNET permission or automatic file transfer. Multiple separate
SharedPreferences files cannot commit atomically; preflight protects
against malformed input, not unusual filesystem failure. Android
instrumentation tests restore local synthetic preferences afterward.
