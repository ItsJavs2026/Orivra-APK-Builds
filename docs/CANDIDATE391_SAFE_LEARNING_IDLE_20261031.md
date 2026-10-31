# Orivra Candidate 391 — Safe V4 Learning Migration + Next-Word Letter-Row Gestures

Build from Research 391 combined head `35e9eb02f8db7d527f53881d9a05e0c72d4b5405`. Separately installable debug-signed APK with `com.chroomareborn.keyboard.build391trial` and `versionCode=391`. **Does not overwrite Candidate 378 or Candidate 386, and does not automatically access either app's private learning.**

## Why this build exists

The user's verified V4 backups from Candidate 378 and Candidate 386 have union capacities of **304 corrections / 242 contextual records / 326 word frequencies**, exceeding the old **300/240/320** caps but under the new coordinated limits **384/320/400**. Research 390 passed **250/250 Android 35 tests**, including synthesized exact-union imports both ways, idempotency, no discarded entries, preservation of maximum per-key counts, typed vocabulary and typed pairs, and overflow rejection without changes.

Research 388 separately passed **255/255 Android 35 tests**, adding center-only and idle-next-word letter-row swipes (`I | The | Hey` after Space), protecting the live InputConnection/caret/session. Candidate 391 ships these two changes together only after full integrated CI gate >=261 Android tests plus 200k bounded capacity stress, 130k letter-row gestures and 80k idle next-word cases.

## User safety-critical instructions

- **Keep Candidate 378 and Candidate 386 installed**, and keep their original SHA-256-verified V4 backups unchanged. Do not reset app storage or choose erase-memory.
- Before installing 391, export a NEW V4 from Candidate 386 with a distinct filename and keep the original 378 V4. Check both with the already-created offline checker. Backups are private plaintext, so keep them somewhere trusted.
- Install Candidate 391 **alongside** the other two. Do not assume learning migrates automatically. In 391 settings, select **Import V4**, choose the 386 file, verify preview and explicitly confirm. Then import the 378 V4, preview and confirm. If a step fails, do not delete or edit either original.
- Export a NEW combined V4 from Candidate 391. SHA-256 should pass. Expected aggregate union (per the two source files): dictionary=1, corrections=304, context records=242, frequency=326, typed vocabulary=58, typed pairs=30. Read-only comparator created for old Candidate 386 may mark 304/242/326 as beyond the **old** 300/240/320 caps even though Candidate 391 accepts them. The merged file may not import backward into Candidate 386.
- Reenable **Swipe predictions from letter rows → ASDF middle row** manually in 391 settings if desired. All separate-app preferences start fresh/OFF. Test DOWN on idle `I | The | Hey` and on tiny words, confirming one appropriate trailing space, no Send, no unintended keypress or two-thumb regression.
- Do not consider real-phone migration confirmed until the user reports successful imports and verifies the combined export. Android emulation, static checks and synthetic records are NOT a read of user private files.

## Limits and privacy

V4 checksum/UTF-8 LF/100KB maximum, typed vocabulary/pair ceilings (192/256), and per-record 1000 cap remain unchanged. A successful merge chooses MAX count for overlapping keys, never SUM. Corrections, contexts and frequencies have coordinated bounded ceilings of 384/320/400 across import and live learning. No Internet permission, telemetry, public backup access, automatic app-to-app transfer or silent memory clearing.
