package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, bounded, exact-only English vocabulary tier.
 *
 * Words in this tier do NOT acquire invented corpus frequencies and are never
 * passed through fuzzy edit-candidate generation. The base high-confidence
 * lexicon remains the only source of typo candidates.
 *
 * NOTE: This class is experimental on the 299 research branch. No APK or
 * production language pack is shipped. Loading must be on an IME worker,
 * never inside the touch-event or frame path.
 */
final class ExactOnlyLanguagePack {
    private static final int MAX_LINES=125_000;
    private final String[] sortedWords;

    private ExactOnlyLanguagePack(String[] sorted) {
        sortedWords=sorted;
    }
    private static boolean acceptable(String word) {
        if(word.length()<2 || word.length()>24)return false;
        for(int i=0;i<word.length();i++) {
            char c=word.charAt(i);
            if(c<'a'||c>'z')return false;
        }
        return true;
    }
    static ExactOnlyLanguagePack fromReader(Reader source)throws IOException {
        if(source==null)throw new IllegalArgumentException("Source required");
        BufferedReader reader=source instanceof BufferedReader
                ?(BufferedReader)source:new BufferedReader(source);
        List<String> list=new ArrayList<>();
        int lines=0;
        String word;
        while((word=reader.readLine())!=null) {
            if(++lines>MAX_LINES)throw new IOException("Offline vocabulary exceeds cap");
            // Only lowercase ASCII words in this tier; accented words
            // belong to language-specific, Unicode-aware packs.
            if(acceptable(word))list.add(word);
        }
        Collections.sort(list);
        int keep=0;
        for(int i=0;i<list.size();i++) {
            String candidate=list.get(i);
            if(keep==0 || !candidate.equals(list.get(keep-1)))
                list.set(keep++,candidate);
        }
        return new ExactOnlyLanguagePack(list.subList(0,keep).toArray(new String[0]));
    }
    boolean contains(String lowercaseAsciiWord) {
        return lowercaseAsciiWord!=null &&
                acceptable(lowercaseAsciiWord) &&
                Arrays.binarySearch(sortedWords,lowercaseAsciiWord)>=0;
    }
    int size() {return sortedWords.length;}
}
