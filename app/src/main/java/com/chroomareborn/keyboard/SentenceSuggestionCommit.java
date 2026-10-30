package com.chroomareborn.keyboard;

/** One-tap, single-commit replacement of an active composing span. */
final class SentenceSuggestionCommit {
    private SentenceSuggestionCommit() {}
    enum Result { APPLIED, STALE, REJECTED }
    interface Editor {
        CharSequence before(int length);
        CharSequence after(int length);
        void beginBatch();
        boolean commit(String value);
        void endBatch();
    }
    static Result insert(Editor editor, String expected, String candidate) {
        if (editor == null || expected == null || expected.isEmpty() ||
                candidate == null || candidate.isEmpty()) return Result.REJECTED;
        CharSequence live = editor.before(expected.length());
        if (live == null || !expected.contentEquals(live)) return Result.STALE;
        // Every explicit completion has a word boundary, unless a boundary
        // or closing punctuation already follows. Never double-space.
        CharSequence following = editor.after(1);
        String insertion = candidate + SuggestionContinuity353.spaceAfter(following);
        editor.beginBatch();
        try {
            // commitText replaces the active composing span. Never retry a
            // failed commit with deletion: remote editor state is uncertain.
            return editor.commit(insertion) ? Result.APPLIED : Result.REJECTED;
        } finally {
            editor.endBatch();
        }
    }
}
