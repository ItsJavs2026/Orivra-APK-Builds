package com.chroomareborn.keyboard;

/**
 * Selection callback classification when the host editor has no active
 * composing span. A word immediately LEFT of the caret is not evidence
 * of editing in the middle if the caret is already at document end.
 *
 * Previous code unconditionally set editingInMiddle=true on any nonempty
 * current caret word, even after a late callback at the end of a long
 * message. That could suppress live composing and prefix predictions.
 *
 * A genuine editor suffix or selected range still requires editing mode.
 * No editor content is stored, recorded, or mutated here.
 */
final class LongDraftCaretState375 {
    private LongDraftCaretState375() {}

    static boolean editingWithoutComposition(
            boolean hasSelectedText,boolean textAfterCaret) {
        return hasSelectedText || textAfterCaret;
    }

    /** Never resurrect an active composing region the host rejected.
     * When its reported span is gone, enter committed-word fallback, but
     * do not retain a sticky "editing in the middle" flag at text end.
     */
    static boolean afterHostEndedComposition(
            boolean hasSelectedText,boolean textAfterCaret) {
        return editingWithoutComposition(hasSelectedText,textAfterCaret);
    }
}
