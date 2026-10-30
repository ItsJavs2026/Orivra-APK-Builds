package com.chroomareborn.keyboard;

import java.util.List;

/** User screenshot reproduced with synthetic data, never original user text. */
public final class JoinedEnglishContraction314Test {
    private static int total;
    private static void yes(boolean condition,String message) {
        total++;if(!condition)throw new AssertionError(message);
    }
    private static void accepts(String raw,String decoded,String expected) {
        yes(expected.equals(JoinedEnglishContractionPolicy.punctuateExactJoined(raw,decoded)),
                "Exact missing-mark reconstruction: "+raw);
    }
    public static void main(String[] args) {
        accepts("Yesthats","Yes thats","Yes that's");
        accepts("yesthats","yes thats","yes that's");
        accepts("youredone","youre done",null); // correction only at final word
        accepts("yesdont","yes dont","yes don't");
        accepts("welldidnt","well didnt","well didn't");
        accepts("myfriendwasnt","my friend wasnt","my friend wasn't");
        accepts("thatsright","thats right",null); // don't invent earlier punctuation
        accepts("wewere","we were",null); // ambiguous contraction
        accepts("welets","we lets",null); // ordinary plural verb
        accepts("wehows","we hows",null); // ambiguous alternative
        accepts("yesthat","yes that",null);
        accepts("yesthats","yes that",null); // no letter insertion/deletion
        accepts("yesthats","Yes thats! ",null);
        accepts("yesthats","yes th ats",null);
        accepts("yesthats","yesthats",null);
        accepts("foo@example.com","foo example com",null);
        List<String> observed=JoinedEnglishContractionPolicy.offer("Yesthats",
                List.of("Yesthats","Yes thats","Yes that"));
        yes(observed.equals(List.of("Yesthats","Yes thats","Yes that's","Yes that")),
                "Build 310 screenshot: offer 'Yes that's' in visible top-three");
        yes(JoinedEnglishContractionPolicy.offer("Yesthats",
                List.of("Yes thats","Yes that's")).equals(List.of("Yes thats","Yes that's")),
                "Do not duplicate an existing phrase");
        yes(JoinedEnglishContractionPolicy.offer("Wewere",
                List.of("We were","We we're")).equals(List.of("We were","We we're")),
                "Do not force ambiguous were/we're");
        List<String> base=List.of("typing","typo","typed");
        yes(JoinedEnglishContractionPolicy.offer("typing",base)==base,
                "Known standalone spelling preserves the untouched list");
        yes(JoinedEnglishContractionPolicy.offer("Yesthats",
                List.of("Something unrelated","Yes that")) .equals(
                List.of("Something unrelated","Yes that")),
                "Never fabricate a phrase without exact segmentation evidence");
        yes(JoinedEnglishContractionPolicy.offer("Yes thats",
                List.of("Yes thats")).equals(List.of("Yes thats")),
                "Input with explicit spaces is not joined-token reconstruction");
        yes(JoinedEnglishContractionPolicy.offer("yes123",
                List.of("yes dont")).equals(List.of("yes dont")),
                "Never process digits or sensitive-shaped strings");
        String[] words={"hello","keyboard","that's","I'm","were","cantilever",
                "address","password","tomorrow"};
        for(String word:words) {
            List<String> literal=List.of(word);
            yes(JoinedEnglishContractionPolicy.offer(word,literal)==literal,
                    "Protected complete word: "+word);
        }
        System.out.println("PASS: JoinedEnglishContraction314Test "+total+" assertions");
    }
}
