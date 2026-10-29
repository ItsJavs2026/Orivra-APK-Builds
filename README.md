# Orivra — Public APK Build Channel

This repository is a **clean public build channel**, separate from the private `Chrooma-Reborn` development repository.

**Current status:** infrastructure only. No Orivra source has been published here, and no APK from this repository has been built or approved.

## What is here
- Manual-only GitHub Actions workflow: [Build Public Trial](../../actions/workflows/build-public-trial.yml)
- Android Gradle 8.7.3/Gradle 8.9 configuration
- Distinct trial Android package ID: `com.chroomareborn.keyboard.publictrial`
- [Publication safeguards](PUBLICATION_GATE.md) and [source approval checklist](docs/PUBLIC_SOURCE_APPROVAL.md)

## Source publication boundary
The public build will eventually include **reviewed, build-required source files only**. They will be visible to everyone. It will never import or fork the private Git history or copy sensitive keys, user screenshots/typing, or experimental third-party datasets. Third-party license/NOTICE obligations must be preserved.

## Install safety
The public trial app has a separate package ID and cannot replace the existing `.candidate` or `.stablepreview` versions. GitHub-hosted debug keys are disposable; APK signing identities can change between public runs. Never uninstall a working Candidate or Preview #178 just to force an APK update.

Do not use a prototype keyboard for credentials or other sensitive entries. An Android build success does not prove phone behavior.

## Next step
Review and approve the *specific source snapshot* for publication, then run the first manual Actions build and inspect its logs, APK identity and signing certificate.
