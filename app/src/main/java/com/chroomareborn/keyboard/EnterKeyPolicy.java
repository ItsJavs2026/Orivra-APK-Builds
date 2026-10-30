package com.chroomareborn.keyboard;

/**
 * Safety-first Enter behavior. An explicit SEARCH or GO field submits on an
 * ordinary Enter tap. SEND (chat/message) still requires a deliberate held
 * release; ordinary multiline notes insert a newline only. App-specific
 * guesses based on package names are forbidden.
 *
 * Constants match android.view.inputmethod.EditorInfo, but this class remains
 * dependency-free for fast Java regression testing.
 */
public final class EnterKeyPolicy {
    static final int IME_ACTION_GO = 2;
    static final int IME_ACTION_SEARCH = 3;
    static final int IME_ACTION_SEND = 4;
    static final int IME_MASK_ACTION = 0xFF;
    static final int IME_FLAG_NO_ENTER_ACTION = 0x40000000;
    private EnterKeyPolicy() {}

    public static boolean supportsLongPressSend(int imeOptions) {
        // NO_ENTER_ACTION forbids replacing the normal Enter key with Send.
        // It does not forbid a separate, deliberately held SEND gesture.
        return (imeOptions & IME_MASK_ACTION) == IME_ACTION_SEND;
    }

    /**
     * Only an explicitly declared SEARCH/GO action may execute on a short
     * Enter tap. Some editable search fields are MULTI_LINE and Android
     * automatically adds NO_ENTER_ACTION; the app's explicit SEARCH or GO
     * declaration is still more specific than that generic formatting hint.
     * SEND always requires a deliberate hold; no unspecified action submits.
     * Returns 0 when no explicit search/navigation action is declared.
     */
    static int tapEditorAction(int imeOptions) {
        // Explicit Search/Go is intent; NO_ENTER_ACTION can be automatically
        // attached by Android for multi-line-capable search editors.
        // Do not promote SEND, DONE, NEXT or unspecified actions.
        int action=imeOptions & IME_MASK_ACTION;
        return action==IME_ACTION_SEARCH || action==IME_ACTION_GO?action:0;
    }

    static String keyLabel(int imeOptions) {
        int action=tapEditorAction(imeOptions);
        return action==IME_ACTION_SEARCH?"⌕":action==IME_ACTION_GO?"Go":"↵";
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
