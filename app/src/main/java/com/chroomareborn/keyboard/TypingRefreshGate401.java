package com.chroomareborn.keyboard;

/**
 * Research 401: one bounded, editor-scoped pending suggestion refresh.
 * This class never reads/stores typed text, suggestions or touch positions.
 *
 * The IME continues committing keys synchronously through the existing
 * safety-checked InputConnection path, but expensive suggestions are only
 * rendered at most once for a burst of rapid character/Space presses.
 */
final class TypingRefreshGate401 {
    static final long WAIT_MS=96L;
    private Object editor;
    private long session=-1L;
    private boolean pending;

    boolean arm(long currentSession,Object currentEditor){
        if(currentEditor==null)return false;
        if(pending && session==currentSession && editor==currentEditor)
            return false;
        editor=currentEditor;
        session=currentSession;
        pending=true;
        return true;
    }
    boolean isPendingFor(long currentSession,Object currentEditor){
        return pending && editor!=null && session==currentSession &&
                editor==currentEditor;
    }
    boolean consume(long currentSession,Object currentEditor){
        boolean valid=isPendingFor(currentSession,currentEditor);
        cancel();
        return valid;
    }
    boolean isPending(){return pending;}
    void cancel(){pending=false;editor=null;session=-1L;}
    static boolean safeToCoalesce(boolean sensitive,boolean allowed,
            boolean numeric,boolean symbols,boolean hasSelection){
        return !sensitive && allowed && !numeric && !symbols &&
                !hasSelection;
    }
}
