package com.chroomareborn.keyboard;

import java.util.Locale;

/**
 * Conservative opt-in learning extraction for explicit ACCEPT taps.
 * Does not persist original sentences. At most one short word pair is
 * extracted from a whole sentence when every other token is identical.
 */
final class LearningV2Policy {
    static final class Pair {
        final String typed,accepted;
        Pair(String in,String out){typed=in;accepted=out;}
    }
    private LearningV2Policy(){}

    static Pair extract(String original,String chosen) {
        if(original==null||chosen==null)return null;
        String left=original.trim(),right=chosen.trim();
        String in=EnglishApostrophePolicy.canonical(left);
        String out=EnglishApostrophePolicy.canonical(right);
        if(in!=null && out!=null && !in.equals(out))
            return new Pair(in,out);
        // Can't align a no-spaces string with a spaced phrase. Not safe
        // to infer which segment was "corrected" from a whole sentence.
        if(left.length()>120 || right.length()>120 ||
                !left.contains(" ")||!right.contains(" "))return null;
        String[] typed=left.split(" +"),accepted=right.split(" +");
        if(typed.length!=accepted.length || typed.length>12 ||
                typed.length<2)return null;
        Pair changed=null;
        for(int i=0;i<typed.length;i++){
            String a=EnglishApostrophePolicy.canonical(typed[i]);
            String z=EnglishApostrophePolicy.canonical(accepted[i]);
            if(a==null||z==null)return null;
            if(a.equals(z))continue;
            if(changed!=null)return null;
            changed=new Pair(a,z);
        }
        return changed;
    }

    /** At least two explicit acceptances must agree before re-ranking. */
    static boolean reinforced(int count){return count>=2;}
}
