package com.chroomareborn.keyboard;

/**
 * Deletes the final character of a live Android composing word.
 * finishComposingText() alone is NEVER a delete operation.
 *
 * Snapshots are transient for verification only; no persistent text,
 * network, or logging. A false-returning editor gets a fallback only
 * if the same session and the complete before-cursor snapshot are intact.
 */
final class FinalComposingDelete338 {
    private FinalComposingDelete338() {}
    enum Result { APPLIED, REJECTED, STALE }
    interface Editor {
        boolean sameSession();
        CharSequence before();
        boolean replaceComposingWithEmpty();
        boolean finishComposing();
        boolean deleteOneCodePoint();
    }
    private static String snapshot(CharSequence text){
        return text==null?null:text.toString();
    }
    static Result apply(Editor editor,String lastCharacter) {
        if(editor==null||lastCharacter==null||
                lastCharacter.codePointCount(0,lastCharacter.length())!=1 ||
                !editor.sameSession())return Result.REJECTED;
        try {
            String before=snapshot(editor.before());
            if(before!=null && !before.endsWith(lastCharacter))
                return Result.STALE;
            // For an active composing span, setComposingText("",1) is
            // replacement/deletion. finishComposingText() is NOT.
            boolean replacement=editor.replaceComposingWithEmpty();
            if(!editor.sameSession())return Result.STALE;
            String after=snapshot(editor.before());
            String expected=before==null?null:
                    before.substring(0,before.length()-lastCharacter.length());
            if(expected!=null && expected.equals(after))
                return Result.APPLIED;
            if(replacement)
                return after==null?Result.APPLIED:Result.REJECTED;
            // Some custom editors reject empty composition. One guarded
            // fallback is allowed only when nothing was modified.
            if(before==null || after==null || !before.equals(after))
                return Result.STALE;
            if(!editor.finishComposing() || !editor.sameSession())
                return Result.REJECTED;
            String finalized=snapshot(editor.before());
            if(!before.equals(finalized))return Result.STALE;
            if(!editor.deleteOneCodePoint()||!editor.sameSession())
                return Result.REJECTED;
            String deleted=snapshot(editor.before());
            return expected.equals(deleted)?Result.APPLIED:Result.REJECTED;
        } catch(RuntimeException detached) {
            return Result.STALE;
        }
    }
}
