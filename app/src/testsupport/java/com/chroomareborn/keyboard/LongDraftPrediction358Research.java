package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Random;

/** Synthetic Research 358 assertions; not a Samsung speed/accuracy claim. */
public final class LongDraftPrediction358Research {
    private static int checks;
    private static void eq(Object expected,Object got,String label) {
        checks++;
        if(!expected.equals(got))
            throw new AssertionError(label+": expected="+expected+" got="+got);
    }
    private static void yes(boolean value,String label) {
        checks++;
        if(!value)throw new AssertionError(label);
    }
    private static String longDraft(String tail) {
        StringBuilder msg=new StringBuilder();
        for(int i=0;i<25;i++)
            msg.append("This is a separate sentence with some ordinary words. ");
        return msg.append(tail).toString();
    }
    public static void main(String[] args) {
        // Screenshot reproduction: exact, correctly spelled content word.
        eq(List.of("screenshot","screenshot of","screenshot and"),
                LongDraftPrediction358.whileComposing(
                        "screenshot",List.of("screenshot"),true,true),
                "Current word must not leave two dead side slots");
        eq(List.of("Especially","Especially when","Especially if"),
                LongDraftPrediction358.whileComposing(
                        "Especially",List.of("Especially"),true,true),
                "Function word in the screenshot");

        // The model always wins if it has established a proper phrase.
        eq(List.of("screenshot","screenshot from","screenshot with"),
                LongDraftPrediction358.whileComposing("screenshot",
                        List.of("screenshot","screenshot from","screenshot with"),
                        true,true),"Established rank and phrase preservation");
        eq(List.of("screenshot","screenshots","screenshot of"),
                LongDraftPrediction358.whileComposing("screenshot",
                        List.of("screenshot","screenshots"),true,true),
                "Lexical correction stays above low-confidence phrase");
        eq(List.of("tekk","tell"),
                LongDraftPrediction358.whileComposing("tekk",
                        List.of("tekk","tell"),true,false),
                "No speculative completions for unknown typo");
        eq(List.of("screenshot"),
                LongDraftPrediction358.whileComposing("screenshot",
                        List.of("screenshot"),false,true),
                "Private and selected editor must opt out");

        // After Space, backfill when known examples and personal learning
        // do not recognize an unfamiliar ending, irrespective of draft length.
        String paragraph=longDraft("Especially what I screenshot ");
        eq(List.of("of","and"),
                LongDraftPrediction358.afterCommittedBoundary(
                        paragraph,List.of(),true),
                "Long draft still has next-word suggestions");
        eq(List.of("you","of","and"),
                LongDraftPrediction358.afterCommittedBoundary(
                        paragraph,List.of("you"),true),
                "Existing learned candidate stays first");
        eq(List.of("you","know","what"),
                LongDraftPrediction358.afterCommittedBoundary(
                        paragraph,List.of("you","know","what"),true),
                "Full evidence leaves fallback unused");
        eq(List.of(),
                LongDraftPrediction358.afterCommittedBoundary(
                        paragraph,List.of(),false),
                "Private or editing editor stays empty");
        eq(List.of(),
                LongDraftPrediction358.afterCommittedBoundary(
                        paragraph.stripTrailing(),List.of(),true),
                "Never treat in-progress word as a word boundary");
        eq(List.of("I","The"),
                LongDraftPrediction358.afterCommittedBoundary(
                        longDraft("This test is done. "),List.of(),true),
                "Sentence punctuation starts new prediction context");
        eq(List.of(),
                LongDraftPrediction358.afterCommittedBoundary(
                        "https://example.com ",List.of(),true),
                "URL must not receive generic word hints");
        eq(List.of(),
                LongDraftPrediction358.afterCommittedBoundary(
                        "account@private ",List.of(),true),
                "Email must not receive generic word hints");
        eq(List.of(),
                LongDraftPrediction358.afterCommittedBoundary(
                        "code_123 ",List.of(),true),
                "Identifier must not receive generic word hints");
        eq(List.of("and","with"),
                LongDraftPrediction358.afterCommittedBoundary(
                        "normaltopic ",List.of(),true),
                "Non-special unknown word still gets safe tap-only options");
        eq(List.of("the","a"),
                LongDraftPrediction358.afterCommittedBoundary(
                        "I am going to ",List.of(),true),
                "Grammar backoff for common preposition");
        eq(List.of(),
                LongDraftPrediction358.afterCommittedBoundary(
                        " ",List.of(),true),
                "Empty boundary without word does not hallucinate");

        // Random long messages must not erase the backoff or overflow limits.
        Random r=new Random(35820261030L);
        for(int n=0;n<25_000;n++) {
            int count=5+r.nextInt(70);
            StringBuilder body=new StringBuilder();
            for(int i=0;i<count;i++)body.append("hello long message ");
            body.append("screenshot ");
            List<String> next=LongDraftPrediction358.afterCommittedBoundary(
                    body,List.of(),true);
            eq(List.of("of","and"),next,"long paragraph "+n);
            yes(next.size()<=3,"Bounded chips "+n);
        }
        System.out.println("PASS Research 358: "+checks+
                " privacy, long-draft, known-word and legacy-priority checks");
    }
}
