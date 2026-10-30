package com.chroomareborn.keyboard;

/**
 * Strict gate for one stable, visibly composed known word.
 * This does NOT claim the host sent the message; a stable typed full word
 * is weaker evidence and must be repeated before suggestion promotion.
 */
final class StableWordUsagePolicy329 {
    private StableWordUsagePolicy329(){}
    static String candidate(String raw,boolean optedIn,boolean editorAllows,
            boolean sensitive,boolean editing,boolean selected,
            boolean textAfterCursor,boolean symbols,boolean numericEditor) {
        if(!optedIn || !editorAllows || sensitive || editing || selected ||
                textAfterCursor || symbols || numericEditor)return null;
        return EnglishApostrophePolicy.canonical(raw);
    }
    static boolean sameVisibleWord(String candidate,String composing,
            CharSequence editorBeforeCursor,boolean sameEditor,
            boolean sameSession) {
        if(!sameEditor || !sameSession || candidate==null ||
                editorBeforeCursor==null || composing==null)return false;
        String current=EnglishApostrophePolicy.canonical(composing);
        if(!candidate.equals(current))return false;
        String before=editorBeforeCursor.toString();
        if(before.length()<composing.length() ||
                !before.regionMatches(true,before.length()-composing.length(),
                        composing,0,composing.length()))return false;
        int prior=before.length()-composing.length()-1;
        // A valid standalone word boundary or empty editor; reject
        // a suffix inside another longer token.
        return prior<0 || !Character.isLetter(before.charAt(prior));
    }
}
