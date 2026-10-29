package com.chroomareborn.keyboard;

/**
 * Pure gesture and deletion-range policy for the Backspace swipe.
 * No input content is stored or transmitted.
 */
public final class BackspaceSwipePolicy {
    private BackspaceSwipePolicy() {}

    /** Return 0 for an ordinary tap/non-leftward gesture, otherwise 1..8 words. */
    public static int wordsForSwipe(float leftPx, float verticalPx,
                                   float thresholdPx, float additionalWordPx) {
        // Malformed pointer coordinates must fail closed, never delete text.
        if (!Float.isFinite(leftPx) || !Float.isFinite(verticalPx) ||
                !Float.isFinite(thresholdPx) || !Float.isFinite(additionalWordPx) ||
                thresholdPx <= 0 || additionalWordPx <= 0 || leftPx < thresholdPx ||
                leftPx < Math.abs(verticalPx) * 1.25f) return 0;
        float step = Math.max(1f, additionalWordPx);
        return Math.min(8, 1 + (int) ((leftPx - thresholdPx) / step));
    }

    /**
     * A delayed deletion must only target the editor and input session that
     * received the original finger-down event.
     */
    public static boolean sameEditorSession(long gestureSession, long currentSession,
                                            Object touchedEditor, Object currentEditor) {
        return touchedEditor != null && touchedEditor == currentEditor &&
                gestureSession == currentSession;
    }

    /**
     * Non-destructive, privacy-local preview of the exact characters the
     * existing word-boundary deletion policy would remove. It abbreviates
     * the *display* only; the deletion range itself is unchanged.
     */
    public static String previewForSwipe(CharSequence beforeCursor,
                                         int words, int maxCodePoints) {
        int chars=charsToDelete(beforeCursor,words);
        if(chars<=0 || maxCodePoints<=0)return "";
        String fragment=beforeCursor.subSequence(beforeCursor.length()-chars,
                beforeCursor.length()).toString();
        // Keep the one-line preview readable without changing editor text.
        fragment=fragment.replace("\r\n","↵").replace("\r","↵")
                .replace("\n","↵").replace("\t","⇥");
        int length=fragment.codePointCount(0,fragment.length());
        if(length<=maxCodePoints)return fragment;
        int start=fragment.offsetByCodePoints(0,length-maxCodePoints);
        return "…"+fragment.substring(start);
    }

    /**
     * Return UTF-16 units before the cursor to remove, aligned to whitespace
     * boundaries. This avoids cutting surrogate pairs within a word.
     */
    public static int charsToDelete(CharSequence beforeCursor, int words) {
        if (beforeCursor == null || words <= 0) return 0;
        int end = beforeCursor.length();
        int start = end;
        for (int i = 0; i < words && start > 0; i++) {
            while (start > 0 && Character.isWhitespace(beforeCursor.charAt(start - 1)))
                start--;
            while (start > 0 && !Character.isWhitespace(beforeCursor.charAt(start - 1)))
                start--;
        }
        return end - start;
    }
}
