package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manual vocabulary v2: pure Java policy, no Android or user text persistence.
 * The user explicitly saves words in settings; the engine never scrapes text.
 * Supports ASCII English words with internal straight/curly apostrophes.
 * Rejects whitespace, emails, URLs, numeric strings and freeform sentences.
 */
final class PersonalDictionaryPolicy {
    static final int MAX_WORDS=250;
    static final int MAX_SUGGESTIONS=8;
    private PersonalDictionaryPolicy(){}

    static String canonical(String input) {
        if(input==null)return null;
        return EnglishApostrophePolicy.canonical(input);
    }

    static String acceptedDisplay(String input) {
        return canonical(input)==null?null:input.trim();
    }

    private static String adaptCase(String word,String typed) {
        if(typed.equals(typed.toUpperCase(Locale.ROOT)))return word.toUpperCase(Locale.ROOT);
        if(!typed.isEmpty() && Character.isUpperCase(typed.charAt(0)) &&
           word.equals(word.toLowerCase(Locale.ROOT)))
            return Character.toUpperCase(word.charAt(0))+word.substring(1);
        return word;
    }

    static List<String> merge(String typed,List<String> base,List<String> saved) {
        if(base==null)return List.of();
        String input=EnglishApostrophePolicy.prefix(typed);
        if(input==null||saved==null||saved.isEmpty())return base;
        // Known correctly spelled input stays top unless the user explicitly
        // saved that same word; adding vocabulary must not rewrite good text.
        boolean baseProtectsLiteral=!base.isEmpty()&&canonical(base.get(0))!=null&&
            input.equals(canonical(base.get(0)));
        List<String> manual=new ArrayList<>();
        for(String candidate:saved) {
            String normalized=canonical(candidate);
            if(normalized!=null&&normalized.startsWith(input)&&
                !containsCanonical(manual,normalized)) {
                manual.add(adaptCase(candidate,typed));
            }
            if(manual.size()>=MAX_SUGGESTIONS)break;
        }
        if(manual.isEmpty())return base;
        ArrayList<String> out=new ArrayList<>(MAX_SUGGESTIONS);
        if(baseProtectsLiteral) {
            out.add(base.get(0));
            for(String custom:manual)
                if(out.size()<MAX_SUGGESTIONS&&!containsCanonical(out,canonical(custom)))
                    out.add(custom);
        } else {
            // A directly saved exact word wins over general speculative splits.
            // All choices remain optional: no automatic editor modification.
            for(String custom:manual)
                if(out.size()<MAX_SUGGESTIONS&&!containsCanonical(out,canonical(custom)))
                    out.add(custom);
        }
        for(String general:base) {
            if(out.size()>=MAX_SUGGESTIONS)break;
            String normalized=canonical(general);
            // A full phrase is not a dictionary word, but remains an existing
            // legitimate sentence prediction. Deduplicate by display ignoring case.
            if(!containsCaseInsensitive(out,general)&&
                 (normalized==null||!containsCanonical(out,normalized)))
                out.add(general);
        }
        return out;
    }

    static boolean containsCanonical(List<String> strings,String normalized) {
        if(normalized==null)return false;
        for(String text:strings)
            if(normalized.equals(canonical(text)))return true;
        return false;
    }
    private static boolean containsCaseInsensitive(List<String> strings,String value) {
        for(String text:strings)if(text.equalsIgnoreCase(value))return true;
        return false;
    }
}
