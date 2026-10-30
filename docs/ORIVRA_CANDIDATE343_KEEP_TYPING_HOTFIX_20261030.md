# Candidate 343 — Keep typing placeholder hotfix, Rise preserved

Priority: user reports severe lag and constant switching from real suggestions
to the **"Keep typing"** placeholder while typing on Samsung Galaxy S26 Ultra
Android 17 with Candidate 340.

Candidate 342 was released but mistakenly disabled automatic Rise expansion;
**do not recommend Candidate 342**. Candidate 343 supersedes it by restoring
the exact Candidate 340 auto Rise and split-mode next-word code.

Candidate 343 uses separate app ID com.chroomareborn.keyboard.build343trial.
The normal empty suggestion strip is QUIET: no "Keep typing" or "NW OFF"
placeholders, while private/suggestions-disabled editor status is preserved.
It renders the literal current composing word immediately while slower
candidates are calculated off-thread, and ignores only tightly guarded exact
duplicate cursor echoes (not real caret movement or selection).

V1/V2/V3, Backspace, existing learned habits, Android manifest, optional
dictionary, automatic Rise and split mode are unchanged from Candidate 340.
Emulator qualification is not a Samsung performance verification.
