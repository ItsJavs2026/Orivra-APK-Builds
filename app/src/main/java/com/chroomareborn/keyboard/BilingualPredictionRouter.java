package com.chroomareborn.keyboard;

import java.util.Collections;
import java.util.List;

/**
 * Explicit language router for RESEARCH ONLY. No automatic inference from
 * private typing, language settings changes, or network translation.
 * A future IME language selector may call this after device acceptance.
 * The default English branch calls the original production predictor.
 */
final class BilingualPredictionRouter {
    enum Language { ENGLISH, SPANISH }
    private BilingualPredictionRouter() {}

    static List<String> suggestions(String text,Language language,boolean editing) {
        if(text==null || language==null)return Collections.emptyList();
        if(language==Language.SPANISH)
            return SpanishPredictionLite.suggestions(text);
        return editing?PredictionLite.editingSuggestions(text):
                PredictionLite.suggestions(text);
    }
}
