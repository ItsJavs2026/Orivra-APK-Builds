package com.chroomareborn.keyboard;

/**
 * Pure, editor-identity-based gate for Backspace's delayed hold/repeat.
 * It is deliberately separate from a finger being physically held:
 * cancelling a repeat must not make the active key safe to rebuild.
 */
public final class BackspaceRepeatGuard {
    private BackspaceRepeatGuard() {}

    public static boolean startAllowed(boolean fingerDown,boolean holdArmed,
            boolean cancelled,boolean swiping,boolean moved,
            long touchedSession,long currentSession,
            Object touchedEditor,Object currentEditor) {
        return fingerDown && holdArmed && !cancelled && !swiping && !moved &&
                BackspaceSwipePolicy.sameEditorSession(
                        touchedSession,currentSession,touchedEditor,currentEditor);
    }

    public static boolean repeatAllowed(boolean fingerDown,boolean repeatArmed,
            long touchedSession,long currentSession,
            Object touchedEditor,Object currentEditor) {
        return fingerDown && repeatArmed &&
                BackspaceSwipePolicy.sameEditorSession(
                        touchedSession,currentSession,touchedEditor,currentEditor);
    }

    /** The keyboard's key hierarchy must stay mounted until touch release. */
    public static boolean mayRebuildKeys(boolean fingerDown,boolean repeatArmed) {
        return !fingerDown && !repeatArmed;
    }
}
