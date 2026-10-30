# Orivra Research 324 — full integration

Combines actual Android 35-qualified separate branches:
- 321: successfully committed ordinary words reinforce accepted corrections
  and safe next-word habits; existing opt-in and V1 backup preserved
- 322: V2 backup-format prototype, not yet wired into live export/import
- 323: secure numeric amount keypad with real digit rows, decimal, ABC
  fallback, and protected private-field learning

Based on Research 322. Numeric IME implementation transplanted from
qualified Research 323, without dropping 320/321 adaptive word usage.
Preserves phone-tested Candidate 319 tap timing and all existing gestures.

No package ID modifications; no APK in this branch. Full Java,
Android compilation, Android 35 emulator tests, and independent
English A/B are required before any new Candidate is published.
