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
        return formatPreviewText(beforeCursor.subSequence(
                beforeCursor.length()-chars,beforeCursor.length()),maxCodePoints);
    }

    /**
     * Display-only abbreviated text. No edits, clipboard calls, or storage.
     * A selection may span multiple words and must be previewed as a whole.
     */
    public static String formatPreviewText(CharSequence target,int maxCodePoints) {
        if(target==null || maxCodePoints<=0)return "";
        String fragment=target.toString();
        // Keep the one-line preview readable without altering input text.
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

    /**
     * A slow swipe should not fall into the repeat-backspace timer merely
     * because it has not crossed the larger whole-word swipe threshold yet.
     * Invalid motion data also blocks an accidental tap/hold.
     */
    public static boolean beyondTapSlop(float horizontalPx, float verticalPx, float slopPx) {
        return !Float.isFinite(horizontalPx) || !Float.isFinite(verticalPx) ||
                !Float.isFinite(slopPx) || slopPx <= 0 ||
                Math.abs(horizontalPx) > slopPx || Math.abs(verticalPx) > slopPx;
    }

    /**
     * Compute a deletion range from the editor's FRESH text snapshot, after
     * finishing any active composition. The suffix must still match the text
     * that Orivra was composing, otherwise fail closed rather than delete
     * another editor fragment. This method never stores or logs input text.
     */
    public static int safeCharsToDelete(CharSequence liveBeforeCursor,
                                        CharSequence composingBeforeFinish, int words) {
        if (liveBeforeCursor == null || words <= 0) return 0;
        if (composingBeforeFinish != null && composingBeforeFinish.length() > 0) {
            int offset = liveBeforeCursor.length() - composingBeforeFinish.length();
            if (offset < 0) return 0;
            for (int i = 0; i < composingBeforeFinish.length(); i++)
                if (liveBeforeCursor.charAt(offset + i) != composingBeforeFinish.charAt(i))
                    return 0;
        }
        int chars = charsToDelete(liveBeforeCursor, words);
        // InputConnection.getTextBeforeCursor(2048) can truncate a giant
        // uninterrupted word. Never blindly delete a potentially partial word.
        if (liveBeforeCursor.length() >= 2048 && chars == liveBeforeCursor.length())
            return 0;
        return chars;
    }
}
