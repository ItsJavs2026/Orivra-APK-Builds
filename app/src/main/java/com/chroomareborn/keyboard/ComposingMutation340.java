package com.chroomareborn.keyboard;

/**
 * A single character-entry transaction for remote Android editors.
 *
 * Some InputConnections return false AFTER mutating their text. Blindly
 * calling commitText(letter) then duplicates the typed character. Other
 * editors reject composing text without changing anything, in which case
 * a guarded commit fallback is appropriate.
 *
 * Editor snapshots are transient, capped, and never recorded or exported.
 * This policy never retries after an ambiguous or stale mutation.
 */
final class ComposingMutation340 {
    private ComposingMutation340() {}
    static final int READ_WINDOW = 256;

    enum Result {
        COMPOSING,        // Verified composing operation
        COMMITTED,        // Character inserted, no safe composing ownership
        PRECONDITION,     // Caret no longer follows the locally composed word
        REJECTED,         // Could not verify a successful insertion
        STALE             // Editor/session changed or mutation is ambiguous
    }
    interface Editor {
        boolean sameSession();
        CharSequence before();
        boolean setComposing(String next);
        boolean finishComposing();
        boolean commit(String addition);
    }
    private static String copy(CharSequence value) {
        return value == null ? null : value.toString();
    }
    private static String expected(String before,String addition) {
        String text=before+addition;
        return text.length()<=READ_WINDOW ? text :
                text.substring(text.length()-READ_WINDOW);
    }

    static Result append(Editor editor,String prior,String addition) {
        if(editor==null || prior==null || addition==null ||
                addition.isEmpty() || addition.length()>2 ||
                prior.length()>READ_WINDOW || !editor.sameSession())
            return Result.REJECTED;
        try {
            String before=copy(editor.before());
            if(before!=null && !before.endsWith(prior))
                return Result.PRECONDITION; // no editor mutation attempted
            boolean accepted=editor.setComposing(prior+addition);
            if(!editor.sameSession())return Result.STALE;
            String after=copy(editor.before());
            if(before==null || after==null)
                return accepted?Result.COMPOSING:Result.REJECTED;
            String wanted=expected(before,addition);
            if(wanted.equals(after)) {
                if(accepted)return Result.COMPOSING;
                // Even a false-returning editor may have left an active
                // composing span. Finish it without changing visible text so
                // the next key cannot replace the whole previous word.
                if(!editor.finishComposing() || !editor.sameSession() ||
                        !wanted.equals(copy(editor.before())))
                    return Result.STALE;
                return Result.COMMITTED;
            }
            // A false result is not permission to type a second time if
            // the host changed *anything*. Avoid doubling or corrupting text.
            if(!before.equals(after))return Result.STALE;
            if(accepted)return Result.REJECTED;
            // The editor affirmatively rejected composition and did not
            // modify text. Finalize only this old span, then commit once.
            if(!editor.finishComposing() || !editor.sameSession() ||
                    !before.equals(copy(editor.before())))
                return Result.STALE;
            if(!editor.commit(addition) || !editor.sameSession())
                return Result.REJECTED;
            return wanted.equals(copy(editor.before()))
                    ?Result.COMMITTED:Result.STALE;
        } catch(RuntimeException detached) {
            return Result.STALE;
        }
    }
}
