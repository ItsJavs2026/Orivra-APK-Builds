package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Locale;

/**
 * General dictionary/first-letter consistency: every plain word offered as
 * a first-letter completion must also be protected as a correctly spelled
 * standalone word. Uses only the built-in synthetic starter vocabulary.
 */
public final class StarterLexiconConsistencyTest {
    private static int checks=0,words=0;
    private static void ok(boolean v,String why) {
        checks++;
        if(!v)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        for(char initial='a';initial<='z';initial++) {
            List<String> first=PredictionLite.suggestions(String.valueOf(initial));
            ok(first.size()>=2,"Starter completions lost for "+initial);
            for(String completion:first) {
                if(!completion.matches("[A-Za-z]{2,24}"))continue;
                words++;
                List<String> exact=PredictionLite.suggestions(completion);
                ok(!exact.isEmpty() && exact.get(0).equalsIgnoreCase(completion),
                        "First-letter completion is treated as a typo when complete: "+
                        completion+" -> "+exact);
                // Case should be displayed consistently, without adding
                // an automatic correction or invented spacing.
                String title=Character.toUpperCase(completion.charAt(0))+
                        completion.substring(1).toLowerCase(Locale.US);
                List<String> titleSuggestions=PredictionLite.suggestions(title);
                ok(!titleSuggestions.isEmpty() &&
                        titleSuggestions.get(0).equalsIgnoreCase(title),
                        "Mixed-case starter changed: "+titleSuggestions);
            }
        }
        for(String stable:new String[]{"also","were","question",
                "quick","quiet","xylophone","morning","keyboard"}) {
            List<String> actual=PredictionLite.suggestions(stable);
            ok(!actual.isEmpty() && stable.equalsIgnoreCase(actual.get(0)),
                    "Correct word changed: "+stable+" -> "+actual);
        }
        // Never claim broad typo success: run and record a representative
        // optional negative test, but leave model accuracy to the benchmark.
        List<String> typo=PredictionLite.suggestions("qeestion");
        System.out.println("One-typo diagnostic qeestion -> "+typo);
        System.out.println("StarterLexiconConsistencyTest PASSED "+
                checks+" assertions across "+words+
                " plain first-letter completion words");
        ok(words>=50,"Starter corpus unexpectedly small");
    }
}
