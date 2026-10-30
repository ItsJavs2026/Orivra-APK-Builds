# Orivra Research 312 — English and Spanish personal learning

Research branch only, not a phone APK. Candidate 310 and Research 311 remain unchanged.

- Normalizes Spanish accents and ñ to NFC without accent stripping; English apostrophe spellings preserved.
- Explicit acceptance-only opt-in Learning V2.1; no passive keystroke capture or raw sentence history.
- Two consistent acceptance taps required to rerank; ties remain undecided.
- The existing V1 SharedPreferences keys and ORIVRA_LOCAL_LEARNING v1 backup grammar remain in use.
- Manually saved Spanish words match prefixes as optional choices.
- No network permissions, telemetry, passwords, names or address-editor learning.
- Public EN/ES word support does not yet represent a full Spanish next-word prediction upgrade.
- User-initiated backup exports are plaintext and should be protected.

Validation: Java tests and Android compiler gates. On-device tests required before any release.
