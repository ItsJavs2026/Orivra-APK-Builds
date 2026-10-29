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
        // Unknown suffix means no invented separator. Respect existing text.
        CharSequence following = editor.after(1);
        String insertion = candidate + (following != null && following.length() == 0 ? " " : "");
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
