package com.chroomareborn.keyboard;

/** Pure-Java sentence boundary detection for IME auto-shift. */
final class SentenceCapitalization {
    private SentenceCapitalization() {}

    static boolean shouldCapitalize(CharSequence beforeCursor) {
        if (beforeCursor == null) return false; // Unknown editor state: do not guess.
        int end = beforeCursor.length();
        while (end > 0 && Character.isWhitespace(beforeCursor.charAt(end - 1))) end--;
        if (end == 0) return true;
        // Enter/newline also starts a new sentence even without punctuation.
        for (int i = end; i < beforeCursor.length(); i++)
            if (beforeCursor.charAt(i) == '\n' || beforeCursor.charAt(i) == '\r') return true;
        // Require whitespace after punctuation, avoiding mid-token changes.
        if (end == beforeCursor.length()) return false;
        char last = beforeCursor.charAt(end - 1);
        return last == '.' || last == '!' || last == '?';
    }
}
