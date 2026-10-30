# Orivra Research Build 320 — contextual word usage

Isolated follow-up to phone-test Candidate 319. Candidate 319 is unchanged.

## Issues addressed

1. **Split next-word taps were excluded from the original learner.** This is
   the frequently used right-side suggestion row. A confirmed explicit tap
   now records a safe previous-word -> selected-next-word relationship,
   protected by opt-in, editor privacy, and current input session validation.
2. **Accepted sentences previously ignored a new word as a preceding
   context.** For the explicitly accepted phrase "please tell me", it now
   learns both `please -> tell` and `tell -> me` as two distinct pair
   counters. It still does not persist the three-word sentence.
3. **Manually saved novel words** can be contextual next-word suggestions
   after the user taps them twice following safe words ("like -> flibbertik").
   Unknown names or unsaved words are never silently collected.
4. **Suggestion-strip responsiveness:** when considering a manually saved
   candidate, load the short personal word list at most once per strip
   update rather than once per pair. No new callback, threads or permissions.
5. Opt-in OFF, private editors, stale cursor, selections, email/URL contexts
   and text-after-caret all prevent the new split-learning path.

We still deliberately do not collect raw ambient typing or automatically
learn full arbitrary sentences. No telemetry, no INTERNET permission, and
existing V1 export format excludes context pairs. Regression includes Java
privacy policy plus synthetic Android SharedPreferences integration and the
full Android 35 emulator suite before any APK publication.
