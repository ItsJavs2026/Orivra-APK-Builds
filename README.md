# Orivra Build 286 — Clean Public Trial

This is a reviewed, clean snapshot of the private Build 286 candidate branch, NOT a fork or mirror of its history. It includes 36 Android Java source files, 13 selected synthetic regression tests, required Android resources, and the attributed SwiftFloris English bigram seed.

**WARNING:** Public GitHub upload makes every included source and asset accessible to others. No private signing key, production credentials, research dataset, screenshots, personal typing, private CI, or full Git history is included.

## Upload safely

1. Extract this ZIP and open the `Orivra_Build286_Public_Trial` directory.
2. Open the public GitHub repository `ItsJavs2026/Orivra-APK-Builds`.
3. Use **Add file > Upload files** and upload the **contents** of the extracted folder, preserving `.github/workflows/build-public-trial.yml` and all directories. GitHub's web upload can have trouble with nested folders/overwriting existing files. If it cannot preserve the structure, use GitHub Desktop or Git from a computer; do not upload a single ZIP as a repo file.
4. Existing public repo contains older workflow/build files. Replace them with the files in this package. Check the GitHub diff before committing. Do not delete `PUBLICATION_GATE.md` or other safety docs unnecessarily.
5. Go to Actions > **Orivra Build 286 Public Trial - manual qualification** > Run workflow.
6. Only if CI is green, inspect APK ID/signature/permissions and download the artifact for separate device testing.

## Safety and signing

Android package ID: `com.chroomareborn.keyboard.publictrial` (not `.candidate` or `.stablepreview`). Debug APKs from separate hosted runs may be signed with different ephemeral keys, so updates may require uninstalling *only the publictrial test app*. Never uninstall the working Orivra Candidate or Preview #178 to install this.

**Status: Source package integrity verified, Android compilation NOT verified, Samsung Android 17 NOT verified.** No claim of full legal clearance or absence of bugs is made. See `PUBLICATION_REVIEW.md`.
