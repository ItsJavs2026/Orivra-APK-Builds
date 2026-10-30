# Build 327 — keep V1 and V2 export files visibly separate

The original V1 Android document creation default filename remains:
orivra-local-learning-backup.txt

The new explicit V2 context-learning export proposes:
orivra-context-learning-v2-backup.txt

Both use the Storage Access Framework's user-chosen destination. No
automatic export or overwrite. Source and Android emulator regression
tests verify V1 behavior was preserved, the distinct V2 filename is
visible in the picker, imports support both versions, private field
learning stays restricted and no INTERNET permission is requested.

This is a research branch only; Candidate 319 is unchanged.
