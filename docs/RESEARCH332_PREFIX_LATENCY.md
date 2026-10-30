# Research 332 — keep frequent-word prediction taps responsive

Candidate 327 improved perceived touch smoothness. New prefix learning in
328/329 must not add noticeable latency to typing.

The matching() hot path formerly called approved() on every candidate.
For a manually saved candidate that triggers sorting/scanning up to 250
saved words, 320 frequency entries could have caused dozens of repeated
reads in one suggestion update. Now it checks known public words directly
and lazily builds a single bounded normalized set of hand-saved words on
the FIRST candidate requiring it.

No heuristic or frequency threshold changed. Existing 328/329 word
completion regression tests plus Android 35 emitter tests protect parity.

V1/V2/V3 backup implementation is inherited unchanged from 331.
Candidate 327 is unchanged and remains rollback-safe.
