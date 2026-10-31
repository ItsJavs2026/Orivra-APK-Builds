package com.chroomareborn.keyboard;

/**
 * Build 416 experimental opt-in: skip a redundant post-Space preceding-text
 * editor IPC only when a verified ordinary composition has ended in a letter.
 *
 * A literal Space after a letter cannot introduce sentence capitalization.
 * This is a candidate for the normal editor path, not a replacement for
 * reading actual editor state during punctuation, selection or navigation.
 * Call only AFTER commitText(" ", 1) succeeds and the active editor/session
 * is reverified by the service. Never inspect or persist user text here.
 */
final class SpaceShiftFastPath416 {
    private SpaceShiftFastPath416() {}

    static boolean canClearShift(String composedBeforeSpace,
            boolean sameEditorSession, boolean hadMidwordEdit,
            boolean hasTrailingText, boolean hasSelection,
            boolean sensitive, boolean numeric, boolean symbols,
            boolean capsLock) {
        if (!sameEditorSession || hadMidwordEdit || hasTrailingText ||
                hasSelection || sensitive || numeric || symbols || capsLock ||
                composedBeforeSpace == null || composedBeforeSpace.isEmpty())
            return false;
        // This is safe only for a normal composed word ending in a letter.
        // All punctuation / whitespace / ambiguous ends take the original
        // live editor read and sentence-boundary policy.
        return Character.isLetter(
                composedBeforeSpace.charAt(composedBeforeSpace.length()-1));
    }
}
