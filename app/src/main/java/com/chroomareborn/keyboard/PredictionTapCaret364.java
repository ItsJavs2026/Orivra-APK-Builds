package com.chroomareborn.keyboard;

/**
 * A one-shot, editor-local insertion boundary after an EXPLICIT tap on an
 * already committed word's prediction chip.
 *
 * "I am hungry" -> tap "am" -> type "not" -> "I am not hungry".
 *
 * At the chip tap, an existing right-hand separator is left untouched and
 * the caret remains just before it. On the FIRST alphabetic key, insert
 * " " + that letter in one commit; the existing right-hand separator stays
 * after the new word. Subsequent letters use normal editor insertion.
 *
 * This avoids both "amnot hungry" and "am nothungry" without inserting an
 * ugly duplicate space while the user is still choosing what to type.
 *
 * Only short bounded snapshots are held in memory for one next key; no
 * preferences, analytics, editor logs, autocorrect or network operations.
 */
final class PredictionTapCaret364 {
    private static final int WINDOW=48;
    private PredictionTapCaret364(){}

    static final class Ticket {
        final String before;
        final String after;
        final String accepted;
        Ticket(String left,String right,String word) {
            before=left;
            after=right;
            accepted=word;
        }
    }

    static int sampleLimit(){return WINDOW;}

    /** Capture only at a safe, successfully tapped cursor-word replacement.
     * Never arm while typing a composing word or editing a selected range.
     */
    static Ticket arm(CharSequence before,CharSequence after,
            String accepted,boolean suggestionsAllowed,
            boolean selected,boolean sensitive,boolean numeric,
            boolean symbols) {
        if(!suggestionsAllowed || selected || sensitive || numeric ||
                symbols || before==null || after==null ||
                accepted==null || accepted.length()<1 ||
                accepted.length()>WINDOW)
            return null;
        String left=before.toString(),right=after.toString();
        if(left.isEmpty() || right.isEmpty() ||
                left.length()>WINDOW || right.length()>WINDOW ||
                !left.endsWith(accepted))
            return null;
        // Only *one ordinary space* before existing text. Do not rewrite
        // indentation, tabs, NBSP, multiple spaces, punctuation or newlines.
        if(right.charAt(0)!=' ' ||
                (right.length()>1 && right.charAt(1)==' '))
            return null;
        int prior=left.length()-accepted.length()-1;
        if(prior>=0 && Character.isLetterOrDigit(left.charAt(prior)))
            return null;
        return new Ticket(left,right,accepted);
    }

    /**
     * Fail closed if the cursor moved, host editor changed text, selection
     * appeared, or the next input isn't exactly one alphabetic letter.
     * Call only once after the tapped suggestion; discard ticket regardless.
     */
    static boolean prefixFirstLetter(Ticket ticket,
            CharSequence currentBefore,CharSequence currentAfter,
            String nextKey,boolean sameEditor,boolean sameSession,
            boolean editingBetweenWords,boolean selected,
            boolean sensitive,boolean numeric,boolean symbols,
            boolean suggestionsAllowed) {
        if(ticket==null || !sameEditor || !sameSession ||
                !editingBetweenWords || selected || sensitive ||
                numeric || symbols || !suggestionsAllowed ||
                currentBefore==null || currentAfter==null ||
                nextKey==null || nextKey.length()!=1)
            return false;
        char ch=nextKey.charAt(0);
        if(!((ch>='a'&&ch<='z')||(ch>='A'&&ch<='Z')))
            return false;
        return ticket.before.contentEquals(currentBefore) &&
                ticket.after.contentEquals(currentAfter);
    }
}
