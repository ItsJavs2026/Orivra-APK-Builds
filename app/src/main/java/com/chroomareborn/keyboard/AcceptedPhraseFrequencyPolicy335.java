package com.chroomareborn.keyboard;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Break up an explicitly accepted phrase ONLY for public-word frequency.
 * Never persist the complete sentence or arbitrary raw editor content.
 */
final class AcceptedPhraseFrequencyPolicy335 {
    private AcceptedPhraseFrequencyPolicy335(){}
    static List<String> words(String acceptedSuggestion) {
        if(acceptedSuggestion==null || acceptedSuggestion.length()>140 ||
                !acceptedSuggestion.matches(
                    "[A-Za-z']+(?: [A-Za-z']+){0,11}"))
            return List.of();
        String[] raw=acceptedSuggestion.split(" ",-1);
        LinkedHashSet<String> distinct=new LinkedHashSet<>();
        for(String token:raw) {
            if(token.length()==1) {
                // Ignore "a" and "I": already-complete one-letter words
                // cannot offer useful prefix completions.
                if(!token.equalsIgnoreCase("a") &&
                        !token.equalsIgnoreCase("i"))return List.of();
                continue;
            }
            String canonical=EnglishApostrophePolicy.canonical(token);
            if(canonical==null)return List.of();
            distinct.add(canonical);
        }
        return new ArrayList<>(distinct);
    }
}
