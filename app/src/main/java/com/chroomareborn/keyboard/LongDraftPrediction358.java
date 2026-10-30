package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Research 358: conservative, tap-only prediction continuity for long drafts.
 *
 * Never edits a user's text, touches an InputConnection, records personal
 * context, or runs a model. The caller must verify the current editor state:
 * unselected, end-of-document, non-private, with suggestions permitted.
 *
 * Two different states are intentionally distinct:
 *  (1) a composing, already-known whole word may preview a "word next" chip;
 *  (2) a verified whitespace boundary may backfill idle next-word chips.
 * The first must go through the existing one-transaction composing acceptance;
 * the second must go through the existing NextWordTapTicket340.
 *
 * This is a low-confidence backoff only after learned/corpus suggestions.
 * Nothing is auto-corrected or auto-inserted.
 */
final class LongDraftPrediction358 {
    private LongDraftPrediction358() {}
    private static final int MAX_CHIPS=3;

    // Public, context-free grammatical hints, not generated user text.
    // This is intentionally small; mature n-gram or learned associations
    // always win and a later language model can replace this backoff.
    private static final Set<String> PREPOSITIONS=Set.of(
            "to","for","of","in","on","at","with","from","about","into");
    private static final Set<String> NOUN_ENDINGS=Set.of(
            "tion","ment","ness","ance","ence","shot","age","graph");

    /** A safe public-word fallback, never a guarantee of grammatical fit. */
    private static List<String> generalAfter(String word) {
        if(word==null || word.isEmpty())return List.of();
        String low=word.toLowerCase(Locale.ROOT);
        if("i".equals(low))return List.of("am","will");
        if("you".equals(low))return List.of("are","can");
        if("we".equals(low))return List.of("can","will");
        if("especially".equals(low))return List.of("when","if");
        if("because".equals(low))return List.of("of","the");
        if("please".equals(low))return List.of("help","check");
        if("and".equals(low))return List.of("the","I");
        if("but".equals(low))return List.of("I","the");
        if("is".equals(low))return List.of("the","a");
        if("are".equals(low))return List.of("the","you");
        if(PREPOSITIONS.contains(low))return List.of("the","a");
        for(String suffix:NOUN_ENDINGS)
            if(low.endsWith(suffix))return List.of("of","and");
        if(low.endsWith("ing") || low.endsWith("ed"))
            return List.of("and","the");
        return List.of("and","with");
    }

    private static boolean simplePublicWord(String word) {
        if(word==null || word.length()<2 || word.length()>24)return false;
        for(int i=0;i<word.length();i++) {
            char ch=word.charAt(i);
            if(!((ch>='a'&&ch<='z')||(ch>='A'&&ch<='Z')))
                return false;
        }
        return true;
    }

    /** Does not add weak guesses when the main model has a real alternative. */
    static List<String> whileComposing(String currentWord,
            List<String> ranked,boolean editorEligible,
            boolean verifiedPublicWord) {
        if(!editorEligible || !verifiedPublicWord ||
                !simplePublicWord(currentWord) ||
                currentWord.length()<4 || currentWord.length()>24)
            return ranked==null?List.of():ranked;
        ArrayList<String> out=new ArrayList<>();
        if(ranked!=null)for(String candidate:ranked)
            if(candidate!=null && !candidate.isEmpty() &&
                    !out.contains(candidate))out.add(candidate);
        // Evidence-backed model outputs have precedence. If existing
        // alternatives already form multiword predictions, leave them alone.
        if(out.size()>=MAX_CHIPS)return out;
        for(String candidate:out)
            if(candidate.indexOf(' ')>=0)return out;
        boolean exact=false;
        for(String choice:out)
            if(choice.equalsIgnoreCase(currentWord))exact=true;
        if(!exact)return out;
        for(String next:generalAfter(currentWord)) {
            String phrase=currentWord+" "+next;
            if(out.size()>=MAX_CHIPS)break;
            if(!out.contains(phrase))out.add(phrase);
        }
        return out;
    }

    /** Backfill only at an actual trailing whitespace boundary. The caller
     * supplies its fully merged personal/corpus/seed predictions FIRST.
     */
    static List<String> afterCommittedBoundary(CharSequence beforeCursor,
            List<String> ranked,boolean editorEligible) {
        ArrayList<String> out=new ArrayList<>();
        if(ranked!=null)for(String choice:ranked) {
            if(choice==null || choice.isEmpty() || out.contains(choice))continue;
            out.add(choice);
        }
        if(!editorEligible || out.size()>=MAX_CHIPS ||
                beforeCursor==null || beforeCursor.length()==0)
            return out;
        char tail=beforeCursor.charAt(beforeCursor.length()-1);
        if(!Character.isWhitespace(tail) && !Character.isSpaceChar(tail))
            return out;

        int end=beforeCursor.length()-1;
        while(end>=0 && (Character.isWhitespace(beforeCursor.charAt(end))
                || Character.isSpaceChar(beforeCursor.charAt(end))))end--;
        if(end<0)return out; // no typed word at this editor boundary
        char last=beforeCursor.charAt(end);
        if(last=='.' || last=='!' || last=='?' ||
                last==';' || last==':') {
            for(String start:List.of("I","The")) {
                if(out.size()>=MAX_CHIPS)break;
                if(!out.contains(start))out.add(start);
            }
            return out;
        }
        int start=end;
        while(start>=0) {
            char ch=beforeCursor.charAt(start);
            if((ch>='a'&&ch<='z')||(ch>='A'&&ch<='Z'))start--;
            else break;
        }
        String word=beforeCursor.subSequence(start+1,end+1).toString();
        if(!simplePublicWord(word))return out;
        // If the text is a URL, identifier or path, offer no generic
        // next-word candidates. The editor privacy gate still applies.
        if(start>=0) {
            char preceding=beforeCursor.charAt(start);
            if(preceding=='/' || preceding=='@' || preceding=='.' ||
                    preceding=='_' || Character.isDigit(preceding))return out;
        }
        for(String next:generalAfter(word)) {
            if(out.size()>=MAX_CHIPS)break;
            boolean duplicate=false;
            for(String value:out)
                if(value.equalsIgnoreCase(next)){duplicate=true;break;}
            if(!duplicate)out.add(next);
        }
        return out;
    }
}
