package com.chroomareborn.keyboard;

/**
 * Explicit suggestion acceptance: place the caret AFTER exactly one
 * existing horizontal word separator, or AFTER the separator just inserted.
 *
 * The existing Candidate 353 spaceAfter() decides whether text needs a new
 * separator; this policy ONLY chooses InputConnection.commitText's
 * newCursorPosition. A cursor position of 2 means one UTF-16 character
 * after the end of the inserted replacement (the already-present separator).
 * A position of 1 means just after the replacement as usual.
 *
 * Never jump over newlines, closing punctuation, letter text, or selection.
 * No extra InputConnection calls, no dictionary work, no text logging.
 */
final class PredictionTapCaret364 {
    private PredictionTapCaret364(){}

    static int afterAcceptedWord(CharSequence following) {
        if(following==null || following.length()==0)return 1;
        // UTF-16 horizontal separators; Character.SPACE_SEPARATOR includes
        // plain SPACE, NBSP, narrow NBSP, and ideographic space. Do not
        // skip a line break, tab-indent, punctuation, or adjacent letters.
        char first=following.charAt(0);
        return Character.getType(first)==Character.SPACE_SEPARATOR?2:1;
    }
}
