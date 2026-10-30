package com.chroomareborn.keyboard;

/**
 * Visual stability policy for fast typing; no text read, storage or network.
 *
 * Ordinary empty suggestion slots are deliberately QUIET: never paint the
 * "Keep typing" or "NW OFF" status in a strip where predictions may appear
 * milliseconds later. Private/disabled fields keep their privacy message.
 *
 * A completed edit might echo Android's cursor callback asynchronously.
 * Suppress only one known one-character forward echo, never a selection,
 * backspace, multi-character paste or a different editor session.
 */
final class PredictionStripStability342 {
    static final long ECHO_WINDOW_MS=180;
    private PredictionStripStability342(){}

    static boolean showPrivateStatus(boolean sensitive,boolean suggestionsAllowed) {
        return sensitive || !suggestionsAllowed;
    }

    static boolean autoExpandDuringTyping() {
        // The row above the keyboard is manually expandable with the arrow.
        // Autoshifting its height after every prediction caused noticeable lag.
        return false;
    }

    static boolean suppressExpectedCursorEcho(long ageMs,int oldStart,int oldEnd,
            int newStart,int newEnd,int expectedNewStart,
            boolean composingEmpty,boolean editorSame) {
        return composingEmpty && editorSame && ageMs>=0 &&
                ageMs<=ECHO_WINDOW_MS && oldStart>=0 &&
                oldStart==oldEnd && newStart==newEnd &&
                newStart==oldStart+1 && newStart==expectedNewStart;
    }
}
