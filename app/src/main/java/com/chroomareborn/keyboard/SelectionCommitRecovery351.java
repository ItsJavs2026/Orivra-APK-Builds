package com.chroomareborn.keyboard;

/**
 * Rare recovery when an Android editor no longer has the composing suffix
 * expected by Orivra. The original composing transaction has NOT written
 * anything (PRECONDITION). Resolve selection before a one-time commit.
 *
 * On a real highlighted selection, commitText REPLACES the selection: do not
 * first call finishComposingText (some editors collapse the highlight).
 * With no selection, an old composing span can be finalized, but only if
 * both bounded caret snapshots remain unchanged afterward.
 *
 * Never replay a key after ambiguous or false-but-mutating editor results.
 * Bounded editor text is ephemeral; no storage/logging/network.
 */
final class SelectionCommitRecovery351 {
    private SelectionCommitRecovery351(){}
    static final int WINDOW=96;
    enum Result {APPLIED, STALE}

    interface Editor {
        boolean sameSession();
        CharSequence before();
        CharSequence after();
        CharSequence selectedText();
        boolean finishComposing();
        boolean commitOnce(String value);
    }

    private static String copy(CharSequence text) {
        return text==null?null:text.toString();
    }
    private static String expected(String before,String value) {
        String joined=before+value;
        return joined.length()<=WINDOW?joined:
                joined.substring(joined.length()-WINDOW);
    }
    static Result recover(Editor editor,boolean selectionPreviouslyKnown,String value) {
        if(editor==null || value==null || value.isEmpty() ||
                value.length()>2 || !editor.sameSession())return Result.STALE;
        try {
            String left=copy(editor.before());
            String right=copy(editor.after());
            String selected=copy(editor.selectedText());
            if(left==null || right==null || !editor.sameSession())
                return Result.STALE;
            // If Android reported selection but now refuses to reveal it,
            // do NOT risk finishing composition and collapsing the highlight.
            if(selectionPreviouslyKnown && (selected==null || selected.isEmpty()))
                return Result.STALE;
            boolean highlighted=selected!=null && !selected.isEmpty();
            if(!highlighted) {
                if(!editor.finishComposing() || !editor.sameSession())return Result.STALE;
                if(!left.equals(copy(editor.before())) ||
                        !right.equals(copy(editor.after())) ||
                        !editor.sameSession())return Result.STALE;
                String newlySelected=copy(editor.selectedText());
                if(newlySelected!=null && !newlySelected.isEmpty())
                    return Result.STALE;
            }
            // Exactly ONE editor mutation; a false return can still mean
            // the editor changed text, so verify instead of retrying.
            editor.commitOnce(value);
            if(!editor.sameSession())return Result.STALE;
            if(!expected(left,value).equals(copy(editor.before())) ||
                    !right.equals(copy(editor.after())))
                return Result.STALE;
            return Result.APPLIED;
        } catch(RuntimeException detachedEditor) {
            return Result.STALE;
        }
    }

    /** A known selection must remain active until the replacement commit.
     * Never finalize an old composing span before replacing highlighted text.
     */
    static boolean finishBeforeTypedReplacement(boolean highlighted) {
        return !highlighted;
    }
}
