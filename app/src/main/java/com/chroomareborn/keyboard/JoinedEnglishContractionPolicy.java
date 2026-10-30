package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reusable punctuation-only alternatives for already-decoded ENGLISH phrases.
 * A separate beam must first discover the exact missing spaces. This policy
 * only inserts an apostrophe in an established, unambiguous final word.
 *
 * Explicit tap only; never changes editor text, shifts the original top rank,
 * stores typed content, changes known-word behavior, or creates new spacing.
 */
final class JoinedEnglishContractionPolicy {
    private static final int MAX_VISIBLE=8;
    private JoinedEnglishContractionPolicy(){}

    static List<String> offer(String input,List<String> decoded) {
        if(input==null || decoded==null || decoded.isEmpty() ||
                input.length()<5 || input.length()>65 ||
                !input.matches("[A-Za-z]+"))return decoded;
        ArrayList<String> out=new ArrayList<>(Math.min(MAX_VISIBLE,decoded.size()+2));
        boolean added=false;
        int inspected=0;
        for(String original:decoded) {
            if(out.size()>=MAX_VISIBLE)break;
            out.add(original);
            if(inspected++>=3 || original==null)continue;
            String candidate=punctuateExactJoined(input,original);
            if(candidate==null || containsEquivalent(decoded,candidate) ||
                    containsEquivalent(out,candidate) || out.size()>=MAX_VISIBLE)continue;
            out.add(candidate);
            added=true;
        }
        return added?out:decoded;
    }

    static String punctuateExactJoined(String input,String decoded) {
        if(input==null || decoded==null || !input.matches("[A-Za-z]{5,65}"))
            return null;
        if(!decoded.matches("[A-Za-z]+(?: [A-Za-z]+){1,7}"))
            return null;
        // Do not infer letters or spaces; apostrophe omission must be the
        // ONLY discrepancy between typed and decoded versions.
        String compact=decoded.replace(" ","");
        if(!input.equalsIgnoreCase(compact))return null;
        int cut=decoded.lastIndexOf(' ');
        String last=decoded.substring(cut+1);
        String normalized=last.toLowerCase(Locale.ROOT);
        // Grammatical alternatives such as "were / we're" or "lets / let's"
        // are ambiguous. Never guess their meaning inside a sentence.
        if("were".equals(normalized)||"hows".equals(normalized)||
                "lets".equals(normalized))return null;
        List<String> choices=EnglishApostrophePolicy.suggestions(last);
        if(choices.size()!=1)return null;
        String corrected=choices.get(0);
        if(corrected.equalsIgnoreCase(last) ||
                !corrected.contains("'"))return null;
        return decoded.substring(0,cut+1)+corrected;
    }

    private static boolean containsEquivalent(List<String> options,String value) {
        for(String item:options)
            if(item!=null && item.replace('’','\'').equalsIgnoreCase(value))
                return true;
        return false;
    }
}
