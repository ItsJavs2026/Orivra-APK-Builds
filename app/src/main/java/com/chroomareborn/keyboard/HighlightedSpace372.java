package com.chroomareborn.keyboard;

import java.util.Locale;

/**
 * Space may accept only a real, final-rendered highlighted correction for
 * the active composing word. Never turns the strip's visual highlight into
 * a claim of verified language-model confidence. Caller must verify a live
 * InputConnection match immediately before the single commit.
 *
 * No automatic editing on Backspace, cursor navigation, private fields,
 * existing committed words, pasted text or incomplete/transient suggestions.
 */
final class HighlightedSpace372 {
    private HighlightedSpace372() {}
    static final String PREF="space_accept_highlight_372";
    static boolean eligible(String expected,String composing,
            String correction,boolean finalRendered,
            boolean visuallyEmphasized,boolean sameSession,
            boolean sameEditor,boolean suggestionsAllowed,
            boolean sensitive,boolean editingMiddle,boolean selected,
            boolean suffixPresent,boolean numeric,boolean symbols,
            boolean splitNextWord) {
        if(expected==null || composing==null || correction==null ||
                !expected.equals(composing) || !finalRendered ||
                !visuallyEmphasized || !sameSession || !sameEditor ||
                !suggestionsAllowed || sensitive || editingMiddle ||
                selected || suffixPresent || numeric || symbols ||
                splitNextWord || expected.length()<1 ||
                expected.length()>30 || correction.length()>38 ||
                correction.isEmpty() || !expected.matches("[A-Za-z']{1,30}") ||
                !correction.matches("[A-Za-z']+(?: [A-Za-z']+)?"))
            return false;
        String raw=expected.replace("'","").toLowerCase(Locale.ROOT);
        String candidate=correction.replace(" ","").replace("'","")
                .toLowerCase(Locale.ROOT);
        if(raw.equals(candidate) && !correction.contains(" "))
            return false;
        // A settled highlighted completion is a user-visible Space choice.
        // Previously one character was unconditionally refused, and larger
        // prefix completions from two/three letters failed edit distance.
        // Only a one-word, literal prefix completion gains this exception.
        // Multiword RISE phrases must still be explicitly tapped or swiped.
        if(raw.length()<=3 && candidate.length()>raw.length() &&
                candidate.length()<=24 &&
                correction.matches("[A-Za-z]{2,24}") &&
                candidate.startsWith(raw))
            return true;
        if(Math.abs(raw.length()-candidate.length())>3)return false;
        // Ordinary one-word typos and a tightly matching joined-space fix.
        int distance=editDistance(raw,candidate);
        return distance<=Math.max(2,raw.length()/5);
    }
    private static int editDistance(String a,String b) {
        int[] old=new int[b.length()+1];
        for(int i=0;i<old.length;i++)old[i]=i;
        for(int i=1;i<=a.length();i++) {
            int[] next=new int[b.length()+1];
            next[0]=i;
            for(int j=1;j<=b.length();j++) {
                int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;
                next[j]=Math.min(Math.min(old[j]+1,next[j-1]+1),
                        old[j-1]+cost);
            }
            old=next;
        }
        return old[b.length()];
    }
}
