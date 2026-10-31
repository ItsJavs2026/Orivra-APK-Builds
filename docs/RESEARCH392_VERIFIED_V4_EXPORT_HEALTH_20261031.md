# Research 392 — V4 export read-back verification and learning health

Based directly on positively phone-installed Candidate 391 commit `133d1c2cbe3da72d2b6739ee2b24bcead1f1e78e`. **Research-only, no APK release.** The user reports that the 378 → 386 learning import works in 391, but actual *exact merged V4 verification* has not yet been confirmed. Do not claim otherwise. Candidate 391 and both source backups are untouched.

## Findings and narrow changes

Current Settings V4 export writes a selected Storage Access Framework document and reports "saved" if `OutputStream.write` returns successfully. A provider could still persist different bytes or make the document unreadable, and the result could be mistaken for a validated backup. Research 392 adds:
- A **post-write read-back** of the same user-selected `Uri` on the existing Settings IO worker, strict UTF-8/LF, byte-for-byte string equality, and V4 codec round trip/checksum decode. No file rereads except the selected export. Distinct user messages for VERIFIED, written but provider prevented verification, damaged/different bytes, and failed write. A read failure never erases anything and never implies existing learning is damaged.
- An opt-in **Check saved learning health (read-only)** control under the migration guidance. It exports V4 **in memory**, decodes and counts seven categories (dictionary/corrections/context/frequency/typed words/typed pairs/file size) with their *actual Candidate 391* capacities 250/384/320/400/192/256/100000. A conservative 80% advisory warns to keep older backups. No words or paths shown.
- No change to IME prediction, gestures, data store, imported files, import preflight/consent, caps, signing or app package. No Internet permission.

## Tests needed before new candidate

One empty-app V4 export to an isolated Android app-private file, exact read-back and SHA validity; deliberately altered saved file detected without erasing learning; an unreadable file not mislabeled as bad data; aggregate only health on empty and seeded fake preferences with no leakage; limits/80% deterministic checks; no mutation or network; full >=267 Android tests plus all prior gesture and 200k-capacity suites. Keep separate previous Candidates and V4 backups installed/saved. **Do not issue a new APK until full CI success and a material reason to phone-test.**
