# Research 387 — Center-only letter-row DOWN gesture

Based on **Candidate 386 released and positively phone-tested** (independently installable public prerelease `candidate-386-20261030`, exact commit `7cf2a9d35aa7d5a4b85b02c326e1af9387236256`). This branch is **research-only** and does not update, replace or publish a phone APK.

## Confirmed source-level gap

In Candidate 386, the alphabet-key ACTION_DOWN only initialized `LetterRowPredictionSwipe386` when the **left** or **right** prediction was valid. It did not check the **center** suggestion, even though DOWN chooses only center. When the normal three-slot strip was `["", "a", ""]` or `["", "te", ""]`, pressing a letter key could never start the DOWN recognizer. The user's short-word and single-letter completion requirement was thus not guaranteed in this real UI situation. Prior tests checked the chooser and word boundary but not the actual gesture-initialization gate.

## Corrective experiment

Introduce `LetterRowPredictionSwipe386.hasSelectable(visible)` that also checks the DOWN-center candidate, then use it in the existing alphabet-key initialization gate. No other touch thresholds, gesture direction, pointer order, press timing, suggestion ranking, permission, learning, Space key behavior or keyboard geometry changes.

Add pure Java randomized **30,000 center-only** DOWN sequences across one-letter and two-letter center words, plus the existing 100,000 gesture sequences and single-commit Space checks. Add **five** Android 35 instrumentation tests for center-only one/two-letter words, punctuation/whitespace no-double-space, empty center fail-closed, and short or canceled DOWN fail-closed. Existing Candidate 386 suite: 244 Android tests; target **249**, zero failures, errors or skips.

## Privacy and release gate

This branch runs tests only and never publishes an APK. `com.chroomareborn.keyboard.build386trial` remains unchanged by this research branch. Its app still has no INTERNET permission. Keep Candidate 386 as the phone-tested baseline until there is a specific reason for a successor trial and it passes independent full qualification. ASDF middle-row ergonomics are still based on user feedback, not measured lab telemetry.
