# Research Build 330 — portable frequent-word backup V3

The user's priority is that "continue" and common short words stop
requiring repeated full typing, and that their learning survives new APK
installs. Research 328/329 added evidence for full words; existing V2
backups do not carry those new per-word frequency counts.

This stage provides a distinct V3 codec WITHOUT changing:
- V1 saved words + accepted correction format (still readable/exportable)
- V2 accepted contextual word-pair counts (still readable/exportable)

V3 includes all V2 records plus bounded FRQ word/count entries. Each
file has a full SHA256 checksum; decoding rejects unrecognized records,
duplicate frequency entries, noncanonical words, counts outside 1-1000,
line/header tampering and oversized input. No raw sentences, private
editor text, keystroke history, network sync or automatic transfer.

Next stage: Android adapter to validate every imported target against
the existing public dictionary or manually saved word list, and merge
frequency counts idempotently without enabling Learning. After that,
offer a DISTINCT V3 export filename/UI option and Android emulator test.
Until qualification, Candidate 327 and its V1/V2 backup buttons are
untouched. Older candidates cannot import V3.
