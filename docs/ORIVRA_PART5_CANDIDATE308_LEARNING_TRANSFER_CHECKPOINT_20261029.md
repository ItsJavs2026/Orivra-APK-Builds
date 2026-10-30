# Orivra Part 5 — Candidate 308 Learning V2.1 and optional learning portability

Date: 2026-10-29 (America/Chicago). Isolated test app, **not production stable**; do not replace existing working keyboard. Current user-confirmed Samsung Galaxy S26 Ultra Android 17 **Candidate 304** centers idle next-word chips correctly; prior **Candidate 300** was confirmed to fix Google Keep predictions. Separate **Candidate 305** with contraction support is CI-qualified but user installation/approval is unknown.

## Candidate 308 source / intended implementation
Created from Android-35-qualified Build 307 `research/build307-explicit-learning-portability-20261029` at SHA `486d51fed0da7166d2f39d459c266cb1d92daf89`. 307's full Android 35 research run passed **35/35** tests: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40989781829.

Branch `candidate/build308-learning-portability-trial-20261029`. Preserve all qualified source byte-for-byte except the app's isolated package metadata, launcher instructions, latency diagnostics package gate and app package test. `applicationId=com.chroomareborn.keyboard.build308trial`, versionCode 308, versionName `1.0.25-build308-learning-export-import`, launcher label `Orivra Candidate 308`. The old package is never overwritten.

New features relative to phone-confirmed 304:
- Existing Candidate 305 straight/curly apostrophe word composition and tappable can't / ain't / don't / we're corrections, optional local Learning V2, V1-compatible saved correction key schema (in **this** Android package only).
- Build 306 deterministic Learning V2.1 conflict-handling: ties between equally accepted corrections do not arbitrarily pick a winner, uppercase and first-person contractions preserved. Native research qualification **32/32** tests: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40988582233.
- Build 307 explicit `Export my saved words + learned corrections` and `Import previous Orivra learning backup` via Android's Storage Access Framework document picker. File is **plain text**, up to 100KB, with integrity checksum, 250 manual words and 300 accepted pairs, all validated before import. Preview with counts and confirmation, repeat imports merge using max counts instead of sum. Learning remains **OFF unless separately enabled**. No raw notes, passwords, whole keystroke history, blanket filesystem permissions or INTERNET permission in the app.
- Retains 304 centered idle prediction chips, fixed caret, Keep predictions, Safe Enter/Backspace and 303 optional offline English vocabulary (OFF by default).

## Important explicit limitations
- Android isolates each `applicationId`. Candidate 308 **cannot silently read or import** saved words or learned correction history from Candidate 304/305. They lack this new export control. Don't suggest an impossible automatic migration.
- Backup encryption is NOT provided; it may expose manually entered names or corrected words. The user chooses a trusted file destination/provider; SHA256 detects accidental corruption, **not forged or malicious files**.
- Learning is disabled for protected editor types, and Google Keep `NO_SUGGESTIONS` fields may show predictions while intentionally **not learning**.
- `LearningTransferStore` imports into two private SharedPreferences namespaces sequentially; no cross-preference transaction is guaranteed if storage writes fail. Avoid claiming atomic crash-safe imports. Normal prevalidation and capacity checks happen before writing.
- Running an Android emulator is not physical Samsung acceptance. Future debug signatures may change per run; an upgrade might require uninstalling **only this isolated trial**.

## Release quality gate
The new workflow `.github/workflows/candidate308-learning-portability-apk.yml` pins the exact Build 307 source, diffs every protected main Java, test and manifest file against it and allows only listed package and title differences. Repeats original 89 contraction checks, 266 Learning V2.1 policy checks and 46 transfer-codec checks (total 401). Rebuilds licensed pinned ESDB asset, verifies no INTERNET permission and correct package ID, runs all 35 existing Android 35 emulator instrumentation tests, checks APK debug signer, manifest version, asset/license and SHA256, and only then uploads an APK/artifact and creates a public GitHub **prerelease**. No stable app overwrite.

GitHub CI run: https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40991044687 . **Check final conclusion before claiming release available.** Expected tag `candidate-308-20261029` and downloadable APK `Orivra-Candidate-308.apk`. Do not make up a checksum.

## Physical Samsung acceptance checklist
1. Install Candidate 308 alongside the existing working Candidate 304, not over it.
2. Enable Candidate 308 input method. In a disposable Keep or Samsung Notes test field, verify centered idle next words; type and clear, watch for caret not moving.
3. Type `can't`, `can’t`, `ain't`, `don't`, `I'm`, `we're`; check apostrophe stays inside one composing word and a candidate selection works; type `were` and `its` to verify no unwanted silent replacement.
4. In Orivra app settings, turn Learning V2 ON if wanted, then use **synthetic** corrections only. Review learned word pairs and counts. Do not use real private content, banking/email/password input.
5. Save a harmless custom word (e.g. `Testword`); use **Export** to a trusted local file, then **Import** the exact same file. Confirm the preview and confirm repeated import does not duplicate or inflate learning counts. Learning ON/OFF state must stay unchanged.
6. Recheck Google Keep predictions, Backspace hold, Safe Enter in a multiline note and responsive suggestions. Revert to Candidate 304 if any issue.

## Further work
Treat both accuracy and backup transfer as ongoing research. After physical acceptance, add optional preview of exact word-list changes before committing, stronger error reporting and two-preference rollback; keep improving multi-typo SentenceFlow/English-Spanish offline routing in separate research branches. Do not call any research result a phone-confirmed stable version without the user's explicit report.
