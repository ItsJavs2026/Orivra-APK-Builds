# Orivra: external keyboard engineering research — 2026-10-30

Status: design/analysis, NOT new APK or device validation. Candidate 354 is the user's positive Samsung S26 Ultra smoothness baseline. Do not modify its release or stable Preview #178.

## Primary authoritative references

1. **Android Developers — Creating an input method**  
   https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method  
   Lesson: respect InputMethodService lifecycle, EditorInfo capabilities, composition/selection, secure fields, and editor-controlled IME action behavior.  
   Orivra mapping: regression matrix for Google Keep, Search, numeric/bank inputs, selection edits, editor restarts; treat editor callbacks as asynchronous.

2. **Android API — InputConnection**  
   https://developer.android.com/reference/android/view/inputmethod/InputConnection  
   Lesson: setComposingText() replaces active composing span or selected text; editor operations can update selection asynchronously. Avoid unsafe delete + retry if editor state changes.  
   Orivra mapping: retain verified before/after caret tickets, single insertion, no accidental highlighted-word duplication and no second Backspace press.

3. **AOSP LatinIME — InputLogic.java**  
   https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/ecea8551c39a497e036be5c010d7ddb6b51a36bc/java/src/com/android/inputmethod/latin/inputlogic/InputLogic.java  
   Lesson: separate composition, suggestion commits, word-at-caret correction, selection/cursor checking, transaction logic and punctuation rules.  
   Orivra mapping: extract explicit finite state machine (COMPOSING, SELECTION, CURSOR_WORD, IDLE_NEXT_WORD, PRIVATE), ensure transitions only use live valid editor snapshots. Study design; do not paste code.

4. **Android performance guide — slow rendering**  
   https://developer.android.com/topic/performance/issues/render  
   Lesson: keyboard perception is dominated by input-path latency, layout churn, frame drops and blocking I/O.  
   Orivra mapping: keep candidate 354 fast-path pure; do not read SharedPreferences, sort learned-word maps, rebuild rows or block on disk from key touches. Use trace-based jank analysis; tests on actual Samsung 120 Hz device are necessary.

5. **HeliBoard**  
   https://github.com/HeliBorg/HeliBoard  
   Lesson: offline dictionaries, personalized words, configurable touch/gestures, backup and multilingual handling. GPL-3.0 components need careful licensing review; do not copy blindly.

6. **FlorisBoard**  
   https://github.com/florisboard/florisboard  
   Lesson: modular Kotlin IME architecture, keyboard layouts, themes, state and extensions. Apache-2.0 project. Its published roadmap notes incomplete prediction features, so not the only prediction reference.

7. **FUTO Keyboard**  
   https://github.com/futo-org/android-keyboard  
   Lesson: offline-focused modern LatinIME fork and practical decoder design. FUTO Source First license is **not** a generic free-to-copy permissive grant. Study architecture, not code-port.

8. **Google Research — The Machine Intelligence Behind Gboard**  
   https://research.google/blog/the-machine-intelligence-behind-gboard/  
   Lesson: combine touch spatial uncertainty with lexicon and context model; n-gram weighted search/finite-state decoding improves joined words, mistakes, and phrase completion.  
   Orivra mapping: *ranked candidate combination* only off UI thread. Preserve explicitly typed forms and tap-only alternatives before any auto-correction.

9. **Google Research — Spatial model personalization in Gboard**  
   https://research.google/pubs/spatial-model-personalization-in-gboard/  
   Lesson: adaptive key-center offsets can help repeated personal tap errors, but avoid overfitting to a small set of taps.  
   Orivra mapping: opt-in aggregate per-key spatial offsets only after sufficient eligible observations; no raw touch trails, no sensitive editors, no implied release.

10. **Google Research — A cost–benefit study of text-entry suggestion interaction**  
    https://research.google/pubs/a-costbenefit-study-of-text-entry-suggestion-interaction/  
    Lesson: more suggestions are not always better if reading them slows down actual typing.  
    Orivra mapping: three visually stable chips, confidence-gated extra suggestions, preserve Candidate 345 strip stability.

## Candidate 354 source audit crosswalk

- **Phone confirmed**: smoother typing and better suggestion catch-up. No measured latency claims.
- **Existing mechanisms**: Background PredictionDispatcher and 347 typed-learning worker; Candidate 345 stable prediction strip; 351 safe selection replacement; 353 touch-order and next-word continuity.
- **Research 356 (NOT wired)**: immutable, pre-ranked lookup from a *verified opt-in evidence snapshot*. Legacy completedWords/nextWords each scans and sorts small stored maps on the suggestion request path. A pre-ranked, background-built index should keep exact Candidate 354 ranking while reducing sorting and allocations on the hot path. Validate parity and privacy before considering a new APK.
- **Research 355 (NOT wired)**: quote/unicode-boundary punctuation regression, separately tested.

## Minimum acceptance criteria before future APK

- Preserve Candidate 354 visible suggestions and positive Samsung typing response.
- Exact legacy parity under a bounded 192-word/256-pair model, including short words, ties, malformed inputs and all allowed limits.
- Confirm no shared-preference reads, disk writes, network, leaked editor text, or sorting in every tap-path lookup; index building strictly off UI thread.
- Rebuild immutable snapshots only when verified word evidence changes and when editor session/consent generation is valid.
- Retain default-OFF extra word-memory opt-in, personal-learning gate, secured inputType exclusions, V4 backup and OFF+erase behavior.
- Instrumented emulator: selection, composition, Backspace, punctuation, Search/Keep, rapid two-thumb events, 20k+ synthetic stress; no failures.
- Samsung phone: manual responsiveness and real-life cursor/long-draft validation. Never represent emulator as device validation.

## Engineering decision

Do **not** attempt to replicate Gboard's proprietary model or copy licensed code. Prefer a small, testable, offline ranker with verified-word learning, contextual signals, and a bounded editor-state machine. Preserve the responsive Candidate 354 UX first.
