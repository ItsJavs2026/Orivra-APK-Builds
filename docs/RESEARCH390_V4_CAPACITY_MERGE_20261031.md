# Research 390 — Safe 378-to-386 V4 learning union (no release)

**User evidence:** Both SHA-256-verified 378 and 386 V4 files are valid; the local counts-only comparator reports **304 distinct accepted corrections, 242 context keys and 326 word-frequency keys** on union. Existing Candidate 386 ceilings are 300, 240 and 320 respectively. No plaintext learned words have been transferred to the repo or assistant.

## Scope and privacy

Based *directly* on installed positively phone-tested Candidate 386 commit `7cf2a9d35aa7d5a4b85b02c326e1af9387236256`. Never overwrite Candidate 378 or 386, never erase or truncate a backup, and never install an update over their existing data. On this research branch, change only the **static live-store and backup-codec capacities**, jointly controlled by a pure-Java policy: **384 corrections, 320 context preference keys, 400 frequency records**. No changes to prediction scoring, key gestures, InputMethodService, V4 record format, typed-word/pair limits, 100KB document maximum, per-record count ceiling of 1000, or private-editor/consent gates.

The import preflight must continue to reject any overflow **before mutating preferences**. Existing records merge by canonical key and per-record **MAX**, never SUM; retaining both files intact is essential until a separately installed migration build has exported and verified a complete new V4. Existing V4 files still decode; new V4 files larger than old ceilings may NOT import back into Candidate 386 (keep earlier backups for rollback).

## Robust qualification required

1. Pure Java assertions: all six live/codec limits share the same constants; exact union 304/242/326 fits; all original ceilings reject this union; limits remain finite and bounded; over-limit combinations fail closed.
2. One-file V4 end-to-end encode/decode after forming synthetic 304-correction, 242-context, 326-frequency union, including the correct 58 typed-word/30 next-word-pair bounds where practical. Validate SHA-256, record counts and no truncation. Verify old V4 backup remains readable without rewriting.
3. Android 35 isolated InputContext or clean app test: import a 386-type V4 and 378-type V4 sequentially, preserving all distinct records and maximum counts; a repeated import must be idempotent; no learned data is erased by a rejected overflow; preference consent remains unchanged.
4. Run full Candidate 386 regression (244 Android tests) plus new integration tests, no errors/skips/failures. CI never releases an APK from research.

## User recovery after independent release

Use a *different* Android package from Candidate 386 to keep existing phone learning untouched. Import the new 386 backup **first**, then the original 378 backup, confirm both, export a NEW V4 merge file, run the offline checker and compare its counts to **304/242/326**. Keep both original backups until confirmed. Installing a new package never migrates learning automatically.

**Known caveat:** the comparator's context-key count must be verified against the actual stored preference keys (`pair:` and `used:`) in the Android test; V4 CTX lines can combine two counts for one pair. Do not interpret a mere format checksum as a verified merge. No actual user data is read in research tests.
