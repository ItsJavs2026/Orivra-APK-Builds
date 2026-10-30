package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Only a tiny closed class of common, non-identifying preceding words is
 * allowed into persistent context. Full phrases, addresses, numbers, URLs,
 * line breaks and private text must never form a learning key.
 */
final class AcceptedContextPolicy319 {
    private AcceptedContextPolicy319() {}
    private static final Set<String> SAFE=Set.of(
        "a","an","the","i","you","we","they","he","she","it","my","your",
        "our","their","me","us","him","her","them","to","for","of","in",
        "on","at","with","from","and","or","but","if","when","where","why",
        "how","who","what","is","are","was","were","will","can","could",
        "would","should","do","does","did","have","has","had","need",
        "want","please","let","lets","yes","no","hello","hi","this",
        "that","these","those","some","any","all","more","less","much",
        "really","very","so","just","now","today","tomorrow","again");
    static String safeWord(String input) {
        if(input==null || input.length()<1 || input.length()>20 ||
                !input.matches("[A-Za-z]+"))return null;
        String canonical=input.toLowerCase(Locale.ROOT);
        return SAFE.contains(canonical)?canonical:null;
    }
    /** The before-cursor snapshot MUST end with the literal word being
     * replaced, or the snapshot is stale / inapplicable.
     */
    static String precedingForReplacement(CharSequence before,String typed) {
        if(before==null||typed==null || !typed.matches("[A-Za-z']{2,32}"))
            return null;
        String text=before.toString();
        if(text.length()<typed.length() ||
                !text.regionMatches(true,text.length()-typed.length(),
                    typed,0,typed.length()))return null;
        return precedingForNextWord(text.substring(0,text.length()-typed.length()));
    }
    /** Requires a real whitespace boundary; punctuation and prior private
     * text are never copied into a learning key.
     */
    static String precedingForNextWord(CharSequence before) {
        if(before==null)return null;
        String text=before.toString();
        int pos=text.length()-1;
        if(pos<0 || text.charAt(pos)!=' ')return null;
        while(pos>=0 && text.charAt(pos)==' ')pos--;
        int end=pos+1;
        while(pos>=0 && Character.isLetter(text.charAt(pos)))pos--;
        if(pos>=0 && !Character.isWhitespace(text.charAt(pos)))return null;
        if(pos>=0 && (text.charAt(pos)=='\n'||text.charAt(pos)=='\r'))
            return null;
        return safeWord(text.substring(pos+1,end));
    }
    static String acceptedWord(String value) {
        if(value==null || value.length()<2 || value.length()>26 ||
                !value.matches("[A-Za-z]+(?:'[A-Za-z]+)*"))return null;
        return EnglishApostrophePolicy.canonical(value);
    }
    static List<String> phraseWords(String suggestion) {
        if(suggestion==null || suggestion.length()>100 ||
                !suggestion.matches("[A-Za-z']+(?: [A-Za-z']+){1,9}"))
            return List.of();
        String[] split=suggestion.split(" ");
        for(String word:split)if(acceptedWord(word)==null)return List.of();
        return List.of(split);
    }
}
