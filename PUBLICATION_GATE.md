# Orivra public builder — publication gate

**Status: no private source published yet.**

Private source of truth: `ItsJavs2026/Chrooma-Reborn`. This repository must never copy its commit history or unreviewed research files.

Before publishing application source:

1. Review every selected file for secrets, user-specific information and third-party redistribution rights.
2. Include only build-required code/assets, proper attribution and minimally required tests. Exclude private research history and any signing keys.
3. Give this public trial its own Android application ID, distinct from `.candidate` and `.stablepreview`.
4. Use manual-only GitHub Actions with minimal permissions, no secrets and no production signing credentials.
5. Compile, verify APK identity and debug signing, and run appropriate safety/regression tests. A successful CI build does not establish Samsung Android 17 functionality.
6. Do not uninstall the existing Candidate or Preview #178 to work around signature errors.

The private project is not yet published to this repository; this file is a publication safeguard only.
