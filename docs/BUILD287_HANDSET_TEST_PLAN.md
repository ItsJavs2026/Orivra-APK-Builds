# Orivra Build 287 — Visual/Haptics Handset Qualification

Development branch: `build287-visual-haptics-trial`. Based on known Build 286 public trial commit `0fb83d0`. This is an **experimental Android keyboard**, not a stable release. Do not merge with main or replace stable Preview #178.

## Intended source-level changes

- Gboard-inspired rounded, flatter keycaps and restrained press feedback, plus an original **Clean Neutral** light/dark theme selectable with one tap from the trial app; no copied third-party assets.
- Secondary number/symbol hints moved inside the letter keycaps so they do not clip above the rows.
- Haptic feedback profiles: OFF → LIGHT → STANDARD → CRISP → OFF. Select from the trial app settings or by long-pressing the toolbar collapse control. Existing system haptic policy remains respected, with no new vibration permission.
- Stop dimming the prediction strip while background predictions compute. Reuse suggestion cells rather than destroying and recreating the row on each update; remove animated Rise-panel fade and scaling from successful candidate taps. Former 'Rise' speed becomes a functional **Hold** duration for the panel's auto-dismiss, without fading.
- Separate application ID `com.chroomareborn.keyboard.build287trial`, to avoid collisions with Build 286's public trial and the stable private keyboard. Ephemeral GitHub Actions debug certificates are not suitable for seamless upgrades.

## Device test matrix — Galaxy S26 Ultra / Android 17

1. **Install:** Check package identity and signing, install Build 287 as a *separate app*; keep Preview #178 and Build 286 installed until evaluated. Verify Android's Input Method picker shows the distinct trial keyboard; do not type sensitive data during experiments.
2. **Visual:** Select **Apply Clean Neutral keyboard look**, then compare light/dark and adaptive themes in portrait and landscape at different keyboard heights/font scales. Letter and symbol hints must be INSIDE the keycaps and remain readable.
3. **Symbols:** Tap ?123 and both symbol pages; test single taps and long holds (Q→1, W→2, A→@, etc.). Ensure long-press inserts exactly one symbol and does not append the original letter. Test punctuation near spaces, cursor middle, and password fields.
4. **Predictions:** Type quickly then hold Backspace in a plain notes field. Suggestion chip backgrounds, row height and text should not flicker or bounce; no suggestion may commit without a tap. Exercise Split ON/OFF, NW ON/OFF, expanded panel, the functional Hold 400/600/800/1000 setting, correction acceptance, and fast cursor movement.
5. **Haptics:** Long-press toolbar collapse control to cycle OFF, LIGHT, STANDARD, CRISP. Compare letters, Backspace hold, long-press symbols and successful suggestion taps; confirm no double buzz or uncomfortably long effect. Verify system vibration-off overrides the app.
6. **Regression:** Re-run existing user dictionary, prediction, cursor gestures, learning, secure-field protections, and locale/shift tests; check no crashes, memory spikes, stuck pressed states or stale suggestion actions.

## Release gate

GitHub Actions compilation, Java regression tests and APK identity/signature/permissions verification must pass **before** handset installation. User screenshot testing is a separate gate and not implied by CI success. Any unexpected permissions, install identity mismatch, input-method crash or sensitive data behavior blocks promotion.
