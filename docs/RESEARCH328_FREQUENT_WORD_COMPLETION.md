# Research 328 — opt-in learning for early word completions

User's three screenshots illustrate repeated spelling of an ordinary word
such as "continue" without it appearing quickly after "con" or "cont".
The previous model learned corrections and word pairs, not a global
frequency for words completed in arbitrary approved contexts.

New local bounded frequency counts record a public or hand-saved word ONLY
after a successful composing-word Space commit or a confirmed suggestion
tap. On two normal uses, "continue" can be suggested after "co", "con",
or "cont". "go"/"hi" can be learned too, with a five-use requirement for
a single-letter prefix. Suggestions remain TAP ONLY. Exact known words
stay intact and untyped text is never autocorrected.

One accepted completion is weighted two and is not misclassified as an
incorrect typo mapping (e.g. con -> continue). Learning toggle OFF blocks
reads/writes; Clear removes the new per-word history. At most 160 words,
each bounded to 1000; no ambient logging, network or private field storage.

**Backup note:** This new frequency store is not yet included in the V2
backup format. A separate audited V2 extension is needed to transfer it
between trial packages. Research branch only; no new APK.
