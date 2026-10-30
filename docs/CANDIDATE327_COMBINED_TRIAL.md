# Candidate 327 — combined features, rollback-safe Android trial

Source pinned exactly to qualified Research 327:
`46456605e0f7205f32782545b5c94ba7d8b00be9`.

Inherits Candidate 319's phone-positive key feedback, the qualified
financial numeric keypad, everyday-English lexicon and accepted/used
word contextual learning. Exposes a *separate* optional V2 backup button.
The original V1 export button remains backward compatible.

V1 default export filename: orivra-local-learning-backup.txt
V2 default export filename: orivra-context-learning-v2-backup.txt
Import accepts V1 and V2 and retains explicit approval, count maxima,
privacy checks and the existing user Learning ON/OFF preference.
V2 is plaintext and may reveal saved word associations; treat as private.

VersionCode 327, app id com.chroomareborn.keyboard.build327trial.
This trial does not overwrite Candidate 319, 324, or the default IME.
A new app id means its dictionary is isolated until V1 is imported.

GitHub Actions must pass source diff gate, no INTERNET permission, pinned
offline dictionary checks, 50+ Android emulator tests, signature, sha256
and artifact upload before an APK is considered qualified.
No phone test of the real banking app has happened yet. Do not test by
submitting a payment.
