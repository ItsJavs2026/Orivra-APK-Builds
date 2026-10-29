package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Display-only ranking for an existing cursor-word correction strip.
 * Uses neighboring WHOLE words and a known offline transition index.
 *
 * Intentionally conservative: retain the literal word first, never invent
 * candidates, and reorder alternatives only when the same candidate has
 * evidence on BOTH sides. Unknown or punctuation-separated context must
 * produce exactly the original predictor ranking.
 */
final class CursorContextCorrectionRanker {
    interface PairEvidence {
        boolean supported(String preceding,String following);
    }

    private CursorContextCorrectionRanker() {}

    private static boolean wordCharacter(char character) {
        return Character.isLetter(character);
    }

    private static String previousWord(CharSequence before) {
        if(before==null)return "";
        int i=before.length();
        // First skip the current word's prefix, if caret is inside a word.
        while(i>0 && wordCharacter(before.charAt(i-1)))i--;
        if(i==0 || !Character.isWhitespace(before.charAt(i-1)))return "";
        while(i>0 && Character.isWhitespace(before.charAt(i-1))) {
            if(before.charAt(i-1)!=' ')return ""; // sentence/line boundary
            i--;
        }
        int end=i;
        while(i>0 && wordCharacter(before.charAt(i-1)))i--;
        if(end==i || end-i>32)return "";
        // Do not train or rank across punctuation/sentence breaks.
        for(int x=end;x<before.length();x++)
            if(!Character.isWhitespace(before.charAt(x)) &&
                    !wordCharacter(before.charAt(x)))return "";
        return before.subSequence(i,end).toString().toLowerCase(Locale.ROOT);
    }

    private static String nextWord(CharSequence after) {
        if(after==null)return "";
        int i=0;
        // Skip the remaining characters of the word under the cursor.
        while(i<after.length() && wordCharacter(after.charAt(i)))i++;
        if(i>=after.length() || !Character.isWhitespace(after.charAt(i)))return "";
        while(i<after.length() && Character.isWhitespace(after.charAt(i))) {
            if(after.charAt(i)!=' ')return ""; // sentence/line boundary
            i++;
        }
        int begin=i;
        while(i<after.length() && wordCharacter(after.charAt(i)))i++;
        if(begin==i || i-begin>32)return "";
        return after.subSequence(begin,i).toString().toLowerCase(Locale.ROOT);
    }

    static List<String> rerank(String original,List<String> ranked,
                               CharSequence before,CharSequence after,
                               PairEvidence evidence) {
        if(original==null || ranked==null || ranked.size()<3 ||
                evidence==null)return ranked;
        String left=previousWord(before),right=nextWord(after);
        if(left.isEmpty() || right.isEmpty())return ranked;
        ArrayList<String> supported=new ArrayList<>();
        ArrayList<String> others=new ArrayList<>();
        // Keep the original chip where users expect it and never replace
        // a correctly spelled word automatically.
        for(int i=1;i<ranked.size();i++) {
            String candidate=ranked.get(i);
            if(candidate==null || !candidate.matches("[\\p{L}]{2,32}")) {
                others.add(candidate);
                continue;
            }
            String normal=candidate.toLowerCase(Locale.ROOT);
            if(!normal.equals(original.toLowerCase(Locale.ROOT)) &&
                    evidence.supported(left,normal) &&
                    evidence.supported(normal,right))
                supported.add(candidate);
            else others.add(candidate);
        }
        if(supported.isEmpty())return ranked; // No evidence: exact same list.
        ArrayList<String> result=new ArrayList<>(ranked.size());
        result.add(ranked.get(0));
        result.addAll(supported);
        result.addAll(others);
        return result;
    }
}
