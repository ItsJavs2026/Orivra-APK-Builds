package com.chroomareborn.keyboard;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exact two-key private preference mutation plan for one verified Space.
 * The caller has already validated the complete prospective vocabulary and
 * holds the shared store/consent monitor. No disk I/O or app/editor context.
 *
 * Full snapshots remain unchanged for V4 export and tap-only prediction.
 * An unchanged saturated count yields zero preference writes.
 */
final class TypedWordDelta349 {
    private TypedWordDelta349() {}

    static Map<String,Integer> changedEntries(
            TypedWordEvidenceStore341.Snapshot old,
            Map<String,Integer> words, Map<String,Integer> pairs,
            String current, String pair) {
        if(old==null || words==null || pairs==null)
            throw new IllegalArgumentException("Missing validated word evidence");
        TreeMap<String,Integer> delta=new TreeMap<>();
        if(current==null || !words.containsKey(current))
            throw new IllegalArgumentException("Missing current confirmed word");
        Integer newWordCount=words.get(current);
        if(!newWordCount.equals(old.words.get(current)))
            delta.put("w:"+current,newWordCount);
        if(pair!=null && pairs.containsKey(pair)) {
            Integer newPairCount=pairs.get(pair);
            if(!newPairCount.equals(old.pairs.get(pair)))
                delta.put("p:"+pair,newPairCount);
        }
        return Collections.unmodifiableMap(delta);
    }
}
