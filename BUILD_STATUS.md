# Public Trial - Current Build Status

**Not build-ready yet.** The public repository has a Gradle project skeleton and manual Android workflow, but **no Orivra production Java source has been published**. The private source transfer was halted at the final publication gate. Do not run the APK workflow until source publication is complete, or expect an installable APK.

## Preserved boundaries
- Private source of truth remains `ItsJavs2026/Chrooma-Reborn`.
- Full private Git history, experimental corpora, third-party seed data and signing keys remain excluded.
- Planned public package is `com.chroomareborn.keyboard.publictrial`, distinct from existing Orivra Candidate and Preview #178.
- Proposed source-file inventory is documented in `docs/PROPOSED_SOURCE_INVENTORY.md`.

## Remaining work
1. Complete an explicitly approved and security-reviewed release of build-required source files to the public repository.
2. Check third-party licenses and attributions for any asset actually shipped; do not assume all datasets are redistributable.
3. Run the manual Android build and verify all Java checks, Gradle compilation, APK package ID, signing identity, and manifest permissions.
4. Test the keyboard on an Android device before recommending an install. Debug signing keys from Actions are ephemeral and should not be used for continuous upgrades without a deliberate signing plan.

**No APK was created by this public repository.**
