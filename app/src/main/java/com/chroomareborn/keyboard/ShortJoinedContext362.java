package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Research 362: high-confidence, two-word correction for a missing space
 * combined with a verified double neighboring-key slip.
 *
 * Examples: "Iwikk" -> "I will"; "Iwill" -> "I will"; "wecan" -> "we can".
 * A phrase is only eligible if BOTH words are in the existing public offline
 * lexicon, the pair is a built-in NEXT transition, and the second token
 * is either exact or the UNIQUE candidate of the already-tested
 * NeighborDoubleKeyCorrectionPolicy. No new user data or learned text.
 *
 * This is invoked from the background PredictionLite worker, never the key
 * event path, and never commits/corrects anything automatically.
 */
final class ShortJoinedContext362 {
    private ShortJoinedContext362() {}

    // Explicitly limited to common short conversational heads rather than
    // trying every possible split of unrelated novel words or identifiers.
    private static final List<String> HEADS=List.of(
            "i","we","you","he","she","they","what","how",
            "this","that","can","is","are","its","lets");

    static List<String> suggestions(String typed,
            Map<String,Integer> offlineWords,
            Map<String,List<String>> establishedNext) {
        if(typed==null || offlineWords==null || establishedNext==null ||
                typed.length()<3 || typed.length()>16 ||
                !typed.matches("[A-Za-z]{3,16}"))
            return List.of();
        String compact=typed.toLowerCase(Locale.ROOT);
        if(offlineWords.containsKey(compact))return List.of();
        String match=null;
        // Avoid promoting ambiguous alternative splits when two contexts
        // offer different plausible phrases with equally strong evidence.
        int bestCost=Integer.MAX_VALUE;
        boolean ambiguous=false;
        for(String head:HEADS) {
            if(!compact.startsWith(head) ||
                    !offlineWords.containsKey(head))continue;
            String suffix=compact.substring(head.length());
            if(suffix.length()<2 || suffix.length()>12)continue;
            List<String> next=establishedNext.get(head);
            if(next==null)continue;
            // One unique correction lookup per head. A non-unique neighbor
            // spelling is deliberately not repaired.
            String neighbor=suffix.length()>=4?
                    NeighborDoubleKeyCorrectionPolicy.unique(
                            suffix,offlineWords):null;
            for(String word:next) {
                Integer frequency=offlineWords.get(word);
                if(frequency==null || frequency<12)continue;
                int cost;
                if(word.equals(suffix)) cost=0;
                else if(word.equals(neighbor))cost=2;
                else continue;
                String first=head.equals("i")?"I":head;
                String proposed=first+" "+word;
                if(typed.equals(typed.toUpperCase(Locale.ROOT)))
                    proposed=proposed.toUpperCase(Locale.ROOT);
                else if(!head.equals("i") &&
                        Character.isUpperCase(typed.charAt(0)))
                    proposed=Character.toUpperCase(first.charAt(0))+
                            first.substring(1)+" "+word;
                if(cost<bestCost) {
                    match=proposed;
                    bestCost=cost;
                    ambiguous=false;
                } else if(cost==bestCost && !proposed.equals(match))
                    ambiguous=true;
            }
        }
        if(match==null || ambiguous)return List.of();
        // Visible explicit-tap candidate followed by the exact user's text.
        // No automatic punctuation or space is added until acceptance.
        return List.of(match,typed);
    }
}
