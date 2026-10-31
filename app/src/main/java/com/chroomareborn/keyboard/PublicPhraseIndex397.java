package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Research 397: immutable index of PUBLIC, build-time conversational phrases.
 * No typed/editor input, learned words, SharedPreferences, or telemetry
 * can enter this index. Construct it once instead of repeating the exact
 * same regex normalization on every suggestion generation.
 *
 * Keep original input order: the predictor uses the first tie-winning phrase.
 */
final class PublicPhraseIndex397 {
    private final String[] compact;
    private final Map<String,List<String>> firstWord;

    PublicPhraseIndex397(String[] source) {
        if (source==null) throw new IllegalArgumentException("public phrases required");
        compact=new String[source.length];
        Map<String,List<String>> map=new HashMap<>();
        for (int i=0;i<source.length;i++) {
            String phrase=source[i];
            if(phrase==null)throw new IllegalArgumentException("null public phrase");
            // Exact legacy normalization, performed just once on PUBLIC text.
            compact[i]=phrase.toLowerCase(Locale.US).replaceAll("[^a-z]","");
            String[] parts=phrase.split(" ");
            if(parts.length<2 || parts.length>5)continue;
            String head=parts[0].replaceAll("[^A-Za-z]","").toLowerCase(Locale.US);
            map.computeIfAbsent(head,unused->new ArrayList<>()).add(phrase);
        }
        Map<String,List<String>> frozen=new HashMap<>();
        for(Map.Entry<String,List<String>> entry:map.entrySet())
            frozen.put(entry.getKey(),Collections.unmodifiableList(
                    new ArrayList<>(entry.getValue())));
        firstWord=Collections.unmodifiableMap(frozen);
    }
    int size(){return compact.length;}
    String compactAt(int index){return compact[index];}
    List<String> startingWith(String lowercaseAsciiWord){
        if(lowercaseAsciiWord==null)return Collections.emptyList();
        List<String> result=firstWord.get(lowercaseAsciiWord);
        return result==null?Collections.emptyList():result;
    }
}
