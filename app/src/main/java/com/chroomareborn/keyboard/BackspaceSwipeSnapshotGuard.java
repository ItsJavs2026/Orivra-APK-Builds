package com.chroomareborn.keyboard;

/**
 * In-memory, short-lived Backspace swipe protection. A word deletion initiated
 * on one editor snapshot must not silently target a different cursor context
 * in the same editor before the finger lifts or the posted edit runs.
 *
 * No persistence, telemetry, clipboard access or network use.
 */
public final class BackspaceSwipeSnapshotGuard {
    public static final int BEFORE_LIMIT = 2048;
    public static final int AFTER_LIMIT = 96;
    public static final int SELECTION_LIMIT = 2048;
    private BackspaceSwipeSnapshotGuard() {}

    /** Opaque snapshot, retained only for the duration of one swipe gesture. */
    public static final class Ticket {
        private final String before, selected, after;
        private final boolean hadSelection;
        private final int words;
        private Ticket(String before, String selected, String after,
                       boolean hadSelection, int words) {
            this.before=before;
            this.selected=selected;
            this.after=after;
            this.hadSelection=hadSelection;
            this.words=words;
        }
    }

    /**
     * Fail closed if the editor will not expose the entire required context.
     * A non-null selected range is treated as authoritative even if Android's
     * cached selection callback has not yet arrived.
     */
    public static Ticket capture(CharSequence before, CharSequence selected,
                                 CharSequence after, boolean selectionExpected,
                                 int words) {
        if(words<=0 || words>8 || before==null || after==null ||
                before.length()>BEFORE_LIMIT || after.length()>AFTER_LIMIT ||
                (selected!=null && selected.length()>SELECTION_LIMIT))return null;
        String select=selected==null ? "" : selected.toString();
        boolean hasSelectedText=!select.isEmpty();
        if(selectionExpected && !hasSelectedText)return null;
        if(!hasSelectedText && BackspaceSwipePolicy.safeCharsToDelete(
                before, "", words)<=0)return null;
        return new Ticket(before.toString(),select,after.toString(),
                hasSelectedText,words);
    }

    /** Exact word count and surrounding editor text must still match. */
    public static boolean isCurrent(Ticket ticket, CharSequence before,
                                    CharSequence selected, CharSequence after,
                                    boolean selectionExpected, int words) {
        if(ticket==null || ticket.words!=words)return false;
        Ticket live=capture(before,selected,after,selectionExpected,words);
        return live!=null && ticket.hadSelection==live.hadSelection &&
                ticket.before.equals(live.before) &&
                ticket.selected.equals(live.selected) &&
                ticket.after.equals(live.after);
    }
}
