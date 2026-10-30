package com.chroomareborn.keyboard;

import java.util.Locale;

/**
 * Validates one word that Orivra just typed before an explicit Space.
 *
 * English's standalone "I" and "a" must not be lost to the legacy
 * 2-letter-minimum frequency policy. No new personal word is persisted here;
 * the caller may only offer validated tokens to session-local learning.
 */
final class TappedWordBoundary340 {
    private TappedWordBoundary340() {}

    static String verified(String visible,CharSequence editorBefore) {
        if(visible==null || editorBefore==null || visible.isEmpty())
            return null;
        String normalized;
        if(visible.length()==1 && ("a".equalsIgnoreCase(visible) ||
                "i".equalsIgnoreCase(visible)))
            normalized=visible.toLowerCase(Locale.ROOT);
        else normalized=EnglishApostrophePolicy.canonical(visible);
        if(normalized==null)return null;
        int length=visible.length();
        String before=editorBefore.toString();
        if(before.length()<length || !before.regionMatches(true,
                before.length()-length,visible,0,length))
            return null;
        int preceding=before.length()-length-1;
        if(preceding>=0) {
            char prev=before.charAt(preceding);
            if(Character.isLetterOrDigit(prev) || prev=='\'' || prev=='\u2019')
                return null;
        }
        return normalized;
    }
}
