# Research 322 — portable contextual backup format prototype

IMPORTANT: Format experiment only, not enabled in Candidate 319 or the
live import/export feature. The user says existing V1 dictionary and learning
exports work well. This branch deliberately preserves that entire path.

A separate V2 codec can encode existing manually saved words, V1 correction
counts, and explicitly learned short two-word `CTX` count records.
V1 files can be read using the V2 prototype decoder. A full-file SHA256
checksum rejects tampering; whitelist left-context words, number limits,
token shapes and record caps reject malformed entries. Backups remain
plaintext and user-chosen. Future work: wire V2 import/export into
LearningTransferStore with atomic preview/confirm and idempotent count-merge,
while preserving the exact V1 compatibility tests.

There is no automatic transfer, network, password handling, or new permission.
