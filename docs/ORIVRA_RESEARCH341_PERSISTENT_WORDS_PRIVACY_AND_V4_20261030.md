# Orivra Research 341 — durable tapped-word learning without breaking Candidate 340

## Position at checkpoint — October 30, 2026

**User installed Candidate 340 on Samsung Galaxy S26 Ultra / Android 17.**
The user will report real-world results; no phone fixes have been verified yet.

**Candidate 340** is the installed/unchanged trial package
`com.chroomareborn.keyboard.build340trial`, published from
[`candidate-340-20261030`](https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/tag/candidate-340-20261030).
It passed **91/91 Android 35 emulator tests** on [run 41089548676](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41089548676).

**Research 341** is isolated on
`research/build341-durable-tap-learning-20261030` from the precise passing
Research 340 baseline commit `141b6275df50bd8853e9a47503387862b3a4e37e`.
No published APK, stable source branch, existing V3 exporter/importer, Android
manifest, live InputMethodService or previous package identity were modified.

## Why persistence requires a new design

Candidate 340's `PersonalNextWordModel` learns short word-to-word transitions
from ordinary individually tapped words, with two successful observations
before suggestion promotion. It *clears on editor change*, so it cannot
remember unfamiliar words across sessions or include them in V3.

The existing V3 (`FrequencyBackupFormat330`) explicitly permits only bundled
public dictionary words or manually saved words. The Android V3 importer does
not accept unknown new-word records; attaching unapproved records to V3 would
break integrity and older-version compatibility.

The research design is **one future V4 all-in-one manual backup**, *not*
requiring the user to export four separate backup files. The V4 codec
decodes legacy V1, V2 and V3 imports and preserves all prior V3 content.
Older Candidate 340 cannot import V4; the V3 backup must remain available for
rollback until a future release is qualified.

## Files introduced

- `TypedSequenceArchive341.java`: bounded, opt-in evidence of single words
  (max 192) and adjacent word pairs (max 256). The minimum evidence to qualify
  for future durable storage is **three independently committed Space events**.
  Repeated imports merge counts by maximum, never additive.
- `TypedLearningV4Codec341.java`: local SHA-256-checksummed V4 text backup
  built upon verified V3 records, with only canonical `VOC` and `NXT`
  entries. Strict validation, duplicate detection, bounded counts, and
  rejection of orphaned pairs whose words lack repeated evidence.
- `research/java/com/chroomareborn/keyboard/TypedLearning341SelfTest.java`:
  framework-free Java regression suite for consent, private-editor gating,
  recurring short words including `I` and `a`, format roundtrips,
  legacy imports, corrupted checksums, capacities and atomic merge behavior.
- `.github/workflows/research341-durable-learning.yml`: verifies exact
  Research 340's production files are unchanged, compiles and runs Java tests.

## Privacy and consent constraints

A genuinely new word can be a person's name, an address fragment, a
private term or part of a secret **even when it contains only letters**.
Lexical filtering alone cannot guarantee it is harmless. Therefore:

1. Keep the existing Learning switch, *plus* introduce a distinct
   default-**OFF**, explicit "Remember typed words across sessions" preference
   before ANY Android persistence wiring.
2. The input method must pass only words verified as typed by Orivra and
   committed at real Space boundaries. Never learn from pasted, selected,
   externally changed, numeric, password, URL, email, search-private or
   editor-disallowed text.
3. Reset adjacent-word context on punctuation, editor switch, selection edits,
   backspace interruptions and toggling either relevant consent switch OFF.
   Do not join words across apps, editors or private transitions.
4. Store at most one normalized token and at most one pair per event, never raw
   sentences, cursor context windows, app IDs, clipboard or timestamps. Three
   successful observations precede promotion, and suggestion taps must remain
   explicit, never automatic.
5. Provide visible counts, an inspect/delete interface, "Clear typed vocabulary"
   and a way to opt out. Export/import only through a user-selected document
   URI; V4 backups may contain personally identifying tokens and **should be
   handled as sensitive files**.
6. Keep disk writes off the key-tap critical path and limit I/O using bounded,
   batched storage. A SHA-256 file checksum detects accidents, not
   intentionally forged files; validate imported content before mutation.
7. Treat V4 as **not released/not installed/not active** until Samsung handset
   review and extensive emulator/privacy/backup tests qualify a separate APK.

## Qualification

The pure-Java research workflow
[run 41094730950](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/41094730950)
passed **53/53 assertions** with verified no changes to live 340, V3 or
app manifest.

This is research qualification, **not** Android integration or user phone
validation. No separate default-OFF Android switch or persistent storage
has been implemented yet.

## Next implementation milestones

- Collect user's Candidate 340 observations before changing composition,
  Backspace or suggestion behavior.
- Add Android consent UI, a bounded lazy-loading on-device store, test
  isolation/private editor gates and clearing; keep it OFF by default.
- Design a single V4 export/import path with preflight, merge preview and
  true rollback behavior; preserve the existing V3 route until tested.
- Add Android 35 emulator instrumentation and long-text timing regression
  batches before creating a separate Candidate 341/342 trial package.
