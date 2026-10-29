package com.chroomareborn.keyboard;

/** Ordinary prose punctuation attachment and post-punctuation word spacing. */
final class PunctuationSpacing {
    private PunctuationSpacing() {}
    /**
     * Ordinary closing punctuation attaches to the preceding word, even when
     * a suggestion or an explicit space left horizontal whitespace behind.
     * Do not consume newlines, indentation, selections or sensitive input.
     * Opening brackets and math symbols deliberately keep their spacing.
     */
    static boolean isClosingPunctuation(String mark) {
        return mark!=null && mark.length()==1 && ".,!?;:%)]}".indexOf(mark.charAt(0))>=0;
    }
    /** Sentence terminators create a fresh, capitalized next-word boundary. */
    static boolean startsNextSentence(String mark) {
        return ".".equals(mark) || "!".equals(mark) || "?".equals(mark);
    }
    static boolean appendSpaceAfterPunctuation(String mark, boolean sensitive,
            boolean hasSelection, boolean textAfterCursor) {
        return !sensitive && !hasSelection && !textAfterCursor &&
                isClosingPunctuation(mark) && !")".equals(mark) &&
                !"]".equals(mark) && !"}".equals(mark) && !"%".equals(mark);
    }
    static int spacesBeforeClosingPunctuation(CharSequence before, String mark,
            boolean sensitive, boolean hasSelection) {
        if(sensitive || hasSelection || before==null || !isClosingPunctuation(mark))return 0;
        int i=before.length();
        while(i>0 && (before.charAt(i-1)==' ' || before.charAt(i-1)=='\u00a0'))i--;
        if(i==before.length() || i==0)return 0;
        char preceding=before.charAt(i-1);
        // Only remove a separator after text, not paragraph indentation.
        if(!Character.isLetterOrDigit(preceding) && ")\u005d}\"'!?.".indexOf(preceding)<0)
            return 0;
        return before.length()-i;
    }
    static boolean insertSpaceBeforeLetter(CharSequence before, String next, boolean sensitive,
            boolean editingInMiddle, boolean hasSelection, boolean textAfterCursor) {
        if (sensitive || editingInMiddle || hasSelection || textAfterCursor ||
                before == null || next == null || !next.matches("[A-Za-z]") || before.length() == 0)
            return false;
        char last = before.charAt(before.length() - 1);
        return last == ',' || last == ';' || last == ':' || last == '!' || last == '?';
    }
}
