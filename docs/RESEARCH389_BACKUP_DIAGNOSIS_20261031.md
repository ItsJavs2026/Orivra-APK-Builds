# Research 389 — Safely diagnose Orivra V4 backup import failures

User's installed Candidate 386 showed a toast: **"Backup invalid, damaged, incompatible or over capacity."** They need to preserve long-term learning, not restart. No exported backup was supplied to development, so its actual failure category is **unknown**. This branch starts from released Candidate 386 commit `7cf2a9d35aa7d5a4b85b02c326e1af9387236256` and is NOT a release, migration or repair of uninspected private data.

The existing MainActivity catches file-reading, format/checksum decoding and capacity/local-merge preflight exceptions together and shows the same generic toast. All are rejected before writes in the preview/import path. This research adds an independent **"Check my V4/V3 backup safely (read-only)"** Android document-picker control. It reads ONLY a user-selected local document on the settings IO executor, runs `TypedLearningV4Codec341.decode` and `TypedLearningV4Transfer347.preview`, then displays **stage-specific sanitized results**:
- Cannot read / invalid UTF-8 / over 100KB: document problem
- Unsupported file type/header (e.g., APK/ZIP/unrelated file)
- Integrity checksum/changed line endings
- Recognized but inconsistent Orivra records
- File valid but cannot merge with current store (capacity or current evidence conflict)
- Complete validity: only counts of words, corrections, context, frequencies, verified typed words/pairs, not the actual values

No import, erase, preference mutation, automatic export, file rewriting, network, telemetry, sensitive words, filenames/URI or raw exception message shown. The installed Candidate 386 and prior V3/V4 files remain untouched. A future phone trial must still receive complete Android 35 CI qualification before installation.

## Recovery advice for current Candidate 386

- Do not uninstall, clear app data or tap **Turn OFF and erase typed-word memory**.
- Keep original V3/V4 documents unchanged. V4 file may contain names and private words; do not upload the entire plaintext backup unnecessarily.
- If previous learning lives in Candidate 378, open 378 and export a **new uniquely named V4** file; verify the success toast before attempting to import in 386. App IDs isolate each candidate's learning, so merely installing 386 doesn't transfer 378's data.
- Current importer is strict UTF-8/LF with SHA-256, max 100KB, and may reject capacity conflicts. Don't repair by deleting lines, editing the file or altering whitespace/checksum.
- To narrow down the cause with the existing phone, ask only for **which Candidate exported the file**, **file size**, and **first header line** (e.g. `ORIVRA_LOCAL_LEARNING 4`), not the private word records.

## Test gates

Android instrumentation must verify valid V4 + V3 read-only preflight and no preference mutations, invalid header, damaged SHA256, CRLF, and safe read failure classifications. Candidate 386's 244 Android tests remain mandatory; target >=250 tests. The research workflow MUST NOT publish an APK.
