# Orivra 417 — real pinned offline US-English wordpack; on-phone APK trial

Date: 2026-10-31 / 2026-11-01. Branch: `candidate/build417-offline-esdb-onphone-20261031`
Base: `hotfix/build416c-cursor-chip-reopen-20261031`
**Status:** source changes committed. This branch is *not* a prebuilt APK. No Android Code Studio Gradle log, installed APK, Samsung runtime memory/latency report or screenshot has been received. In-chat static source checks are not Android compile tests.

## Why this is a new candidate
The user wanted a fully populated offline dictionary rather than manually adding common words. Prior Builds 415/416 (and source 416C) contained a toggle for a 78,942-word ESDB exact-only pack but **no actual `.esdbpack` asset in Git**. The 416 phone exporter used `git archive`, omitting generated/untracked assets, so flipping the toggle could not activate the advertised dictionary. Now the 417 build script **generates the pack in the exported Android project before Gradle** and fails if it does not appear in the APK.

The user has not yet verified 416C on the phone. This isolated 417 trial inherits 416C's recently committed cursor-word suggestion-ticket, Backspace selected-range and preference-reload changes, with prediction-bar gesture default OFF and independent letter-row swipe intact. Do NOT call these phone fixes confirmed until user reports they work. Gboard and prior installed Orivra builds must remain available.

## Word source, integrity and licensing
Source: https://github.com/en-wl/wordlist (English Speller Database, previously SCOWL)
Pin: `1e5b7d3a72f47a71da5d28686c1dd4b397178485`
Generation: `./scowl word-list 60 A 1 --categories=` after running `make` in pinned checkout.
Expected raw UTF-8 list: exactly `1,022,018` bytes with SHA-256 `a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600`.
Expected accepted exact lowercase US English spellings: `78,942`. Deterministically gzip with `gzip -n -9`; compressed output typically ~280 KiB. The archive is `app/src/main/assets/dictionaries/esdb-en-US-60.esdbpack`.
License: full `app/src/main/assets/dictionaries/ESDB_COPYRIGHT.txt` from pinned source, including original Kevin Atkinson notice and applicable notices; verify source and packaged notice exist.

`scripts/build-esdb-wordpack.sh` now checks pinned commit, source+packaged copyright headers, expected original size and hash, gzip integrity and round-trip hash.

`scripts/build_orivra417_android_code_studio.sh` performs:
1. exact Git branch and known local Android Code Studio Gradle 8.9/AAPT2 wrapper checks; Python 3, SQLite 3.33+, `git/tar/gzip/sha256sum/wc/make/javac/java/unzip/cmp` preflight.
2. download pinned *public official* ESDB source once to `$HOME/.cache/orivra417-esdb-pinned`, or reuse cached verified checkout; the source is not uploaded anywhere. May use verified `ESDB_SOURCE_DIR` if offline.
3. export current tracked app source into a fresh temporary project (no overwrite of older APKs); generate wordpack **inside exported project's assets** and verify the decompressed SHA and byte count.
4. run existing 413–416C source Java regression runner, then compile and run `OfflinePackAsset417Test` reading the generated gzip, asserting 78,942 words, exact uncompressed count, common spellings, and rejection of corrupt gzip. Wordpack indexing is not done on a key event.
5. assemble Android debug APK locally (no GitHub Actions). Check output metadata's unique app ID/version; extract exact pack bytes from **APK zip** and compare SHA-256 to generated archive; byte-compare license from APK against source.
6. only after ALL checks succeed rename the temporary build project into the final new path. On failure, clean staging; leave old projects untouched.

If Python/SQLite/make are missing in the phone terminal, the script fails with a clear prerequisite rather than claiming an APK. Local Internet may be needed the first time to fetch the pinned public source. This is not a download/compile step performed on the user's phone remotely.

## New actual app
`applicationId=com.chroomareborn.keyboard.build417dictionarytrial`
`versionCode=4170`
`versionName=1.0.73-build417-offline-exact`
Label: `Orivra 417 Offline English Trial`.
Default new app preferences: offline exact dictionary opt-in **OFF**; prediction-bar swipes **OFF**; independent letter-row swipe mode defaults OFF until user enables their preferred mode.
`OfflineDictionaryStatus417` is process-local and has no stored typing. In settings, press "Refresh offline dictionary status" to see pack-header present/missing, expected count, and state NOT LOADED / LOADING / READY (actual verified count/load milliseconds) / FAILED (base dictionary fallback) / PAUSED (protected fields) / OFF.
`RebornImeService.reconcileOptionalDictionary()` loads only after explicit opt-in in an allowed editor; reevaluates when keyboard view reopens without an onStartInput event; preserves 416C privacy restrictions and worker callback identity check. `OfflineLanguagePackWorker` loads asynchronously at low priority and `CompressedLanguagePack` enforces max decompressed size, exact SHA and word count; always falls back on failure without applying unverified predictions.

## What "full" means (do not overclaim)
This pack activates exact spelling recognition and highly conservative split correction for ESDB words. It is **not a frequency-ranked 79k-word predictive vocabulary**, not an expanded fuzzy candidate search, and not a Spanish language pack. A full next-word and typo-aware bilingual model is a separate subsequent quality/performance phase, after 417 is stable and fast. Do not advertise an exact-only list as Gboard-level word prediction.

## Commands to try in Android Code Studio terminal after 416C phone test
```bash
cd ~
git clone --depth 1 --single-branch \
 --branch candidate/build417-offline-esdb-onphone-20261031 \
 git@github.com:ItsJavs2026/Orivra-APK-Builds.git Orivra-Dictionary417
bash ~/Orivra-Dictionary417/scripts/build_orivra417_android_code_studio.sh
```
If cloning this branch occurred *before* the final commits, update source before build:
```bash
cd ~/Orivra-Dictionary417
git pull --ff-only
bash scripts/build_orivra417_android_code_studio.sh
```
Only if **script finishes successfully**, output APK: `/storage/emulated/0/AndroidIDEProjects/Orivra417OfflineEnglish/app/build/outputs/apk/debug/app-debug.apk`. No claimed sandbox APK unless created in an actual runtime; GitHub is source only.

## Samsung test sequence
1. Install 417 as a **separate keyboard**. Do not remove 415, 416, 416C or Gboard. New package means no migrated per-app learned dictionary, haptic settings, selected letter-row swipe modes or preference backups.
2. Leave dictionary switch OFF initially. Test fast typing, notification quick reply with YouTube Music playing, ordinary Space and cursor movement, long highlighted text Backspace once, and selection correction by tapping a word under cursor after **hide/reopen**. These must work without dictionary loaded.
3. In settings turn ON **Experimental offline English dictionary**. Reopen a non-private text editor to trigger the background worker, then return to Settings and select **Refresh offline dictionary status**. Expect **READY 78,942 verified**. If FAILED or not loaded, do NOT assume pack worked; share exact status and build output.
4. Test previously unknown, correctly spelled words (exact recognition), and ensure repeated typo candidates are no worse than OFF; compare app responsiveness and aggregate audit with same other toggles. Password/private numeric or no-suggestions editors must not load or apply this pack. Check learned word persistence independently.
5. If app becomes sluggish, switch dictionary OFF, reopen keyboard and use 416C/Gboard while investigating. Never force the main thread to parse 79k words or assume a successful generator means good live typing speed.

## Next stages (not done)
A frequency-weighted next-word model, better typo candidate generation and a Unicode Spanish/English bilingual pack require their own provenance, offline RAM/latency budgets and real-world evaluation. Current per-user manual dictionary cap 250 is separate; do not silently alter backup/migration without tests.

## Live Android Code Studio troubleshooting — missing make
User cloned the 417 branch successfully on their Samsung; original script stopped immediately with `ERROR: Missing make in Android Code Studio terminal.` No APK was built and no installed keyboard was touched. Verified official pinned upstream Makefile's `scowl.db` rule invokes only `./combine.py create-db scowl.db`, a Python script. **Both** `scripts/build-esdb-wordpack.sh` and `scripts/build_orivra417_android_code_studio.sh` now avoid `make`: the generator runs `python3 combine.py create-db scowl.db` directly from the pinned upstream checkout. Expected byte-count/SHA check and APK verification are unchanged.

In already cloned 417 source, update first:
```bash
cd ~/Orivra-Dictionary417
git pull --ff-only
bash scripts/build_orivra417_android_code_studio.sh
```
Do not reclone or install make. If next preflight fails (Python/SQLite/tools) or generation fails, request the terminal's last lines and resolve next prerequisite without claiming a successful APK. Source change is not phone-tested yet.
