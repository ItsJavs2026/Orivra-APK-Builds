package com.chroomareborn.keyboard;

import java.util.Map;

/**
 * Conservative QWERTY same-row neighboring double-key correction.
 *
 * The 2-char prefix is untouched; the mistyped suffix must be the same
 * letter twice. The *only* eligible replacement is another doubled letter,
 * one physical horizontal key away, and in the installed offline lexicon.
 * Ambiguous neighbors abstain. This does not learn, log or commit any text.
 */
final class NeighborDoubleKeyCorrectionPolicy {
    private static final String[] ROWS={"qwertyuiop","asdfghjkl","zxcvbnm"};
    private NeighborDoubleKeyCorrectionPolicy(){}

    static String unique(String typed,Map<String,Integer> lexicon){
        if(typed==null || lexicon==null || !typed.matches("[a-z]{4,14}") ||
                lexicon.containsKey(typed))return null;
        int n=typed.length();
        char repeated=typed.charAt(n-1);
        if(repeated!=typed.charAt(n-2))return null;
        String row=null;
        int offset=-1;
        for(String candidate:ROWS) {
            int found=candidate.indexOf(repeated);
            if(found>=0){row=candidate;offset=found;break;}
        }
        if(row==null)return null;
        String prefix=typed.substring(0,n-2),only=null;
        for(int position: new int[]{offset-1,offset+1}) {
            if(position<0 || position>=row.length())continue;
            char neighbor=row.charAt(position);
            String corrected=prefix+neighbor+neighbor;
            Integer count=lexicon.get(corrected);
            if(count==null || count<12)continue;
            if(only!=null)return null; // equally plausible neighboring word
            only=corrected;
        }
        return only;
    }
}
