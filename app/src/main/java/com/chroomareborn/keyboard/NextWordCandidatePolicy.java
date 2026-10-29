package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Stateless display-only integration of optional offline next-word seeds.
 * Neither reads private editor text nor persists or transmits anything.
 */
final class NextWordCandidatePolicy {
    private NextWordCandidatePolicy() {}

    static boolean eligible(boolean enabled,boolean sensitive,boolean editing,
            boolean selection,boolean textAfterCursor,String precedingWord) {
        return enabled && !sensitive && !editing && !selection
                && !textAfterCursor && precedingWord!=null
                && !precedingWord.trim().isEmpty();
    }

    /**
     * Extract the committed word before one or more trailing whitespace chars.
     * Keep this in the production policy so the multi-space regression test
     * exercises exactly the logic used by the IME.
     */
    static String precedingWord(CharSequence visible) {
        if(visible==null || visible.length()==0)return "";
        int pos=visible.length()-1;
        if(!Character.isWhitespace(visible.charAt(pos)))return "";
        while(pos>=0 && Character.isWhitespace(visible.charAt(pos)))pos--;
        int end=pos+1;
        while(pos>=0 && (Character.isLetter(visible.charAt(pos))
                || visible.charAt(pos)=='\'' || visible.charAt(pos)=='’'))pos--;
        return visible.subSequence(pos+1,end).toString();
    }

    static List<String> merge(List<String> seed,List<String> legacy,int limit) {
        return mergeWithPhrase(null,seed,legacy,limit);
    }

    static List<String> mergeWithPhrase(List<String> phrase,List<String> seed,
            List<String> legacy,int limit) {
        ArrayList<String> out=new ArrayList<>();
        if(limit<=0)return out;
        Set<String> seen=new HashSet<>();
        add(out,seen,phrase,limit);
        add(out,seen,seed,limit);
        add(out,seen,legacy,limit);
        return out;
    }

    private static void add(List<String> out,Set<String> seen,List<String> values,int limit) {
        if(values==null)return;
        for(String raw:values) {
            if(out.size()>=limit)return;
            if(raw==null)continue;
            String candidate=raw.trim();
            // This row inserts exactly one word followed by a space.
            if(!candidate.matches("[\\p{L}][\\p{L}'’-]{0,31}"))continue;
            if(seen.add(candidate.toLowerCase(Locale.ROOT)))out.add(candidate);
        }
    }
}
