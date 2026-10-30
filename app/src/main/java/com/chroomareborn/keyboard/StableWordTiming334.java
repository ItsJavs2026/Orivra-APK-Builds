package com.chroomareborn.keyboard;

/**
 * Short common words are often incidental prefixes of longer words.
 * A longer quiet period is required before treating them as finished.
 * Explicit Space and tapped suggestions still count immediately.
 */
final class StableWordTiming334 {
    private StableWordTiming334() {}
    static long milliseconds(String normalizedWord) {
        String canonical=EnglishApostrophePolicy.canonical(normalizedWord);
        if(canonical==null)return -1L;
        int letters=0;
        for(int i=0;i<canonical.length();i++)
            if(Character.isLetter(canonical.charAt(i)))letters++;
        if(letters<=3)return 1600L;
        if(letters<=5)return 1100L;
        if(letters<=6)return 800L;
        return 550L;
    }
}
