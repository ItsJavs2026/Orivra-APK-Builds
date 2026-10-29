package com.chroomareborn.keyboard;

/**
 * Safety-first Enter behavior. The keyboard never submits an editor action on
 * a normal Enter tap. A supported SEND action requires a deliberate long press
 * and release without a cancelled gesture or input-session change.
 *
 * Constants match android.view.inputmethod.EditorInfo, but this class remains
 * dependency-free for fast Java regression testing.
 */
public final class EnterKeyPolicy {
    static final int IME_ACTION_SEND = 4;
    static final int IME_MASK_ACTION = 0xFF;
    static final int IME_FLAG_NO_ENTER_ACTION = 0x40000000;
    private EnterKeyPolicy() {}

    public static boolean supportsLongPressSend(int imeOptions) {
        return (imeOptions & IME_MASK_ACTION) == IME_ACTION_SEND &&
                (imeOptions & IME_FLAG_NO_ENTER_ACTION) == 0;
    }

    /**
     * Safely insert a newline only in a multiline text editor. Single-line
     * fields may treat Enter/newline as submit, so a tap fails closed there.
     * Android InputType TYPE_CLASS_TEXT=1; TYPE_TEXT_FLAG_MULTI_LINE=0x20000.
     */
    public static boolean supportsNewline(int inputType) {
        return (inputType & 0xF) == 1 && (inputType & 0x20000) != 0;
    }

    /** A moved/multifinger gesture fails closed, never sending a message. */
    public static boolean gestureWithinSlop(float deltaX, float deltaY, float slop) {
        return Float.isFinite(deltaX) && Float.isFinite(deltaY) &&
                Float.isFinite(slop) && slop > 0 &&
                Math.abs(deltaX) <= slop && Math.abs(deltaY) <= slop;
    }

    public static boolean shouldSendOnRelease(boolean releasedNormally,
            boolean wasArmed, boolean cancelled, boolean sameEditorSession,
            boolean stillSupportsSend) {
        return releasedNormally && wasArmed && !cancelled &&
                sameEditorSession && stillSupportsSend;
    }

    public static boolean shouldInsertNewline(boolean releasedNormally,
            boolean wasArmed, boolean cancelled, boolean sameEditorSession) {
        return releasedNormally && !wasArmed && !cancelled && sameEditorSession;
    }
}
