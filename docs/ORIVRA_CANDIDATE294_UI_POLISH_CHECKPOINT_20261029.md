# Orivra Candidate 294 — UI polish and emulator checkpoint

**Date:** 2026-10-29  
**Development branch:** `candidate/build294-suggestion-visual-motion-20261029`  
**Last functional-source revision:** `053786091ef8ff41125dbbee77138f23692baa1b` (contrast fix)  
**Latest qualification workflow revision:** `bf62eaa16f71325608ea5c9d281ce3bb8f744bab` (no APK release)

## Scope and safeguards

This is a **source-only Candidate**. Do not replace Preview #178, Build 262, Build 291, the public Build 292 trial, or the published Candidate 293 Samsung acceptance APK. The isolated trial applicationId is `com.chroomareborn.keyboard.build293trial` until a deliberately versioned future installable Candidate. No permissions, networking, predictor scores, or learning-data storage were added in this pass.

## Current UI contract

- **Prediction strip:** full usable width. No palette button or second redundant settings button inside the prediction row. One existing right-side caret remains.
- **Single arrow:** short tap expands/collapses sentence predictions when settings are closed; hold about 500ms toggles keyboard settings; short tap closes settings when open. All interactions should preserve their touch target during dispatch.
- **Theme controls:** only within the fully collapsible two-row panel below predictions. Top row shows the selected theme and Dark/Light appearance; lower row holds 🎨 and the other settings. Collapsed theme panel must measure zero height.
- **Prediction typography:** clean Android sans-serif. The leading *recommended correction* uses sans-serif-medium and a subtly emphasized theme-derived chip. Identical uncorrected input is never marked as a confident correction. Long sentences remain readable in two lines; nothing is auto-accepted.
- **Color accessibility:** recommended chips subtly darken in light themes and brighten in dark themes. Preserve original theme text when it meets 4.5:1 contrast; otherwise switch to pure black or white, whichever provides more contrast. This is a legibility policy, not a linguistic correctness classifier.
- **Typing animations:** short, eased ~55ms key-down and ~95ms release, reversible visual scale only. Never change layout bounds or touch hitboxes. Android disabled/reduced-motion behavior takes precedence.
- **Backspace and Enter:** preserve whole-connected-word swipe (`likethis`, `heythere`), highlighted deletion snapshot parity, exact editor-session guards, and Safe Enter tap newline / intentional held SEND.

## Test evidence

- 18 pure-Java policy suites and 12 Android 35 instrumented tests passed for the earlier visual candidate: [qualified baseline](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40934671652).
- Real installed Android 35 keyboard UI — unified arrow, panel expansion/collapse, ordinary letter touch, full-word Backspace gesture — passed: [visual UI run](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40934824051).
- Latest **contrast-enhanced** Candidate includes 18 Java suites with additional 75k light/dark color scenarios, 100k motion scenarios, and two new Android tests covering all 8 palette modes in both appearances. See [latest CI qualification](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40936640145) for live status and actual outcome. **Do not infer a pass without inspecting the run result.**

## Next steps

1. Confirm the latest Java/Android qualification and inspect screenshots; correct only failures with grounded evidence.
2. Perform remaining device-specific Samsung Android 17 acceptance before releasing a new version. Do not claim an emulator reproduces Samsung One UI.
3. On acceptance, freeze the layout baseline and return main focus to SentenceFlow: multiword segmentation, multiple typos, English/Spanish casing, punctuation, smarter single-word suggestions, next-word predictions, and regression-based accuracy measurements.
4. Add foldable/tablet screen-size and orientation testing when additional physical devices are available.

**Never fabricate emulator screenshots or create sandbox links for unmaterialized files.** Use exact GitHub Actions artifact links and actual verified outputs.
