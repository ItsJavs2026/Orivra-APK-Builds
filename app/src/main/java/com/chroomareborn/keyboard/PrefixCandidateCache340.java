package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small per-IME, single-prefix cache for personal word-frequency suggestions.
 *
 * A single keystroke can render an immediate row, an apostrophe row, a split
 * row and a background result. Those paths used to repeat the same
 * SharedPreferences/dictionary scan on the main thread. This cache permits
 * one scan per distinct prefix until explicit learning or editor changes.
 * The cache is memory-only and never contains raw draft/editor text.
 */
final class PrefixCandidateCache340 {
    interface Lookup {
        List<String> find(String prefix);
    }
    private String previousPrefix;
    private List<String> previousMatches=Collections.emptyList();

    synchronized List<String> get(String prefix,Lookup lookup) {
        if(prefix==null || lookup==null)return Collections.emptyList();
        if(prefix.equals(previousPrefix))return previousMatches;
        List<String> found=lookup.find(prefix);
        previousPrefix=prefix;
        previousMatches=found==null?Collections.emptyList():
                Collections.unmodifiableList(new ArrayList<>(found));
        return previousMatches;
    }

    synchronized void clear() {
        previousPrefix=null;
        previousMatches=Collections.emptyList();
    }
}
