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
