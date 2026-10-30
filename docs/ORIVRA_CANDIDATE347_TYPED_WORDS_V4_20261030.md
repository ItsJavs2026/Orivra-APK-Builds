# Orivra Candidate 347 — verified typed vocabulary + one-file V4 backup

**Baseline:** Candidate 345 (Galaxy S26 Ultra user-reported smoother normal prediction strip). Research 346 passed **120/120 Android 35 emulator tests** before this integration. Candidate 347 is a separately installed trial; it does NOT overwrite Candidate 345, its V3 files or any installed keyboard.

## What changed
- Verified `Space` boundaries from real Orivra key taps now feed a **serial background worker**, only after Android reports a successful commit. Clipboard text, paste, unverified words, cursor edits, secure/numeric editors and failed commits are excluded.
- **Two independent gates:** existing **Learning: ON**, and new **Remember words I type: ON** (OFF by default, separate explicit dialog). Both are required. Reopen the text field after changing either gate.
- Bounded private evidence includes short words `i` / `a`, novel words `continue` and pairs `to continue`. Counts 1–2 stay pending, and only evidence 3+ can become a tappable suggestion. Never auto-correct or insert a word without a tap.
- Disk transactions happen on the worker, not per keystroke. The input thread reads immutable bounded snapshots for suggestion display. The previous Candidate 345 normal-strip flicker fix, automatic Rise, split predictions, cursor/Backspace fixes and existing sentence prediction remain.
- One **V4** plaintext document includes the V1 personal dictionary/corrections, V2 contexts, V3 frequencies **and all verified typed evidence (including pending counts of 1–2)**. V1–V3 export/import options remain for old builds. V4 import accepts V1–V4 input, validates before mutation and merges by MAX, never additive inflation. Permission ON/OFF is unchanged by importing.
- Explicit V4 import/export is **user selected**, without automatic backup, cloud sync, network permission or broad storage permission. The Android app disables Auto Backup. V4 may contain names and personal words; treat the plaintext file as sensitive.

## How to test safely on phone
1. Keep Candidate 345 installed for rollback; install the separate Candidate 347 APK and select its keyboard.
2. If you need earlier learned words on 347, **export V3 from Candidate 345** and choose **Import V4 (or earlier V1/V2/V3)** inside Candidate 347. Different trial package IDs cannot share private app data automatically. Keep that older V3 file separately.
3. On Candidate 347 settings, turn existing **Learning ON**, then enable **Remember words I type** in its separate confirmation. Reopen the text field.
4. In an ordinary non-private test note, type `I need to continue ` (with final Space) in three separate sessions. Open a new field and check whether `cont` offers `continue`, and whether `to ` produces `continue` as a next-word candidate. Suggestions remain tap-only.
5. Test rapid typing/Backspace and automatic Rise against Candidate 345. Turn memory OFF to stop recording; use **Turn OFF and erase typed-word memory** for explicit deletion.
6. Export a single V4 backup and verify importing it into a fresh Candidate 347 installation can restore the counts. A real second-install restore is not proven by emulator tests alone.

## Known limitations
- **Opt-in**, English alphabetic words and approved public contractions only; not Spanish yet.
- It counts verified Space commits, **not** all text the user enters through other keyboards or via paste. Sentence/phrase context is word-pair based; trigrams remain transient.
- Each independent trial install has its own private data. Export/import rather than copying Android app storage.
- Multiple preference files in legacy V1–V3 import are not one filesystem-atomic transaction. The importer preflights all data, but a rare write failure may leave a partial import. Keep the original backup and retry only after checking.
- V4 is newer than Candidate 345's V3 importer. **Do not import V4 into Candidate 345**; keep V3 rollback.

## Qualification
New Candidate 347 Android tests cover V4 pending evidence, corruption, legacy compatibility, repeat imports, consent generation, verified Space boundaries, private fields and rejected edits. CI must run the previous 120 tests plus at least 16 new tests, with 0 failures/errors/skips before publishing any APK.
