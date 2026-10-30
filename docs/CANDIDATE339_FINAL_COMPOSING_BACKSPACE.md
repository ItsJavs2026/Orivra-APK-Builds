# Candidate 339 — fix reproduced final composing Backspace bug

Based solely on pinned Research 338 commit ce6da11dbc404c54da3055761b56d382d35e8639 and the isolated
app metadata edits required for a separate Android trial package.
No live Candidate 333 data or preferences are modified.

Source-confirmed Candidate 333 defect:
  Backspace at final composing letter called finishComposingText(), which
  does not delete the character; another press was required. During the
  intermediate composing-vs-committed state the prediction strip could
  incorrectly show a no-predictions/keep-typing message.

Candidate 339 calls setComposingText("",1) to delete the character.
A checked fallback handles special editors that reject empty composing
replacement, without double delete. During the editor mutation,
reentrant selection callbacks do not prematurely reset composition.

New Java and Android BaseInputConnection tests reproduce and protect
the asking -> a -> empty screenshot example. Retains the short-word
learning, accepted sentence batching, contractions, bank keypad, and
V1/V2/V3 backups. Does not require learning reset.

Separate trial: com.chroomareborn.keyboard.build339trial,
versionCode 339, no INTERNET permission. Do not install before CI
source equivalence, Android 35 emulator, signature and SHA256 pass.
The user must still validate on Galaxy S26 Ultra Android 17.
