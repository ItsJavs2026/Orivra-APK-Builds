package com.chroomareborn.keyboard;

/**
 * Testable, fail-closed Backspace swipe transaction.
 *
 * The input-method service supplies a live InputConnection adapter.
 * This component owns no Android UI, persistent state, network, or logging.
 * A verified preview ticket must still match the editor BEFORE and AFTER
 * composition is finalized, and the editor session must remain unchanged.
 */
public final class BackspaceSwipeEditorTransaction {
    private BackspaceSwipeEditorTransaction() {}

    public enum Result { APPLIED, STALE, REJECTED }

    public interface Editor {
        boolean sameSession();
        boolean selectionExpected();
        CharSequence before(int count);
        CharSequence selected();
        CharSequence after(int count);
        void beginBatchEdit();
        boolean finishComposition();
        boolean deleteBeforeCursor(int utf16Units);
        boolean deleteSelection();
        void endBatchEdit();
    }

    private static boolean current(Editor editor,
            BackspaceSwipeSnapshotGuard.Ticket ticket,int words) {
        return editor.sameSession() &&
                BackspaceSwipeSnapshotGuard.isCurrent(
                        ticket,editor.before(BackspaceSwipeSnapshotGuard.BEFORE_LIMIT),
                        editor.selected(),
                        editor.after(BackspaceSwipeSnapshotGuard.AFTER_LIMIT),
                        editor.selectionExpected(),words) &&
                editor.sameSession();
    }

    public static Result apply(Editor editor,BackspaceSwipeSnapshotGuard.Ticket ticket,
                               int words,CharSequence composingBeforeFinish) {
        if(editor==null || ticket==null || words<1 || words>8)return Result.STALE;
        boolean began=false;
        try {
            if(!current(editor,ticket,words))return Result.STALE;
            editor.beginBatchEdit();
            began=true;
            if(!current(editor,ticket,words))return Result.STALE;
            if(!editor.finishComposition())return Result.REJECTED;
            // A composing word may change, collapse, or move the cursor when
            // finishComposingText is called by an unfamiliar host editor.
            // If so, do not delete whatever is now in front of Backspace.
            if(!current(editor,ticket,words))return Result.STALE;
            CharSequence selected=editor.selected();
            if(selected!=null && selected.length()>0) {
                return editor.sameSession() && editor.deleteSelection() ?
                        Result.APPLIED : Result.REJECTED;
            }
            CharSequence before=editor.before(BackspaceSwipeSnapshotGuard.BEFORE_LIMIT);
            int length=BackspaceSwipePolicy.safeCharsToDelete(
                    before,composingBeforeFinish,words);
            if(length<=0 || !current(editor,ticket,words))return Result.STALE;
            return editor.sameSession() && editor.deleteBeforeCursor(length) ?
                    Result.APPLIED : Result.REJECTED;
        } catch (RuntimeException hostEditorFailure) {
            // A disconnected InputConnection or an editor API failure must
            // never fall back to synthetic key presses or a guessed range.
            return Result.REJECTED;
        } finally {
            if(began) {
                try { editor.endBatchEdit(); }
                catch (RuntimeException disconnectedEditor) {
                    // The original edit result cannot be rolled back here.
                }
            }
        }
    }
}
