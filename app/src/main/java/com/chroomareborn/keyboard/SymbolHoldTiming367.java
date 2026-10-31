package com.chroomareborn.keyboard;

/**
 * Fast hint-symbol hold calibration for the next Orivra trial.
 * Only alphabet QWERTY hint symbols use this timing; key taps, shift,
 * Backspace repeat, Enter hold and long-press prediction panels are unchanged.
 *
 * The default is 320 ms instead of Android's longer general UI long press.
 * An opt-out mode preserves the exact Android system preference for people
 * who need a longer hold. A limited range protects against accidental hints.
 */
final class SymbolHoldTiming367 {
    private SymbolHoldTiming367() {}
    static final int FAST_HOLD_MS=320;
    static final String PREFS="keyboard_feedback";
    static final String KEY_FAST_SYMBOL_HOLD="fast_special_character_hold";

    static int delayMs(int androidLongPressMs,boolean fastEnabled) {
        // Fail safely on an unexpected Android duration; never use 0 ms.
        int safeSystem=androidLongPressMs>=180 && androidLongPressMs<=3000?
                androidLongPressMs:500;
        return fastEnabled?Math.min(safeSystem,FAST_HOLD_MS):safeSystem;
    }
}
