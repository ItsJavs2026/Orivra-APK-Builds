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
        String in=PersonalWordCanonical.canonical(left);
        String out=PersonalWordCanonical.canonical(right);
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
            String a=contextToken(typed[i]);
            String z=contextToken(accepted[i]);
            if(a==null||z==null)return null;
            if(a.equals(z))continue;
            // Single-letter "I" and "a" are legitimate unchanged context,
            // but never stored as a learned correction pair.
            if(a.length()<2||z.length()<2||changed!=null)return null;
            changed=new Pair(a,z);
        }
        return changed;
    }

    private static String contextToken(String value) {
        String lowered=value.toLowerCase(java.util.Locale.ROOT);
        if("i".equals(lowered)||"a".equals(lowered))return lowered;
        return PersonalWordCanonical.canonical(value);
    }

    /** At least two explicit acceptances must agree before re-ranking. */
    static boolean reinforced(int count){return count>=2;}

    /**
     * V2.1 deterministic ranking for an existing V1-compatible correction map.
     * A tie must NOT arbitrarily choose a replacement; explicit taps of
     * competing words express uncertainty rather than a stable preference.
     * Never upgrades a single acceptance or a malformed/same-word pair.
     */
    static String preferred(String typed,java.util.Map<String,Integer> acceptedCounts) {
        String key=PersonalWordCanonical.canonical(typed);
        if(key==null||acceptedCounts==null||acceptedCounts.isEmpty())return null;
        java.util.Map<String,Integer> normalized=new java.util.HashMap<>();
        for(java.util.Map.Entry<String,Integer> item:acceptedCounts.entrySet()){
            String output=PersonalWordCanonical.canonical(item.getKey());
            Integer count=item.getValue();
            if(output==null||output.equals(key)||count==null||
                    count<1||count>1000)continue;
            // Legacy spellings may collide after straight/curly normalization.
            // Never sum duplicated rows; that could falsely reinforce one tap.
            normalized.merge(output,count,Math::max);
        }
        String best=null;
        int highest=1,second=0;
        for(java.util.Map.Entry<String,Integer> item:normalized.entrySet()){
            int value=item.getValue();
            if(value>highest){
                second=highest;highest=value;best=item.getKey();
            }else if(value==highest){
                second=highest; // tied; do not guess a winner
            }else if(value>second)second=value;
        }
        if(best==null||!reinforced(highest)||highest<=second)return null;
        return displayCorrection(typed,best);
    }

    /** Preserve typing case while capitalizing first-person contractions. */
    static String displayCorrection(String raw,String canonical) {
        if(raw==null||canonical==null)return canonical;
        if(raw.length()>1 && raw.equals(raw.toUpperCase(Locale.ROOT)))
            return canonical.toUpperCase(Locale.ROOT);
        if(!raw.isEmpty()&&Character.isUpperCase(raw.charAt(0)))
            return Character.toUpperCase(canonical.charAt(0))+canonical.substring(1);
        // For English contractions, the pronoun I is conventionally uppercase.
        if(canonical.startsWith("i'"))
            return "I"+canonical.substring(1);
        return canonical;
    }
}
