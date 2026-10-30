# Orivra Research 314 — English missing apostrophes inside joined sentences

**Real-device starting point:** Candidate Build 310, Samsung Galaxy S26 Ultra Android 17. A Build 310 screenshot showed typed `Yesthats` and candidates `Yesthats`, `Yes thats`, `Yes that`. User intent was `Yes that's`.

## Fix in this research branch

The SentenceFlow decoder must first find the correct spacing with exactly
the same letters. A small post-decoding policy can then offer an *additional*
candidate with the final unambiguous English apostrophe inserted, e.g.
`Yesthats` -> `Yes thats` -> optional `Yes that's`.

The policy does not invent letters, alter boundaries, change the top ranked
base suggestion, or auto-commit. Ambiguous `were / we're`, `lets / let's`
and `hows / how's` are excluded from this specific shortcut; the existing
single-word tap-only paths remain unchanged. This preserves word protection
and avoids widespread grammar guesses.

## Quality

- Unit cases: screenshot, positive and negative multiword reconstructions.
- Add user screenshot as strict end-to-end `PredictionSmokeTest` regression.
- Re-run 487 synthetic English segmentation and typo cases.
- Android compile and Android 35 emulator regression tests.
- Do not release APK until automated qualification and Samsung review.
