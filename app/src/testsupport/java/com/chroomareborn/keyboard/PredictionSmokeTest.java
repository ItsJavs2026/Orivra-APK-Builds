package com.chroomareborn.keyboard;

import java.util.List;

public final class PredictionSmokeTest {
    private static int assertions=0;
    private static void expect(boolean check, String message) {
        assertions++;
        if (!check) throw new AssertionError(message);
    }
    private static void includes(String typed,String suggestion) {
        List<String> list=PredictionLite.suggestions(typed);
        expect(list.contains(suggestion),typed+" should offer "+suggestion+" but was "+list);
    }
    private static void top(String input,String expected) {
        List<String> got=PredictionLite.suggestions(input);
        expect(!got.isEmpty() && got.get(0).equalsIgnoreCase(expected),
            input+" expected first "+expected+" but got "+got);
    }
    private static void diagnostic(String typed,String intended) {
        List<String> guesses=PredictionLite.suggestions(typed);
        boolean top1=!guesses.isEmpty() && intended.equalsIgnoreCase(guesses.get(0));
        boolean top3=guesses.stream().anyMatch(x -> intended.equalsIgnoreCase(x));
        System.out.println("Screenshot diagnostic input="+typed+
                " top1="+top1+" top3="+top3+
                " suggestion="+(guesses.isEmpty()?"(none)":guesses.get(0)));
    }
    public static void main(String[] args) {
        // New: all single-letter keys must offer candidates.
        for(char c='a';c<='z';c++) {
            List<String> options=PredictionLite.suggestions(String.valueOf(c));
            expect(!options.isEmpty(),"No candidates for "+c);
            expect(options.size()>=2,"Not enough candidates for "+c+": "+options);
        }
        includes("h","how");
        includes("h","hello");
        includes("c","can");
        includes("i","I'm");
        includes("w","what");
        includes("t","this");
        includes("hom","home");
        includes("hel","hello");
        includes("thisis","this is");
        includes("howareyoudoingtoday","how are you doing today");
        expect(PredictionLite.suggestions("hel1").isEmpty(),"Numbers must be ignored");
        expect(PredictionLite.suggestions("").isEmpty(),"Blank must be ignored");
        expect(PredictionLite.suggestions("working").size()<4,"No more than three suggestions");
        top("okattthisisstartingtocomeakong","okay this is starting to come along");
        top("thisisstartingtocomealong","this is starting to come along");
        top("firstifall","first of all");
        top("firstofall","first of all");
        top("howareyoudoingtoday","how are you doing today");
        top("thisiscookbutitslaking","this is cool but it's lacking");
        top("letsseewhathappenswhenidothis","let's see what happens when I do this");
        top("canyouletmeknowwhenyougethome","can you let me know when you get home");
        // Confirmed user screenshots: stable words and exact phrase ranking.
        top("also","also");
        top("were","were");
        top("howsitgoing","how's it going");
        top("howsotgoing","how's it going");
        // User screenshot diagnostics: never claim these are fixed until measured.
        diagnostic("vibrstion","vibration");
        diagnostic("ofanimation","of animation");
        diagnostic("singke","single");
        diagnostic("singkewoods","single words");
        diagnostic("hiwsitgoijg","how's it going");
        diagnostic("howsotgkikf","how's it going");
        expect(PredictionLite.suggestions("also").size()<=3,
                "Known word must not open an expanded nonsense-suggestion row");
        // User-confirmed intended sentence; tracked diagnostically until the
        // multi-typo recovery engine can pass reliably.
        diagnostic("okayniwweregettijgsokewjere","okay now we're getting somewhere");
        // Screenshot intents are provisionally inferred, not user-confirmed.
        // Record improvement, but do not fail the build over experimental ranking.
        diagnostic("aksoadd","also add");
        diagnostic("aksoaddsomwmore","also add some more");
        diagnostic("letsteysayingsimetbingcompltekekyunrelated",
                "let's try saying something completely unrelated");
        diagnostic("inusingtheeybjsrdtotyoethis",
                "I'm using the keyboard to type this");
        // These are known difficult queries: they should remain safe/no crash,
        // but accurate full-sentence recognition is NOT claimed by this test.
        PredictionLite.suggestions("hellowhiwareyou");
        PredictionLite.suggestions("thisiscookbutitslaking");
        System.out.println("PredictionSmokeTest: "+assertions+" checks passed");
    }
}
