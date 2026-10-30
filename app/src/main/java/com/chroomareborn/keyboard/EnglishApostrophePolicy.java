package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bounded English single-word apostrophe and contraction policy.
 * Pure Java, no editor access or telemetry. Normalizes curly U+2019 only
 * for matching; retains user-entered display punctuation when appropriate.
 */
final class EnglishApostrophePolicy {
    private EnglishApostrophePolicy() {}
    private static final List<String> FORMS=Collections.unmodifiableList(Arrays.asList(
            "ain't","aren't","can't","couldn't","didn't","doesn't","don't",
            "hadn't","hasn't","haven't","he'd","he'll","he's","how'd",
            "how'll","how's","I'd","I'll","I'm","I've","isn't","it's",
            "let's","mightn't","mustn't","o'clock","shan't","she'd",
            "she'll","she's","shouldn't","that's","there's","they'd",
            "they'll","they're","they've","wasn't","we'd","we'll",
            "we're","we've","weren't","what's","where's","who'd",
            "who'll","who's","won't","wouldn't","you'd","you'll",
            "you're","you've","y'all"
    ));
    private static final Map<String,String> UNAMBIGUOUS_MISSING_MARK;
    static {
        Map<String,String> words=new LinkedHashMap<>();
        for(String original:FORMS){
            String key=canonical(original);
            if(key==null)throw new AssertionError("Invalid built-in form");
            // Already-valid ordinary words like "were", "its", "well",
            // "shell", "id", "ill" must NOT be converted automatically.
        }
        for(String pair:new String[]{
                "aint=ain't","arent=aren't","cant=can't",
                "couldnt=couldn't","didnt=didn't","doesnt=doesn't",
                "dont=don't","hadnt=hadn't","hasnt=hasn't",
                "havent=haven't","im=I'm","isnt=isn't",
                "mustnt=mustn't","shouldnt=shouldn't",
                "wasnt=wasn't","werent=weren't","wont=won't",
                "wouldnt=wouldn't","youre=you're","theyre=they're",
                "youve=you've","weve=we've","whats=what's",
                "thats=that's","wheres=where's","whos=who's",
                "lets=let's","yall=y'all"
        }) {
            String[] parts=pair.split("=",2);
            words.put(parts[0],parts[1]);
        }
        UNAMBIGUOUS_MISSING_MARK=Collections.unmodifiableMap(words);
    }

    /** ASCII letters with one or more internal apostrophes, no separators.
     * Exact user dictionary storage also accepts a single proper-name apostrophe.
     */
    static String canonical(String word) {
        if(word==null)return null;
        String value=word.trim().replace('\u2019','\'');
        if(value.length()<2||value.length()>32)return null;
        if(!value.matches("[A-Za-z]+(?:'[A-Za-z]+)*"))return null;
        int letters=0;
        for(int i=0;i<value.length();i++)
            if(Character.isLetter(value.charAt(i)))letters++;
        if(letters<2||letters>28)return null;
        return value.toLowerCase(Locale.ROOT);
    }

    /** An incomplete in-progress word is permitted to end in apostrophe. */
    static String prefix(String raw){
        if(raw==null)return null;
        String normal=raw.replace('\u2019','\'');
        if(normal.length()<1 || normal.length()>32 ||
                !normal.matches("[A-Za-z]+(?:'[A-Za-z]+)*'?"))
            return null;
        return normal.toLowerCase(Locale.ROOT);
    }

    static boolean canAppendApostrophe(String composing,String key){
        if(!("'".equals(key)||"\u2019".equals(key)))return false;
        return composing!=null && composing.matches("[A-Za-z]{1,28}");
    }

    static boolean isTokenChar(char c) {
        return Character.isLetter(c) || c=='\'' || c=='\u2019';
    }

    static boolean containsApostrophe(String value) {
        return value!=null && (value.indexOf('\'')>=0 || value.indexOf('\u2019')>=0);
    }

    /** Exact contractions preserve the user's straight/curly punctuation.
     * Missing-apostrophe alternatives never auto-commit or steal a known word.
     */
    static List<String> suggestions(String raw){
        String normalized=prefix(raw);
        if(normalized==null)return Collections.emptyList();
        ArrayList<String> choices=new ArrayList<>();
        boolean typedApostrophe=containsApostrophe(raw);
        if(typedApostrophe && canonical(raw)!=null)
            choices.add(raw); // literal spelling first, never rewrite it
        if(typedApostrophe){
            for(String word:FORMS){
                String key=canonical(word);
                if(key.startsWith(normalized) && !containsEquivalent(choices,word)){
                    choices.add(word);
                    if(choices.size()>=6)break;
                }
            }
        } else {
            String fixed=UNAMBIGUOUS_MISSING_MARK.get(normalized);
            if(fixed!=null)choices.add(fixed);
        }
        return choices;
    }

    static List<String> merge(String input,List<String> base){
        if(input==null||base==null)return base==null?Collections.emptyList():base;
        List<String> extra=suggestions(input);
        if(extra.isEmpty())return base;
        ArrayList<String> out=new ArrayList<>();
        // Completed apostrophized token: show the literal as the first option.
        if(containsApostrophe(input)&&canonical(input)!=null)
            out.add(input);
        else if(!base.isEmpty())
            out.add(base.get(0)); // no-apostrophe "cant" may be deliberate
        for(String candidate:extra)
            if(!containsEquivalent(out,candidate)&&out.size()<8)out.add(candidate);
        for(String candidate:base)
            if(candidate!=null&&!containsEquivalent(out,candidate)&&out.size()<8)
                out.add(candidate);
        return out;
    }

    private static boolean containsEquivalent(List<String> arr,String word){
        for(String x:arr){
            if(x!=null && x.replace('\u2019','\'').equalsIgnoreCase(
                    word.replace('\u2019','\'')))return true;
        }
        return false;
    }
}
